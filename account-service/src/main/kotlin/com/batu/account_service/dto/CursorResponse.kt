package com.batu.account_service

public data class CursorResponse<T>(
        val data: List<T>,
        val hasMore: Boolean,
        val nextCursor: String? = null
) {}
