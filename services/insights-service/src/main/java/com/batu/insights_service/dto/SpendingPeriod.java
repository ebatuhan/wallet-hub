package com.batu.insights_service.dto;

import java.time.Year;
import java.time.YearMonth;

public sealed interface SpendingPeriod permits SpendingPeriod.SpendingYear, SpendingPeriod.SpendingMonth {

    record SpendingYear(Year year) implements SpendingPeriod {
    }

    record SpendingMonth(YearMonth month) implements SpendingPeriod {
    }
}
