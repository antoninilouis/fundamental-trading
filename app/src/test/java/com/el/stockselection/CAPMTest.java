package com.el.stockselection;

import com.el.financeutils.CAPM;
import com.el.marketdata.LocalMarketDataRepository;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CAPMTest {
  @Test
  public void calculateMeanMarketReturns() {
    final var tradeDate = LocalDate.of(2022, 9, 1);
    final var es = new LocalMarketDataRepository(
      Set.of("AAPL"),
      tradeDate,
      ZonedDateTime.of(LocalDate.of(2015, 12, 1), LocalTime.MIDNIGHT, ZoneId.of("America/New_York")).toInstant(),
      ZonedDateTime.of(LocalDate.of(2022, 9, 1), LocalTime.MIDNIGHT, ZoneId.of("America/New_York")).toInstant()
    );
    final var indexReturns = es.getPastIndexReturns();
    final var erm = toDailyReturn(CAPM.calculateMeanMarketReturns(indexReturns));
    assertEquals(indexReturns.values().stream().reduce(100.0, (a, b) -> a * (1 + b)),
      indexReturns.values().stream().reduce(100.0, (a, b) -> a * (1 + erm)), 1e-10);
  }

  private double toDailyReturn(Double meanMarketReturn) {
    return Math.pow(meanMarketReturn + 1.0, 1.0 / 365.0) - 1.0;
  }

  @Disabled
  @Test
  public void testCalculateStockBeta() {
    // fixme: verify with real input and output
    // final var beta = CAPM.calculateStockBeta(stockReturns, indexReturns);
  }
}
