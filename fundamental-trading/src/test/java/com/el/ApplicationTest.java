package com.el;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit test for simple App.
 */
public class ApplicationTest
{

    @Test
    public void testLoadResources()
    {
        final var spReturns = Application.extractSPReturns();
        assertEquals(spReturns.size(), 2608);
        final var tBillsReturns =  Application.extractTBillsReturns();
        assertEquals(tBillsReturns.size(), 17826);
//        App.extractStockReturns();
//        App.calculateStockBeta();
//        App.calculateExpectedReturnsOnMarket();
    }

}
