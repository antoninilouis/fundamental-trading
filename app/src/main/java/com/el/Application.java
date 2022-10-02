package com.el;

import com.el.marketdata.RemoteMarketDataRepository;
import com.el.stockselection.EquityScreener;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Algorithm to trade equities of an optimal risky portfolio based on the CAPM... every minute.
 */
public class Application {

  public static void main(String[] args) {
    final var marketDataRepository = new RemoteMarketDataRepository(
      LocalDate.of(2020, 9, 1),
      ZonedDateTime.of(LocalDate.of(2013, 9, 1), LocalTime.MIDNIGHT, ZoneId.of("America/New_York")).toInstant(),
      ZonedDateTime.of(LocalDate.of(2021, 9, 1), LocalTime.MIDNIGHT, ZoneId.of("America/New_York")).toInstant()
    );
    final var es = new EquityScreener(marketDataRepository);
    final var selection = es.screenEquities();
    final var orp = new OptimalRiskyPortfolio(marketDataRepository, selection);
    final var optimalAllocation = orp.calculate();
  }
}
