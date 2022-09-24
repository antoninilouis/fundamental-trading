package com.el.marketdata;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class AlpacaMarketDataRepository extends MarketDataRepository {

    public AlpacaMarketDataRepository(final LocalDate tradeDate, final Instant from, final Instant to) {
        super(tradeDate, from, to);
    }

    @Override
    Map<String, LinkedHashMap<LocalDate, Double>> getStockPrices(Set<String> symbols, Instant from, Instant to) {
        final AlpacaService alpacaService = new AlpacaService();
        return alpacaService.getMultiBars(symbols, from, to);
    }
}
