package com.el;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CAPMTest
{
    @Test
    public void calculateMeanMarketReturns() {
        final var es = new SymbolStatisticsRepository();
        final var indexReturns = es.getIndexReturns();
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
