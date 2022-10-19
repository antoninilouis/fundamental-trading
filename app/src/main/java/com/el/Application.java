package com.el;

import com.el.marketdata.LiveCacheRemoteMarketDataRepository;
import com.el.stockselection.EquityScreener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Set;

public class Application {

  public static void main(String[] args) {
    final var marketDataRepository = new LiveCacheRemoteMarketDataRepository(
      extractSymbols("symbols.txt"),
      ZonedDateTime.of(LocalDate.of(2015, 12, 1), LocalTime.MIDNIGHT, ZoneId.of("America/New_York")).toInstant()
    );
    final var es = new EquityScreener(marketDataRepository);
    final var selection = es.screenEquities();
    final var orp = new OptimalRiskyPortfolio(marketDataRepository, selection);
    final var optimalAllocation = orp.calculate();
  }

  private static Set<String> extractSymbols(final String fileName) {
    final Set<String> symbols = new HashSet<>();
    final var inputStreamReader = new InputStreamReader(getFileFromResourceAsStream(fileName));
    String line;

    try (BufferedReader reader = new BufferedReader(inputStreamReader)) {
      while ((line = reader.readLine()) != null) {
        symbols.add(line);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return symbols;
  }

  private static InputStream getFileFromResourceAsStream(String fileName) {
    ClassLoader classLoader = Application.class.getClassLoader();
    InputStream inputStream = classLoader.getResourceAsStream(fileName);

    if (inputStream == null) {
      throw new IllegalArgumentException("file not found! " + fileName);
    } else {
      return inputStream;
    }
  }
}
