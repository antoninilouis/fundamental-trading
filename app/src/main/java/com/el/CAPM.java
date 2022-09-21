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
        // todo: verify if erm needs to be annual or daily
        // E(Rm)
        var erm = calculateMeanMarketReturns(indexReturns);
        // todo: verify if rf is in % or decimal
        // rf
        var rf = tBillsReturns.entrySet().stream().max(Map.Entry.comparingByKey()).orElseThrow().getValue() / 100.0;
        // Bi
        var beta = symbolStatisticsRepository.getStockRegressionResults(symbol).getSlope();
        return rf + beta * (erm - rf);
    }

    /**
     * Computes geometric mean annual return
     * @param indexReturns index returns daily
     * @return decimal value, e.g 0.05 for 5%
     */
    public static double calculateMeanMarketReturns(final Map<LocalDate, Double> indexReturns) {
        var daily = Math.pow(indexReturns.values().stream().reduce(1.0, (a, b) -> a * (1.0 + b)), 1.0 / indexReturns.size()) - 1.0;
        return Math.pow((daily + 1.0), 365) - 1.0;
    }
}
