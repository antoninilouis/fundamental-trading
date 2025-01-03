package com.el.marketdata;

import com.el.service.FMPService;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class RemoteMarketDataRepository extends MarketDataRepository {

  private final FMPService fmpService = new FMPService();

  public RemoteMarketDataRepository(final Set<String> symbols, final LocalDate tradeDate, final Instant from, final Instant to) {
    super(symbols, tradeDate);
    this.initialize(from, to);
  }

  @Override
  protected Map<String, TreeMap<LocalDate, Double>> getStockPrices(Set<String> symbols, Instant from, Instant to) {
    return fmpService.getStockPrices(symbols, from, to);
  }

  @Override
  protected TreeMap<LocalDate, Double> getIndexPrices(Instant from, Instant to) {
    return fmpService.getIndexPrices(INDEX_NAME, from, to);
  }

  @Override
  protected TreeMap<LocalDate, Double> getTbReturns(Instant from, Instant to) {
    return fmpService.getTbReturns(from, to);
  }

  @Override
  protected Map<String, TreeMap<LocalDate, Double>> getStockDividends(Set<String> symbols, Instant from, Instant to) {
    return fmpService.getStockDividends(symbols, from, to);
  }

  @Override
  protected Map<String, TreeMap<LocalDate, Double>> getStockReturnOnEquity(Set<String> symbols, Instant from, Instant to) {
    return fmpService.getStockReturnOnEquity(symbols, from, to);
  }

  @Override
  protected Map<String, TreeMap<LocalDate, Double>> getStockDividendPayoutRatio(Set<String> symbols, Instant from, Instant to) {
    return fmpService.getStockDividendPayoutRatio(symbols, from, to);
  }
}
