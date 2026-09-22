package com.example.treasury.modules.treasury.infrastructure.controllers;

import java.time.LocalDate;

import com.example.treasury.modules.treasury.domain.models.TreasuryAverage;
import com.example.treasury.modules.treasury.usecases.gettreasuryaverage.GetTreasuryAverageUseCases;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/treasury")
@RequiredArgsConstructor
public class TreasuryController {

    private final GetTreasuryAverageUseCases getTreasuryAverageUseCases;

    @GetMapping("/30y/average")
    public Mono<TreasuryAverage> average(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return getTreasuryAverageUseCases.execute(from, to);
    }
}
