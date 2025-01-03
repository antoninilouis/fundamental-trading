package com.el.marketdata;

import com.el.service.FMPService;
import com.el.service.FundamentalTradingDbFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * As part of this naive cache implementation:
 * - a record in the cache is interpreted as if SOME OLDER data for that metric and symbol is in cache,
 * - any missing cache record causes all the data for that metric and symbol to be fetched and SAVED in cache
 *
 * Usage:
 * - Before use, fill the cache as much as possible e.g. using CacheRemoteMarketDataService to limit api calls
 */
public class LiveCacheRemoteMarketDataRepository extends MarketDataRepository {

  private static final Logger logger = LoggerFactory.getLogger(LiveCacheRemoteMarketDataRepository.class);
  private static final FMPService fmpService = new FMPService();
  private final FundamentalTradingDbFacade fundamentalTradingDbFacade;
  private final LocalDate oldestRecord;
  private LocalDate latestRefresh;

  public LiveCacheRemoteMarketDataRepository(
    final String dbpath,
    final Set<String> symbols,
    final Instant from
  ) {
    super(symbols, LocalDate.now());
    this.fundamentalTradingDbFacade = new FundamentalTradingDbFacade(dbpath);
    this.oldestRecord = LocalDate.ofInstant(from, ZoneId.of("America/New_York"));
    this.latestRefresh = LocalDate.now();
    initialize(this.oldestRecord.atStartOfDay(ZoneId.of("America/New_York")).toInstant(), this.latestRefresh.atStartOfDay(ZoneId.of("America/New_York")).toInstant());
  }

  /**
   * Fetch and save data between the latest record and now, reuse data already in memory
   *
   * Refresh algorithm: pull and save prices in [Newest, Previous Day]
   * - /v3/ratios can't be multi company
   * - /v3/historical-price-full can be multi company (up to 250pts per company)
   */
  public void updateCache() {
    this.latestRefresh = LocalDate.now();
    initialize(this.oldestRecord.atStartOfDay(ZoneId.of("America/New_York")).toInstant(), this.latestRefresh.atStartOfDay(ZoneId.of("America/New_York")).toInstant());
  }

  @Override
  protected Map<String, TreeMap<LocalDate, Double>> getStockPrices(Set<String> symbols, Instant from, Instant to) {
    final var stockPrices = fundamentalTradingDbFacade.getCachedStockPrices(symbols, from, to);
    if (isUpdatedToday("STOCK_PRICES_CACHE")) {
      logger.info("Skipping stock prices cache update.");
      return stockPrices;
    }
    final var periodsToFetch = getPeriodsToFetch(stockPrices, to);
    fmpService.getStockPricesUpdates(periodsToFetch).forEach((key, value) -> {
      stockPrices.get(key).putAll(value);
      fundamentalTradingDbFacade.insertStockPrices(key, value);
    });
    fundamentalTradingDbFacade.insertRefreshHistoryEntry("STOCK_PRICES_CACHE");
    return stockPrices;
  }

  @Override
  protected TreeMap<LocalDate, Double> getIndexPrices(Instant from, Instant to) {
    final var indexPrices = fundamentalTradingDbFacade.getCachedIndexPrices(INDEX_NAME, from, to);
    if (isUpdatedToday("INDEX_PRICES_CACHE")) {
      logger.info("Skipping index prices cache update.");
      return indexPrices;
    }
    final var periodToFetch = getPeriodToFetch(indexPrices, to);
    final var indexPricesUpdates = fmpService.getIndexPricesUpdates(INDEX_NAME, periodToFetch);
    fundamentalTradingDbFacade.insertIndexPrices(INDEX_NAME, indexPricesUpdates);
    indexPrices.putAll(indexPricesUpdates);
    fundamentalTradingDbFacade.insertRefreshHistoryEntry("INDEX_PRICES_CACHE");
    return indexPrices;
  }

  @Override
  protected TreeMap<LocalDate, Double> getTbReturns(Instant from, Instant to) {
    final var tbReturns = fundamentalTradingDbFacade.getCachedTbReturns(from, to);
    if (isUpdatedToday("TB_RETURNS_CACHE")) {
      logger.info("Skipping tb returns cache update.");
      return tbReturns;
    }
    final var periodToFetch = getPeriodToFetch(tbReturns, to);
    final var tbReturnsUpdates = fmpService.getTbReturnsUpdates(periodToFetch);
    fundamentalTradingDbFacade.insertTbReturns(tbReturnsUpdates);
    tbReturns.putAll(tbReturnsUpdates);
    fundamentalTradingDbFacade.insertRefreshHistoryEntry("TB_RETURNS_CACHE");
    return tbReturns;
  }

