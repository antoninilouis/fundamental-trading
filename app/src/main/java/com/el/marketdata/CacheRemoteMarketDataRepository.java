package com.el.marketdata;

import com.el.service.FMPService;
import com.el.service.FundamentalTradingDbFacade;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Fit in 300rpm limit when backtesting:
 * - History: pull and save prices in [2012-01-01, 2022-10-01] in a table [Stock, LocalDate, Double]
 *   - Oldest data point is always at date 2012-01-01
 *   - Newest data point is always at date 2022-10-01
 * - Rate limit FMPService (will take 20+ minutes to load the data so should be done once)
 * - 1/ batch pull data using FMPService, 2/ persist all the data, 3/ make FMPService able to retrieve from it
 * - Only enable this behavior if an instance of FundamentalTradingDbFacade is provided
 *
 * Reduce nb requests when live trading:
 * - History: pull and save prices in [2012-01-01, Previous Day] in a table [Stock, LocalDate, Double]
 *   - Oldest data point is always at date 2012-01-01
 *   - Newest data point is always the last one inserted
 * - Refresh: pull and save prices in [Newest, Previous Day]
 *   - /v3/ratios can't be multi company
 *   - /v3/historical-price-full can be multi company (up to 250pts per company)
 */
public class CacheRemoteMarketDataRepository extends MarketDataRepository {

  private final FMPService fmpService = new FMPService();
  private final FundamentalTradingDbFacade fundamentalTradingDbFacade;
  private final LocalDate MIN_DATE = LocalDate.of(2012, 1, 1);
  private final LocalDate MAX_DATE = LocalDate.of(2022, 9, 1);

  public CacheRemoteMarketDataRepository(
    final LocalDate tradeDate,
    final Instant from,
    final Instant to
  ) {
    super(tradeDate);
    this.fundamentalTradingDbFacade = new FundamentalTradingDbFacade();
    this.initialize(from, to);
  }

  @Override
  protected Map<String, TreeMap<LocalDate, Double>> getStockPrices(Set<String> symbols, Instant from, Instant to) {
    // todo: the cache will always store data from period [2012-01-01, 2022-09-01]
    //  any date outside of this is forbidden
    //  any symbol present in db will be considered to have all the data,
    final var stockPrices = fundamentalTradingDbFacade.getCachedStockPrices(symbols, from, to);
    symbols.removeIf(stockPrices::containsKey);
    stockPrices.putAll(fmpService.getStockPrices(symbols, from, to));
    return stockPrices;
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
