package com.el;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.Disabled;

import java.time.LocalDate;
import java.util.LinkedHashMap;

import static org.junit.Assert.assertEquals;

/**
 * Unit test for simple App.
 */
public class ApplicationTest
{

    private static LinkedHashMap<LocalDate, Double> stockPrices;
    private static LinkedHashMap<LocalDate, Double> stockReturns;
    private static LinkedHashMap<LocalDate, Double> marketReturns;

    @BeforeClass
    public static void setUp() {
        marketReturns = (LinkedHashMap<LocalDate, Double>) Application.extractReturns("^GSPC");

        stockPrices = new LinkedHashMap<>();
        stockPrices.put(LocalDate.parse("2012-05-03"), 77.680000);
        stockPrices.put(LocalDate.parse("2012-05-04"), 76.800003);
        stockPrices.put(LocalDate.parse("2012-05-05"), 76.930000);
        stockPrices.put(LocalDate.parse("2012-05-06"), 76.389999);
        stockPrices.put(LocalDate.parse("2012-05-09"), 76.290001);
        stockPrices.put(LocalDate.parse("2012-05-10"), 77.419998);
        stockPrices.put(LocalDate.parse("2012-05-11"), 78.000000);
        stockPrices.put(LocalDate.parse("2012-05-12"), 78.500000);
        stockPrices.put(LocalDate.parse("2012-05-13"), 77.769997);
        stockPrices.put(LocalDate.parse("2012-05-17"), 77.970001);

        stockReturns = new LinkedHashMap<>();
        stockReturns.put(LocalDate.parse("2012-05-03"), 0.0);
        stockReturns.put(LocalDate.parse("2012-05-04"), -0.011328488671472736);
        stockReturns.put(LocalDate.parse("2012-05-05"), 0.0016926692047134484);
        stockReturns.put(LocalDate.parse("2012-05-06"), -0.007019381255687018);
        stockReturns.put(LocalDate.parse("2012-05-09"), -0.0013090457037445713);
        stockReturns.put(LocalDate.parse("2012-05-10"), 0.014811862435288203);
        stockReturns.put(LocalDate.parse("2012-05-11"), 0.007491630263281479);
        stockReturns.put(LocalDate.parse("2012-05-12"), 0.0064102564102563875);
        stockReturns.put(LocalDate.parse("2012-05-13"), -0.009299401273885288);
        stockReturns.put(LocalDate.parse("2012-05-17"), 0.002571737272922814);
    }

    @Test
    public void testLoadResources()
    {
        assertEquals(marketReturns.size(), 356);
        final var tBillsReturns =  Application.extractTBillsReturnsAtDate(LocalDate.of(2022, 5, 31));
        assertEquals(tBillsReturns, 1.13, 1e-3);
    }

    @Test
    public void testConversionToReturnPercents()
    {
        Application.toReturnPercents(stockPrices);
        assertEquals(stockPrices, stockReturns);
    }

    @Test
    public void calculateExpectedReturnsOnMarket() {
        final var erm = Application.calculateExpectedReturnsOnMarket(marketReturns);
        assertEquals(marketReturns.values().stream().reduce(100.0, (a, b) -> a * (1 + b)),
                marketReturns.values().stream().reduce(100.0, (a, b) -> a * (1 + erm)), 1e-10);
    }

    @Disabled
    @Test
    public void testCalculateStockBeta() {
        final var beta = Application.calculateStockBeta(stockReturns, marketReturns);
    }
}
