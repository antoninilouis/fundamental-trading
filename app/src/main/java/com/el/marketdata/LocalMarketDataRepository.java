package com.el.marketdata;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class LocalMarketDataRepository extends MarketDataRepository {

    public LocalMarketDataRepository(LocalDate tradeDate, final Instant from, final Instant to) {
        super(tradeDate, null, null);
    }

    @Override
    protected Map<String, TreeMap<LocalDate, Double>> getStockPrices(Set<String> symbols, Instant from, Instant to) {
        final Map<String, TreeMap<LocalDate, Double>> stockPrices = new HashMap<>();
        symbols.forEach(symbol -> stockPrices.put(symbol, extractDatedValues(symbol, ResourceTypes.PRICES)));
        return stockPrices;
    }

    @Override
    protected TreeMap<LocalDate, Double> getIndexPrices(Instant from, Instant to) {
        return extractDatedValues(INDEX_NAME, ResourceTypes.PRICES);
    }
}
