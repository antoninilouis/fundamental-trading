package com.el;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.LinkedHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SymbolStatisticsRepositoryTest {

    @Test
    public void testLoadResources()
    {
        final var es = new SymbolStatisticsRepository();
        final LinkedHashMap<LocalDate, Double> indexPrices = es.getIndexPrices();
        final LinkedHashMap<LocalDate, Double> tBillsReturns = SymbolStatisticsRepository.extractTBillsReturns();

        assertEquals(indexPrices.size(), 356);
        assertEquals(tBillsReturns.get(LocalDate.of(2022,5, 31)), 1.13, 1e-3);
        assertEquals(tBillsReturns.size(), 173);
    }
}