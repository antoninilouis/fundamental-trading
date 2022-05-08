package com.el;

import org.junit.Test;

import java.util.LinkedHashMap;

import static org.junit.Assert.assertEquals;

/**
 * Unit test for simple App.
 */
public class ApplicationTest
{

    @Test
    public void testLoadResources()
    {
        final var spReturns = Application.extractSPReturns();
        assertEquals(spReturns.size(), 2516);
        final var tBillsReturns =  Application.extractTBillsReturns();
        assertEquals(tBillsReturns.size(), 17075);
//        App.extractStockReturns();
//        App.calculateStockBeta();
//        App.calculateExpectedReturnsOnMarket();
    }

    @Test
    public void testConversionToReturnPercents()
    {
        final var prices = new LinkedHashMap<String, Double>();
        prices.put("2012-01-03", 77.680000);
        prices.put("2012-01-04", 76.800003);
        prices.put("2012-01-05", 76.930000);
        prices.put("2012-01-06", 76.389999);
        prices.put("2012-01-09", 76.290001);
        prices.put("2012-01-10", 77.419998);
        prices.put("2012-01-11", 78.000000);
        prices.put("2012-01-12", 78.500000);
        prices.put("2012-01-13", 77.769997);
        prices.put("2012-01-17", 77.970001);

        final var returns = new LinkedHashMap<String, Double>();
        returns.put("2012-01-03", 0.0);
        returns.put("2012-01-04", -0.011328488671472736);
        returns.put("2012-01-05", 0.0016926692047134484);
        returns.put("2012-01-06", -0.007019381255687018);
        returns.put("2012-01-09", -0.0013090457037445713);
        returns.put("2012-01-10", 0.014811862435288203);
        returns.put("2012-01-11", 0.007491630263281479);
        returns.put("2012-01-12", 0.0064102564102563875);
        returns.put("2012-01-13", -0.009299401273885288);
        returns.put("2012-01-17", 0.002571737272922814);

        Application.toReturnPercents(prices);
        assertEquals(prices, returns);
    }

}
