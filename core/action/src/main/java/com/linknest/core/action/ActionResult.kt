package com.linknest.core.action

import kotlinx.coroutines.CancellationException

data class ActionIssue(
    val code: String,
    val message: String,
)

sealed interface ActionResult<out T> {
    data class Success<T>(
        val value: T,
        val issues: List<ActionIssue> = emptyList(),
    ) : ActionResult<T>

    data class PartialSuccess<T>(
        val value: T,
        val issues: List<ActionIssue>,
    ) : ActionResult<T>

    data class Failure(
        val issue: ActionIssue,
        val throwable: Throwable? = null,
    ) : ActionResult<Nothing>
}

fun <T> ActionResult<T>.valueOrNull(): T? = when (this) {
    is ActionResult.Success -> value
    is ActionResult.PartialSuccess -> value
    is ActionResult.Failure -> null
}

suspend inline fun <T> actionResult(
    code: String,
    defaultMessage: String,
    crossinline block: suspend () -> T,
): ActionResult<T> = try {
    ActionResult.Success(block())
} catch (cancellationException: CancellationException) {
    throw cancellationException
} catch (throwable: Throwable) {
    ActionResult.Failure(
        issue = ActionIssue(
            code = code,
            message = throwable.message ?: defaultMessage,
        ),
        throwable = throwable,
    )
}
