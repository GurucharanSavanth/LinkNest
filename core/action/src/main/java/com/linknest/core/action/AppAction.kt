package com.linknest.core.action

interface AppAction<Input, Output> {
    suspend operator fun invoke(input: Input): ActionResult<Output>
}
