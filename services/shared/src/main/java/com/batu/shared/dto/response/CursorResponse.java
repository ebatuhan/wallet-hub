package com.batu.shared.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CursorResponse<T> {

    private List<T> data;
    private boolean hasMore;
    private String nextCursor;
}
