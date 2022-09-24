package com.el.stockselection;

import com.el.marketdata.MarketDataRepository;

import java.time.LocalDate;
import java.util.Map;

public class CAPM {

    static double compute(
        final MarketDataRepository marketDataRepository,
        final String symbol
    ) {
        final var indexReturns = marketDataRepository.getPastIndexReturns();
        final var tBillsReturns = marketDataRepository.getPastTbReturns();
        // E(Rm)
        var erm = calculateMeanMarketReturns(indexReturns);
        // todo: use the 52-weeks (annual return) instead of 13-weeks
        // rf T-Bills returns are in %
        var rf = tBillsReturns.entrySet().stream().max(Map.Entry.comparingByKey()).orElseThrow().getValue() / 100.0;
        // Bi
        var beta = marketDataRepository.getStockRegressionResults(symbol).getSlope();
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
