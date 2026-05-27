package com.namey.domain.port

import com.namey.domain.model.FunctionContext
import com.namey.domain.model.SuggestionResult

interface NamingPort {
    suspend fun suggest(context: FunctionContext, count: Int = 3): SuggestionResult
}
