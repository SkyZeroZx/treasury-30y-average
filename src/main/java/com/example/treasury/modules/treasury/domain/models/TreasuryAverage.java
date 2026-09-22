package com.example.treasury.modules.treasury.domain.models;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TreasuryAverage(LocalDate from, LocalDate to, BigDecimal average) {
}
