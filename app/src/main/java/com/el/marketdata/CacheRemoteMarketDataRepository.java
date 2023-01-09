package com.el.marketdata;

import com.el.service.FMPService;
import com.el.service.FundamentalTradingDbFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Before use, fill the cache for the entire backtest period e.g. using CacheRemoteMarketDataService to limit api calls
 *
 * As part of this naive cache implementation:
 * Any record in cache is interpreted as if ALL the data for that metric and symbol is in cache and so needs not be fetched
 * In the absence of a record, all the data for that metric and symbol is fetched but NOT SAVED in cache
 */
public class CacheRemoteMarketDataRepository extends MarketDataRepository {

  private static final Logger logger = LoggerFactory.getLogger(CacheRemoteMarketDataRepository.class);
  private static final FMPService fmpService = new FMPService();
  private static final FundamentalTradingDbFacade fundamentalTradingDbFacade = new FundamentalTradingDbFacade();

  public CacheRemoteMarketDataRepository(
    final Set<String> symbols,
    final LocalDate tradeDate,
    final Instant from,
    final Instant to
  ) {
    super(symbols, tradeDate);
    initialize(from, to);
  }

  @Override
  protected Map<String, TreeMap<LocalDate, Double>> getStockPrices(Set<String> symbols, Instant from, Instant to) {
    final var stockPrices = fundamentalTradingDbFacade.getCachedStockPrices(symbols, from, to);
    stockPrices.putAll(fmpService.getStockPrices(
      symbols.stream().filter(s -> !stockPrices.containsKey(s)).collect(Collectors.toSet()),
      from,
      to
    ));
    return stockPrices;
  }

  @Override
  protected TreeMap<LocalDate, Double> getIndexPrices(Instant from, Instant to) {
    final var indexPrices = fundamentalTradingDbFacade.getCachedIndexPrices(INDEX_NAME, from, to);
    if (indexPrices.isEmpty()) {
      indexPrices.putAll(fmpService.getIndexPrices(INDEX_NAME, from, to));
    }
    return indexPrices;
  }

  @Override
  protected TreeMap<LocalDate, Double> getTbReturns(Instant from, Instant to) {
    final var tbReturns = fundamentalTradingDbFacade.getCachedTbReturns(from, to);
    if (tbReturns.isEmpty()) {
      tbReturns.putAll(fmpService.getTbReturns(from, to));
    }
    return tbReturns;
  }

  /**
   * Note: getStockDividends is called in the absence of at least an entry in stock_dividends table,
   * therefore it is always called for symbols which did not issue dividends
   */
  @Override
  protected Map<String, TreeMap<LocalDate, Double>> getStockDividends(Set<String> symbols, Instant from, Instant to) {
    final var stockDividends = fundamentalTradingDbFacade.getCachedStockDividends(symbols, from, to);
    stockDividends.putAll(fmpService.getStockDividends(
      symbols.stream().filter(s -> !stockDividends.containsKey(s)).collect(Collectors.toSet()),
      from,
      to
    ));
    return stockDividends;
  }

  @Override
  protected Map<String, TreeMap<LocalDate, Double>> getStockReturnOnEquity(Set<String> symbols, Instant from, Instant to) {
    final var stockROE = fundamentalTradingDbFacade.getCachedStockReturnOnEquity(symbols, from, to);
    stockROE.putAll(fmpService.getStockReturnOnEquity(
      symbols.stream().filter(s -> !stockROE.containsKey(s)).collect(Collectors.toSet()),
      from,
      to
    ));
    return stockROE;
  }

  @Override
  protected Map<String, TreeMap<LocalDate, Double>> getStockDividendPayoutRatio(Set<String> symbols, Instant from, Instant to) {
    final var stockDividendPayoutRatio = fundamentalTradingDbFacade.getCachedStockDividendPayoutRatio(symbols, from, to);
    stockDividendPayoutRatio.putAll(fmpService.getStockDividendPayoutRatio(
      symbols.stream().filter(s -> !stockDividendPayoutRatio.containsKey(s)).collect(Collectors.toSet()),
      from,
      to
    ));
    return stockDividendPayoutRatio;
  }
}
