package com.example.treasury.modules.treasury.domain.adapters;

import com.example.treasury.modules.treasury.domain.models.TreasuryObservation;
import reactor.core.publisher.Flux;

public interface TreasuryServiceAdapter {

    Flux<TreasuryObservation> getObservations();
}
