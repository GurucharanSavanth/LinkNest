"""Measure a prepared dashboard fixture; never builds, installs, or changes app records."""
import argparse
import json
import re
import subprocess
import time
import xml.etree.ElementTree as ET
from pathlib import Path


def bounds(value):
    numbers = list(map(int, re.findall(r"\d+", value)))
    if len(numbers) != 4:
        raise ValueError("Invalid UI bounds")
    return numbers


def metrics(text):
    result = {}
    for percentile in (50, 95, 99):
        match = re.search(rf"{percentile}th percentile: (\d+)ms", text)
        result[f"p{percentile}_ms"] = int(match[1]) if match else None
    for key, pattern in {
        "frames": r"Total frames rendered: (\d+)",
        "janky_frames": r"Janky frames: (\d+)",
        "deadline_misses": r"Number Frame deadline missed: (\d+)",
    }.items():
        match = re.search(pattern, text)
        result[key] = int(match[1]) if match else None
    return result


def selected_state(root, node):
    parents = {child: parent for parent in root.iter() for child in parent}
    while node is not None:
        if node.get("selected") == "true" or node.get("checked") == "true":
            return True
        node = parents.get(node)
    return False


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--adb")
    parser.add_argument("--serial")
    parser.add_argument("--package", default="com.linknest.app")
    parser.add_argument("--mode", choices=("list", "grid"), default="list")
    parser.add_argument("--output", type=Path)
    parser.add_argument("--self-test", action="store_true")
    args = parser.parse_args()
    if args.self_test:
        assert bounds("[12,24][600,900]") == [12, 24, 600, 900]
        parsed = metrics("Total frames rendered: 100\nJanky frames: 25\n50th percentile: 20ms\n95th percentile: 40ms\n99th percentile: 60ms\nNumber Frame deadline missed: 26")
        assert parsed == dict(p50_ms=20, p95_ms=40, p99_ms=60, frames=100, janky_frames=25, deadline_misses=26)
        root = ET.fromstring('<node checked="true"><node content-desc="Grid mode"/></node>')
        assert selected_state(root, root[0])
        assert not selected_state(ET.fromstring('<node/>'), ET.fromstring('<node/>'))
        print("Self-check passed")
        return
    if not args.adb or not args.serial or not args.output:
        parser.error("--adb, --serial and --output are required")
    args.output.mkdir(parents=True, exist_ok=False)

    def adb(*command):
        result = subprocess.run([args.adb, "-s", args.serial, *command],
                                capture_output=True, text=True, timeout=45)
        if result.returncode:
            raise RuntimeError(f"ADB failed: {command}: {result.stderr or result.stdout}")
        return result.stdout

    def tree():
        raw = adb("exec-out", "uiautomator", "dump", "/dev/tty")
        xml = raw[raw.index("<?xml"):raw.index("</hierarchy>") + len("</hierarchy>")]
        return ET.fromstring(xml), xml

    for _ in range(12):
        root, _ = tree()
        toggle = next((node for node in root.iter("node")
                       if node.get("content-desc") == f"{args.mode.title()} mode"), None)
        if toggle is not None:
            break
        scroll = next(node for node in root.iter("node") if node.get("scrollable") == "true")
        left, top, right, bottom = bounds(scroll.get("bounds"))
        adb("shell", "input", "swipe", str((left + right) // 2), str(int(top + (bottom-top)*.25)),
            str((left + right) // 2), str(int(top + (bottom-top)*.8)), "450")
    else:
        raise RuntimeError("Dashboard mode controls remain absent after scrolling to top")
    x1, y1, x2, y2 = bounds(toggle.get("bounds"))
    adb("shell", "input", "tap", str((x1 + x2) // 2), str((y1 + y2) // 2))
    root, xml = tree()
    selected = next(node for node in root.iter("node")
                    if node.get("content-desc") == f"{args.mode.title()} mode")
    if not selected_state(root, selected):
        raise RuntimeError("Requested dashboard mode did not become selected")
    if "QA link" not in xml:
        raise RuntimeError("Prepare the explicit 300-link profiling fixture and open its dashboard first")
    scroll = next(node for node in root.iter("node") if node.get("scrollable") == "true")
    x1, y1, x2, y2 = bounds(scroll.get("bounds"))
    x, low, high = (x1 + x2) // 2, int(y1 + (y2 - y1) * .8), int(y1 + (y2 - y1) * .25)

    def swipe(up):
        adb("shell", "input", "swipe", str(x), str(low if up else high), str(x), str(high if up else low), "450")

    def flow():
        for up in (True, False):
            for _ in range(6):
                swipe(up)

    (args.output / "ui.xml").write_text(xml, encoding="utf-8")
    (args.output / "device.txt").write_text(adb("shell", "getprop") + adb("shell", "wm", "size") +
        adb("shell", "wm", "density") + adb("shell", "dumpsys", "display") +
        adb("shell", "dumpsys", "package", args.package), encoding="utf-8")
    for _ in range(8):
        swipe(False)
    flow()
    summaries = []
    for run in range(1, 4):
        tag = f"linknest-{args.mode}-{time.time_ns()}"
        remote_config = f"/data/misc/perfetto-configs/{tag}.config"
        remote_trace = f"/data/misc/perfetto-traces/{tag}.pftrace"
        config = args.output / f"run-{run}.config"
        config.write_text('duration_ms: 20000\nbuffers { size_kb: 32768 fill_policy: RING_BUFFER }\n'
            'data_sources { config { name: "linux.ftrace" ftrace_config {\n'
            'ftrace_events: "sched/sched_switch" ftrace_events: "sched/sched_wakeup"\n'
            'atrace_categories: "gfx" atrace_categories: "view" atrace_categories: "wm"\n'
            f'atrace_categories: "am" atrace_categories: "dalvik" atrace_apps: "{args.package}"\n'
            '} } }\ndata_sources { config { name: "android.surfaceflinger.frametimeline" } }\n'
            'data_sources { config { name: "linux.process_stats" process_stats_config { scan_all_processes_on_start: true } } }\n', encoding="utf-8")
        adb("push", str(config), remote_config)
        adb("shell", "dumpsys", "gfxinfo", args.package, "reset")
        started = time.monotonic()
        adb("shell", "perfetto", "--background-wait", "--txt", "-c", remote_config, "-o", remote_trace)
        flow()
        while time.monotonic() - started < 22:
            time.sleep(.25)
        previous = None
        for _ in range(10):
            size = adb("shell", "stat", "-c", "%s", remote_trace).strip()
            if size == previous and int(size) > 0:
                break
            previous = size
            time.sleep(.5)
        else:
            raise RuntimeError("Trace did not become stable")
        adb("pull", remote_trace, str(args.output / f"run-{run}.pftrace"))
        summary = adb("shell", "dumpsys", "gfxinfo", args.package)
        (args.output / f"run-{run}-gfxinfo.txt").write_text(summary, encoding="utf-8")
        (args.output / f"run-{run}-framestats.txt").write_text(adb("shell", "dumpsys", "gfxinfo", args.package, "framestats"), encoding="utf-8")
        summaries.append(metrics(summary))
        print(json.dumps(dict(mode=args.mode, run=run, **summaries[-1])), flush=True)
        adb("shell", "rm", remote_config, remote_trace)
    (args.output / "summary.json").write_text(json.dumps(summaries, indent=2), encoding="utf-8")


if __name__ == "__main__":
    main()
