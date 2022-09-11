package com.el;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.LinkedHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SymbolStatisticsRepositoryTest {

    @Test
    public void testLoadResources()
    {
        final LinkedHashMap<LocalDate, Double> indexPrices = SymbolStatisticsRepository.extractDatedValues("^GSPC", SymbolStatisticsRepository.ResourceTypes.PRICES);
        final LinkedHashMap<LocalDate, Double> tBillsReturns = (LinkedHashMap<LocalDate, Double>) SymbolStatisticsRepository.extractTBillsReturns();

        assertEquals(indexPrices.size(), 356);
        assertEquals(tBillsReturns.get(LocalDate.of(2022,5, 31)), 1.13, 1e-3);
        assertEquals(tBillsReturns.size(), 173);
    }
}