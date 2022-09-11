package com.el;

import org.apache.commons.math3.stat.regression.SimpleRegression;

import java.time.LocalDate;
import java.util.Map;

public class CAPM {

    static double compute(
        final Map<LocalDate, Double> marketReturns,
        final Map<LocalDate, Double> stockReturns,
        final Map<LocalDate, Double> tBillsReturns,
        final LocalDate date
    ) {
        // E(Rm)
        var erm = calculateExpectedReturnsOnMarket(marketReturns);
        //rf
        var rf = tBillsReturns.get(date);
        // Bi
        var beta = calculateStockBeta(stockReturns, marketReturns);
        return rf + beta * (erm - rf);
    }

    static double calculateExpectedReturnsOnMarket(final Map<LocalDate, Double> marketReturns) {
        return Math.pow(marketReturns.values().stream().reduce(1.0, (a, b) -> a * (1 + b)), 1.0 / marketReturns.size()) - 1.0;
    }

    static double calculateStockBeta(
            final Map<LocalDate, Double> stockReturns,
            final Map<LocalDate, Double> marketReturns
    ) {
        final var reg = new SimpleRegression();
        for (var entry : stockReturns.entrySet()) {
            if (!marketReturns.containsKey(entry.getKey())) {
                continue;
            }
            // SimpleRegression.addData
            reg.addData(entry.getValue(), marketReturns.get(entry.getKey()));
        }
        return reg.getSlope();
    }
}
