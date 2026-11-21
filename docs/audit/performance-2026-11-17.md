# Optimized emulator performance evidence

The original debug capture in `build/reports/quality-2026-11-16/scroll.pftrace` remains unchanged. It is historical evidence, not a controlled before/after baseline for this build.

Planned measured scenario: Pixel_10_Pro_XL, `emulator-5554`, API 36/x86_64, physical 1344×2992, density 480 (3). Explicit 300 generated-icon profiling links in one identity-checked owned category; existing records stay intact. Build `optimizedQa` inherits release shrinking/optimization and uses Android Debug signing plus shell profiling. It is not debuggable and excludes debug fixture entry points. Package/version/minimum/target remain `com.linknest.app`, 0.1.0 (1), 26/36. The capture APK SHA-256 is `BB51E47EAECCB96767BD05F249650EBBCFD3F279D5E89271801FDC080107431D`.

Before capture: compile the target package with ART `speed`, launch, reset to top, then warm with six upward and six downward 450 ms swipes. Measure list and grid separately, three repetitions each. Swipe coordinates come from the current scroll-container UI bounds. Each run pairs gfxinfo with a 20-second Perfetto trace including frame timelines. No Gradle workflow runs during capture. The script preserves configuration/device metadata and checks that the trace file has become stable before pulling it.

The pre-measurement investigative target for this emulator is p95 ≤33 ms and fewer than 25% frame deadline misses at its normal 60 Hz mode. This is a practical comparison threshold, not a physical-device shipping budget or a promise of zero jank. Report misses/counts and p50/p95/p99 for each run, including variation and whether the target is met.

Reproduction helper: `python scripts/profile-scroll.py --adb <adb.exe> --serial emulator-5554 --mode list|grid --output <new-run-directory>`. Its parser/bounds self-check passes. The fixture requires explicit instrumentation setup/cleanup; the script never builds, installs, clears app data, or modifies library records.

## Results

| Mode/run | Frames | Deadline misses | Miss rate | p50 | p95 | p99 |
|---|---:|---:|---:|---:|---:|---:|
| List 1 | 247 | 128 | 51.82% | 38 ms | 73 ms | 81 ms |
| List 2 | 257 | 138 | 53.70% | 36 ms | 69 ms | 81 ms |
| List 3 | 229 | 131 | 57.21% | 36 ms | 69 ms | 85 ms |
| Grid 1 | 248 | 132 | 53.23% | 36 ms | 65 ms | 81 ms |
| Grid 2 | 230 | 122 | 53.04% | 36 ms | 65 ms | 77 ms |
| Grid 3 | 217 | 118 | 54.38% | 34 ms | 69 ms | 81 ms |

The target is **not met**. List miss-rate range is 51.82–57.21%, grid 53.04–54.38%; p95 varies 4 ms within each mode. These are six emulator runs, not a production/physical comparison. Artifacts: `optimized-list-v2/` and `optimized-grid-v3/` under the evidence root, each containing raw traces, gfxinfo, framestats, configuration, UI and device metadata. All six traces contain application frame timelines and report zero nonzero error/data-loss statistics. Their application timeline rows/misses differ from gfxinfo counters and must not be interchanged:

| Mode/run | App timeline rows | App-deadline rows | SurfaceFlinger CPU-deadline rows |
|---|---:|---:|---:|
| List 1 | 235 | 82 | 217 |
| List 2 | 257 | 85 | 240 |
| List 3 | 230 | 88 | 221 |
| Grid 1 | 248 | 94 | 232 |
| Grid 2 | 232 | 83 | 215 |
| Grid 3 | 217 | 81 | 201 |

In list run 1, main-thread `AndroidOwner:measureAndLayout` averages 0.57 ms (286 slices, max 4.78 ms); idle prefetch averages 2.05 ms (35, max 5.05), prefetch measure 0.97 ms (59, max 4.81). Main-thread `postAndWait` averages 15.10 ms (244, max 54.16). Scheduler-running totals over the trace are 734.27 ms for main and 2326.31 ms for RenderThread. Inclusive slice durations overlap; none are summed as separate CPU costs. See `list-main-analysis.txt` and each `run-N-analysis.txt`.

A separate bounded 20-second grid CPU sample, same APK and six-up/six-down flow, recorded 1,120 cpu-clock samples at 1 kHz, zero samples lost, approximately 1.12 CPU seconds. RenderThread accounts for 58.93% inclusive; EGL buffer swapping approximately 42.05% inclusive. `__vdso_clock_gettime` is 48.93% self across threads. These percentages overlap down the call tree. The capture does not identify a dominant app-owned Kotlin function. Kernel symbol addresses are restricted; no device security setting was changed. Files: `grid-cpu.data`, `simpleperf-self.txt`, `simpleperf-inclusive.txt`, and recording diagnostics.

The capture APK's R8 mapping ID is `ba7d36bb77bcd2cc9e8b400f8fbf26cf66a0d6a23384afe90fbfe4ec6cd82598`. Its matching mapping was not retained. Later mappings cannot resolve its obfuscated APK frames. Repeat the same dataset, APK, compilation and warm-up while changing one rendering condition, with three repetitions per condition. Preserve the matching mapping beside the capture APK so the next sample can distinguish app-owned frames.

The emulator uses the NVIDIA-backed Android Emulator OpenGL ES translator (GTX 1650), not a physical Android GPU. Together, scheduler/frame timelines and sampled stacks implicate rendering/buffer-wait pressure in this environment. This is an inference, not proof that every missed frame has the same cause or that app drawing can never improve. It does not justify rewriting Compose rows from old overlapping debug slice totals. No speculative rendering patch or dashboard timing improvement is claimed. Physical validation remains outside the user's emulator-only scope.

## Separate index evidence

`SearchIndexRecoveryTest.rebuild_usesOneBulkIconReadAtEveryLibrarySize` passes with exactly one icon query at 100, 1,000 and 5,000 links. An isolated API 36 **debug data-test APK** run, with no Gradle work during it, measured 1008, 1491 and 5899 ms respectively. These are single observations including AppSearch document work, not optimized runtime budgets or controlled before/after timings. Previous code performed one icon query per website; current source materializes six coherent bulk source reads, then releases Room before AppSearch writes. Query growth is bounded; elapsed-time speedup is not claimed.

That multi-second full rebuild also exposed a separate search latency mechanism: eager reads could wait for the owner before Room fallback ran. Search/suggestion reads now return fallback promptly when the owner is busy or the index is unready, with bounded coalesced repair in the background. Barrier tests cover useful Room results while rebuild remains held; final freshness execution is recorded in the ledger.

## Capture attempts and limits

The first Perfetto attempt could not read a config from `/data/local/tmp` under SELinux; configs now use `/data/misc/perfetto-configs`. Initial grid setup found the header scrolled out of view, then incorrectly expected native `selected` rather than the enclosing control's `checked` state. The script now scrolls to the header and validates the exported checked/selected ancestor. Failed setup directories contain no measured runs. List selection was independently confirmed from the saved tree; grid rendering was visually inspected. No failed setup is counted among the six results.

No Gradle work or second emulator ran during the six captures or CPU sample. The minimum-API emulator was started afterward. Host background load and emulator graphics scheduling still limit timing conclusions. No leak or retained-object claim follows from these captures. The original debug trace is preserved unchanged.
