package com.el.marketdata;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class RemoteMarketDataRepository extends MarketDataRepository {

  public RemoteMarketDataRepository(final LocalDate tradeDate, final Instant from, final Instant to) {
    super(tradeDate, from, to);
  }

  @Override
  protected Map<String, TreeMap<LocalDate, Double>> getStockPrices(Set<String> symbols, Instant from, Instant to) {
    final FMPService fmpService = new FMPService();
    return fmpService.getStockPrices(symbols, from, to);
  }

  @Override
  protected TreeMap<LocalDate, Double> getIndexPrices(Instant from, Instant to) {
    final FMPService fmpService = new FMPService();
    return fmpService.getIndexPrices(INDEX_NAME, from, to);
  }

  @Override
  protected TreeMap<LocalDate, Double> getTbReturns(Instant from, Instant to) {
    return null;
  }

  @Override
  protected Map<String, TreeMap<LocalDate, Double>> getStockDividends(Set<String> allSymbols, Instant from, Instant to) {
    return null;
  }
}
