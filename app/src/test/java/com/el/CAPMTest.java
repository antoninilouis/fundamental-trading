package com.el;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.LinkedHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CAPMTest
{

    private static LinkedHashMap<LocalDate, Double> stockPrices;

    @BeforeAll
    public static void setUp() {
        stockPrices = new LinkedHashMap<>();
        stockPrices.put(LocalDate.parse("2021-05-03"), 77.680000);
        stockPrices.put(LocalDate.parse("2021-05-04"), 76.800003);
        stockPrices.put(LocalDate.parse("2021-05-05"), 76.930000);
        stockPrices.put(LocalDate.parse("2021-05-06"), 76.389999);
        stockPrices.put(LocalDate.parse("2021-05-09"), 76.290001);
        stockPrices.put(LocalDate.parse("2021-05-10"), 77.419998);
        stockPrices.put(LocalDate.parse("2021-05-11"), 78.000000);
        stockPrices.put(LocalDate.parse("2021-05-12"), 78.500000);
        stockPrices.put(LocalDate.parse("2021-05-13"), 77.769997);
        stockPrices.put(LocalDate.parse("2021-05-17"), 77.970001);

    }

    @Test
    public void testConversionToReturnPercents()
    {
        final var stockReturns = new LinkedHashMap<>();
        stockReturns.put(LocalDate.parse("2021-05-03"), 0.0);
        stockReturns.put(LocalDate.parse("2021-05-04"), -0.011328488671472736);
        stockReturns.put(LocalDate.parse("2021-05-05"), 0.0016926692047134484);
        stockReturns.put(LocalDate.parse("2021-05-06"), -0.007019381255687018);
        stockReturns.put(LocalDate.parse("2021-05-09"), -0.0013090457037445713);
        stockReturns.put(LocalDate.parse("2021-05-10"), 0.014811862435288203);
        stockReturns.put(LocalDate.parse("2021-05-11"), 0.007491630263281479);
        stockReturns.put(LocalDate.parse("2021-05-12"), 0.0064102564102563875);
        stockReturns.put(LocalDate.parse("2021-05-13"), -0.009299401273885288);
        stockReturns.put(LocalDate.parse("2021-05-17"), 0.002571737272922814);

        final var returns = CAPM.toReturnPercents(stockPrices);
        assertEquals(returns, stockReturns);
    }

    @Test
    public void calculateMeanMarketReturns() {
        final var es = new SymbolStatisticsRepository();
        final var indexReturns = (LinkedHashMap<LocalDate, Double>) CAPM.toReturnPercents(es.getIndexPrices());
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
