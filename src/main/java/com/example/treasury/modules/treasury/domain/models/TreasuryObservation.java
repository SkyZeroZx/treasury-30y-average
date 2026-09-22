package com.example.treasury.modules.treasury.domain.models;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TreasuryObservation(LocalDate date, BigDecimal value) {
}
