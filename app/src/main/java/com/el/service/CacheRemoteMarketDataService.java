package com.el.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Set;

public class CacheRemoteMarketDataService {

  private static final Logger logger = LoggerFactory.getLogger(CacheRemoteMarketDataService.class);
  private static final FMPService fmpService = new FMPService();
  private static final FundamentalTradingDbFacade fundamentalTradingDbFacade = new FundamentalTradingDbFacade();
  private static final LocalDate MIN_DATE = LocalDate.of(2012, 1, 1);
  private static final LocalDate MAX_DATE = LocalDate.of(2022, 9, 1);

  public static void fillStockPricesCache(Set<String> symbols) {
    final var stockPrices = fmpService.getStockPrices(symbols,
      MIN_DATE.atStartOfDay(ZoneId.of("America/New_York")).toInstant(),
      MAX_DATE.atStartOfDay(ZoneId.of("America/New_York")).toInstant());
    stockPrices.forEach(fundamentalTradingDbFacade::insertStockPrices);
  }

  public static void fillIndexPricesCache(String index) {
    final var indexPrices = fmpService.getIndexPrices(index,
      MIN_DATE.atStartOfDay(ZoneId.of("America/New_York")).toInstant(),
      MAX_DATE.atStartOfDay(ZoneId.of("America/New_York")).toInstant());
    fundamentalTradingDbFacade.insertIndexPrices(index, indexPrices);
  }

  public static void fillTbReturnsCache() {
    final var tbReturns = fmpService.getTbReturns(
      MIN_DATE.atStartOfDay(ZoneId.of("America/New_York")).toInstant(),
      MAX_DATE.atStartOfDay(ZoneId.of("America/New_York")).toInstant());
    fundamentalTradingDbFacade.insertTbReturns(tbReturns);
  }

}
