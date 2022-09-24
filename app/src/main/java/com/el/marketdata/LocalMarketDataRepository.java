package com.el.marketdata;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class LocalMarketDataRepository extends MarketDataRepository {

    public LocalMarketDataRepository(LocalDate tradeDate) {
        super(tradeDate);
    }

    @Override
    Map<String, LinkedHashMap<LocalDate, Double>> getStockPrices(Set<String> symbols) {
        final Map<String, LinkedHashMap<LocalDate, Double>> stockPrices = new HashMap<>();
        symbols.forEach(symbol -> stockPrices.put(symbol, extractDatedValues(symbol, ResourceTypes.PRICES)));
        return stockPrices;
    }
}
