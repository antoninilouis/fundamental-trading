package com.el.marketdata;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class LocalMarketDataRepository extends MarketDataRepository {

    public LocalMarketDataRepository(LocalDate tradeDate, final Instant from, final Instant to) {
        super(tradeDate, null, null);
    }

    @Override
    protected Map<String, LinkedHashMap<LocalDate, Double>> getStockPrices(Set<String> symbols, Instant from, Instant to) {
        final Map<String, LinkedHashMap<LocalDate, Double>> stockPrices = new HashMap<>();
        symbols.forEach(symbol -> stockPrices.put(symbol, extractDatedValues(symbol, ResourceTypes.PRICES)));
        return stockPrices;
    }

    @Override
    protected LinkedHashMap<LocalDate, Double> getIndexPrices(Instant from, Instant to) {
        return extractDatedValues(INDEX_NAME, ResourceTypes.PRICES);
    }
}
