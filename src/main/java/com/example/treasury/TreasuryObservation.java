package com.example.treasury;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TreasuryObservation(LocalDate date, BigDecimal value) {
}