  @Override
  protected Map<String, TreeMap<LocalDate, Double>> getStockDividends(Set<String> symbols, Instant from, Instant to) {
    final var stockDividends = fundamentalTradingDbFacade.getCachedStockDividends(symbols, from, to);
    if (isUpdatedToday("STOCK_DIVIDENDS_CACHE")) {
      logger.info("Skipping stock dividends cache update.");
      return stockDividends;
    }
    final var periodsToFetch = getPeriodsToFetch(stockDividends, to);
    fmpService.getStockDividendsUpdates(periodsToFetch).forEach((key, value) -> {
      stockDividends.get(key).putAll(value);
      fundamentalTradingDbFacade.insertStockDividends(key, value);
    });
    fundamentalTradingDbFacade.insertRefreshHistoryEntry("STOCK_DIVIDENDS_CACHE");
    return stockDividends;
  }

  @Override
  protected Map<String, TreeMap<LocalDate, Double>> getStockReturnOnEquity(Set<String> symbols, Instant from, Instant to) {
    final var stockReturnOnEquity = fundamentalTradingDbFacade.getCachedStockReturnOnEquity(symbols, from, to);
    if (isUpdatedToday("STOCK_RETURN_ON_EQUITY_CACHE")) {
      logger.info("Skipping stock return on equity update.");
      return stockReturnOnEquity;
    }
    final var periodsToFetch = getPeriodsToFetch(stockReturnOnEquity, to);
    fmpService.getStockReturnOnEquityUpdates(periodsToFetch).forEach((key, value) -> {
      stockReturnOnEquity.get(key).putAll(value);
      fundamentalTradingDbFacade.insertStockReturnOnEquity(key, value);
    });
    fundamentalTradingDbFacade.insertRefreshHistoryEntry("STOCK_RETURN_ON_EQUITY_CACHE");
    return stockReturnOnEquity;
  }

  @Override
  protected Map<String, TreeMap<LocalDate, Double>> getStockDividendPayoutRatio(Set<String> symbols, Instant from, Instant to) {
    final var stockDividendPayoutRatio = fundamentalTradingDbFacade.getCachedStockDividendPayoutRatio(symbols, from, to);
    if (isUpdatedToday("STOCK_DIVIDEND_PAYOUT_RATIO_CACHE")) {
      logger.info("Skipping stock dividend payout ratio cache update.");
      return stockDividendPayoutRatio;
    }
    final var periodsToFetch = getPeriodsToFetch(stockDividendPayoutRatio, to);
    fmpService.getStockDividendPayoutRatioUpdates(periodsToFetch).forEach((key, value) -> {
      stockDividendPayoutRatio.get(key).putAll(value);
      fundamentalTradingDbFacade.insertStockDividendPayoutRatio(key, value);
    });
    fundamentalTradingDbFacade.insertRefreshHistoryEntry("STOCK_DIVIDEND_PAYOUT_RATIO_CACHE");
    return stockDividendPayoutRatio;
  }

  private Map<String, AbstractMap.SimpleEntry<LocalDate, LocalDate>> getPeriodsToFetch(
    Map<String, TreeMap<LocalDate, Double>> map,
    Instant to
  ) {
    return map.entrySet().stream()
      .filter(e -> e.getValue().lastKey().isBefore(LocalDate.ofInstant(to, ZoneId.of("America/New_York"))))
      .collect(Collectors.toMap(
        Map.Entry::getKey,
        entry -> new AbstractMap.SimpleEntry<>(entry.getValue().lastKey().plusDays(1), LocalDate.ofInstant(to, ZoneId.of("America/New_York"))))
      );
  }

  private AbstractMap.SimpleEntry<LocalDate, LocalDate> getPeriodToFetch(
    TreeMap<LocalDate, Double> map,
    Instant to
  ) {
    return new AbstractMap.SimpleEntry<>(map.lastKey().plusDays(1), LocalDate.ofInstant(to, ZoneId.of("America/New_York")));
  }

  /**
   * Returns true if the last refresh was after today at midnight for America/New_York
   */
  private boolean isUpdatedToday(final String name) {
    final var dayStartInNY = Instant.now().atZone(ZoneId.of("America/New_York")).truncatedTo(ChronoUnit.DAYS).toInstant();
    return fundamentalTradingDbFacade.getLatestRefreshTimestamp(name)
      .filter(ts -> ts.toInstant().isAfter(dayStartInNY))
      .isPresent();
  }
}
