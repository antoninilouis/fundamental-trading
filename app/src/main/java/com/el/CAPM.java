package com.el;

import org.apache.commons.math3.stat.regression.SimpleRegression;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class CAPM {

    static double compute(
        final Map<LocalDate, Double> indexPrices,
        final Map<LocalDate, Double> stockPrices,
        final Map<LocalDate, Double> tBillsReturns,
        final LocalDate date
    ) {
        final var indexReturns = CAPM.toReturnPercents(indexPrices);
        final var stockReturns = CAPM.toReturnPercents(stockPrices);
        // E(Rm)
        var erm = calculateMeanMarketReturns(indexReturns);
        // rf
        var rf = tBillsReturns.get(date);
        // Bi
        var beta = calculateStockBeta(stockReturns, indexReturns);
        return rf + beta * (erm - rf);
    }

    static double calculateMeanMarketReturns(final Map<LocalDate, Double> indexReturns) {
        return Math.pow(indexReturns.values().stream().reduce(1.0, (a, b) -> a * (1 + b)), 1.0 / indexReturns.size()) - 1.0;
    }

    static double calculateStockBeta(
            final Map<LocalDate, Double> stockReturns,
            final Map<LocalDate, Double> indexReturns
    ) {
        final var reg = new SimpleRegression();
        for (var entry : stockReturns.entrySet()) {
            if (!indexReturns.containsKey(entry.getKey())) {
                continue;
            }
            // SimpleRegression.addData
            reg.addData(entry.getValue(), indexReturns.get(entry.getKey()));
        }
        return reg.getSlope();
    }

    static LinkedHashMap<LocalDate, Double> toReturnPercents(final Map<LocalDate, Double> prices) {
        final var copy = getCopy(prices);
        final var iterator = copy.entrySet().iterator();
        final var firstEntry = iterator.next();
        var previousValue = firstEntry.getValue();

        firstEntry.setValue(0.0);
        while (iterator.hasNext()) {
            var entry = iterator.next();
            var tmp = entry.getValue();
            entry.setValue(entry.getValue() / previousValue - 1);
            previousValue = tmp;
        }
        return copy;
    }

    private static LinkedHashMap<LocalDate, Double> getCopy(Map<LocalDate, Double> prices) {
        return prices.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (o1, o2) -> o1, LinkedHashMap::new));
    }
}
