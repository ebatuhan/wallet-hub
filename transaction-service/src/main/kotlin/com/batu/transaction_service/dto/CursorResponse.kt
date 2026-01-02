package com.batu.transaction_service.dto


public data class CursorResponse<T>(
        val data: List<T>,
        val hasMore: Boolean,
        val nextCursor: String? = null
) {}
