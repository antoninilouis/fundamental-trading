package com.el;

import java.time.LocalDate;
import java.util.Map;

public class CAPM {

    static double compute(
        final SymbolStatisticsRepository symbolStatisticsRepository,
        final String symbol
    ) {
        final var indexReturns = symbolStatisticsRepository.getPastIndexReturns();
        final var tBillsReturns = symbolStatisticsRepository.getPastTbReturns();
        // E(Rm)
        var erm = calculateMeanMarketReturns(indexReturns);
        // rf
        var rf = tBillsReturns.entrySet().stream().max(Map.Entry.comparingByKey()).orElseThrow().getValue();
        // Bi
        var beta = symbolStatisticsRepository.getPastStockRegressionResults(symbol).getSlope();
        return rf + beta * (erm - rf);
    }

    public static double calculateMeanMarketReturns(final Map<LocalDate, Double> indexReturns) {
        return Math.pow(indexReturns.values().stream()
                .reduce(1.0, (a, b) -> a * (1 + b)), 1.0 / indexReturns.size()) - 1.0;
    }
}
