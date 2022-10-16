package com.el.marketdata;

import com.el.service.FMPService;
import com.el.service.FundamentalTradingDbFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Reduce nb requests when live trading:
 * - History: pull and save prices in [2012-01-01, Previous Day] in a table [Stock, LocalDate, Double]
 *   - Oldest data point is always at date 2012-01-01
 *   - Newest data point is always the last one inserted
 * - Refresh: pull and save prices in [Newest, Previous Day]
 *   - /v3/ratios can't be multi company
 *   - /v3/historical-price-full can be multi company (up to 250pts per company)
 */
public class CacheRemoteMarketDataRepository extends MarketDataRepository {

  private static final Logger logger = LoggerFactory.getLogger(CacheRemoteMarketDataRepository.class);
  private static final FMPService fmpService = new FMPService();
  private static final FundamentalTradingDbFacade fundamentalTradingDbFacade = new FundamentalTradingDbFacade();
  private static final LocalDate MIN_DATE = LocalDate.of(2012, 1, 1);
  private static final LocalDate MAX_DATE = LocalDate.of(2022, 9, 1);
  private Boolean fillCache;

  public CacheRemoteMarketDataRepository(
    final LocalDate tradeDate,
    final Instant from,
    final Instant to,
    final Boolean fillCache
  ) {
    super(tradeDate);
    this.fillCache = fillCache;
    initialize(from, to);
  }

  public static void fillCache(Set<String> symbols) {
    final var stockPrices = fmpService.getStockPrices(symbols,
      MIN_DATE.atStartOfDay(ZoneId.of("America/New_York")).toInstant(),
      MAX_DATE.atStartOfDay(ZoneId.of("America/New_York")).toInstant());
    stockPrices.forEach(fundamentalTradingDbFacade::insertStockPrices);
  }

  /**
   * The cache stores data for the cache period [MIN_DATE, MAX_DATE]
   * All symbols present in cache have all available data for the cache period
   * @return stock prices retrieved from cache or from remote source if not present in cache
   */
  @Override
  protected Map<String, TreeMap<LocalDate, Double>> getStockPrices(Set<String> symbols, Instant from, Instant to) {
    if (this.fillCache) {
      fillCache(symbols);
    }
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
