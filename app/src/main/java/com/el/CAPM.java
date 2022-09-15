package com.el;

import java.time.LocalDate;
import java.util.Map;

public class CAPM {

    static double compute(
        final SymbolStatisticsRepository symbolStatisticsRepository,
        final String symbol,
        final LocalDate date
    ) {
        final var indexReturns = symbolStatisticsRepository.getIndexReturns();
        final var tBillsReturns = symbolStatisticsRepository.getTbReturns();
        // E(Rm)
        var erm = calculateMeanMarketReturns(indexReturns);
        // rf
        var rf = tBillsReturns.get(date);
        // Bi
        var beta = symbolStatisticsRepository.getStockRegressionResults().get(symbol).getSlope();
        return rf + beta * (erm - rf);
    }

    static double calculateMeanMarketReturns(final Map<LocalDate, Double> indexReturns) {
        return Math.pow(indexReturns.values().stream().reduce(1.0, (a, b) -> a * (1 + b)), 1.0 / indexReturns.size()) - 1.0;
    }
}
