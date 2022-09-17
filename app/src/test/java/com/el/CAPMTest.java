package com.el;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CAPMTest
{
    @Test
    public void calculateMeanMarketReturns() {
        final var tradeDate = LocalDate.of(2022, 9, 1);
        final var es = new SymbolStatisticsRepository(tradeDate);
        final var indexReturns = es.getPastIndexReturns();
        final var erm = CAPM.calculateMeanMarketReturns(indexReturns);
        assertEquals(indexReturns.values().stream().reduce(100.0, (a, b) -> a * (1 + b)),
                indexReturns.values().stream().reduce(100.0, (a, b) -> a * (1 + erm)), 1e-10);
    }

    @Disabled
    @Test
    public void testCalculateStockBeta() {
        // fixme: verify with real input and output
        // final var beta = CAPM.calculateStockBeta(stockReturns, indexReturns);
    }
}
