package com.batu.shared.messaging.saga;

import java.util.List;
import java.util.UUID;

import com.batu.shared.dto.request.TransactionRequestDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PersistInitialTransactionsCommand {
    private UUID sagaId;
    private UUID connectionId;
    private String nextCursor;
    private List<TransactionRequestDto> transactions;
}
