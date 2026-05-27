package com.namey.usecase

import com.namey.domain.model.FunctionContext
import com.namey.domain.model.SuggestionResult
import com.namey.domain.port.NamingPort

class SuggestNameUseCase(private val namingPort: NamingPort) {
    suspend fun execute(context: FunctionContext, count: Int = 3): SuggestionResult {
        return namingPort.suggest(context, count)
    }
}
