package com.el.marketdata;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MarketDataRepositoryTest {

  @Test
  public void testLoadResources() {
    final var tradeDate = LocalDate.of(2022, 5, 27);
    final var es = new LocalMarketDataRepository(
      tradeDate,
      ZonedDateTime.of(LocalDate.of(2015, 12, 1), LocalTime.MIDNIGHT, ZoneId.of("America/New_York")).toInstant(),
      ZonedDateTime.of(LocalDate.of(2022, 9, 1), LocalTime.MIDNIGHT, ZoneId.of("America/New_York")).toInstant()
    );
    final TreeMap<LocalDate, Double> indexPrices = es.getIndexPrices();
    final TreeMap<LocalDate, Double> tBillsReturns = es.getPastTbReturns();

    assertEquals(1701, indexPrices.size());
    assertEquals(1.06, tBillsReturns.get(LocalDate.of(2022, 5, 26)), 1e-3);
    assertEquals(853, tBillsReturns.size());
  }

  @Test
  public void testConversionToReturnPercents() {
    final var stockPrices = new TreeMap<LocalDate, Double>();
    stockPrices.put(LocalDate.parse("2021-05-03"), 77.680000);
    stockPrices.put(LocalDate.parse("2021-05-04"), 76.800003);
    stockPrices.put(LocalDate.parse("2021-05-05"), 76.930000);
    stockPrices.put(LocalDate.parse("2021-05-06"), 76.389999);
    stockPrices.put(LocalDate.parse("2021-05-09"), 76.290001);
    stockPrices.put(LocalDate.parse("2021-05-10"), 77.419998);
    stockPrices.put(LocalDate.parse("2021-05-11"), 78.000000);
    stockPrices.put(LocalDate.parse("2021-05-12"), 78.500000);
    stockPrices.put(LocalDate.parse("2021-05-13"), 77.769997);
    stockPrices.put(LocalDate.parse("2021-05-17"), 77.970001);

    final var stockReturns = new TreeMap<>();
    stockReturns.put(LocalDate.parse("2021-05-03"), 0.0);
    stockReturns.put(LocalDate.parse("2021-05-04"), -0.011328488671472736);
    stockReturns.put(LocalDate.parse("2021-05-05"), 0.0016926692047134484);
    stockReturns.put(LocalDate.parse("2021-05-06"), -0.007019381255687018);
    stockReturns.put(LocalDate.parse("2021-05-09"), -0.0013090457037445713);
    stockReturns.put(LocalDate.parse("2021-05-10"), 0.014811862435288203);
    stockReturns.put(LocalDate.parse("2021-05-11"), 0.007491630263281479);
    stockReturns.put(LocalDate.parse("2021-05-12"), 0.0064102564102563875);
    stockReturns.put(LocalDate.parse("2021-05-13"), -0.009299401273885288);
    stockReturns.put(LocalDate.parse("2021-05-17"), 0.002571737272922814);

    assertEquals(MarketDataRepository.toReturnPercents(stockPrices), stockReturns);
  }
}