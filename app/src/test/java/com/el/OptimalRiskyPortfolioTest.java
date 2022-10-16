package com.el;

import com.el.marketdata.CacheRemoteMarketDataRepository;
import com.el.marketdata.LocalMarketDataRepository;
import com.el.marketdata.MarketDataRepository;
import com.el.marketdata.RemoteMarketDataRepository;
import com.el.stockselection.EquityScreener;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.Math.pow;

class OptimalRiskyPortfolioTest {

  private static final Logger logger = LoggerFactory.getLogger(OptimalRiskyPortfolioTest.class);
  private static final LocalDate TRADE_DATE = LocalDate.of(2019, 1, 3);
  private static final Double STARTING_CAPITAL = 10_000.0;

  private static double computePerformance(
    double portfolioValue,
    double time
  ) {
    // r = n[(A/P)1/nt - 1]
    return pow((portfolioValue / OptimalRiskyPortfolioTest.STARTING_CAPITAL), 1.0 / time) - 1.0;
  }

  @Test
  @Disabled
  public void backtestWithLocalMarketData() {
    final var marketDataRepository = new LocalMarketDataRepository(
      extractSymbols("symbols.txt"),
      TRADE_DATE,
      ZonedDateTime.of(LocalDate.of(2015, 12, 1), LocalTime.MIDNIGHT, ZoneId.of("America/New_York")).toInstant(),
      ZonedDateTime.of(LocalDate.of(2022, 9, 1), LocalTime.MIDNIGHT, ZoneId.of("America/New_York")).toInstant()
    );
    final var perf = runBacktest(marketDataRepository);
  }

  @Test
  @Disabled
  public void backtestWithNasdaq100RemoteMarketData() {
    final var marketDataRepository = new RemoteMarketDataRepository(
      extractSymbols("symbols.txt"),
      TRADE_DATE,
      ZonedDateTime.of(LocalDate.of(2015, 12, 1), LocalTime.MIDNIGHT, ZoneId.of("America/New_York")).toInstant(),
      ZonedDateTime.of(LocalDate.of(2022, 9, 1), LocalTime.MIDNIGHT, ZoneId.of("America/New_York")).toInstant()
    );
    final var perf = runBacktest(marketDataRepository);
  }

  @Test
  @Disabled
  public void backtestWithNasdaq100RemoteMarketDataAndCache() {
    final var marketDataRepository = new CacheRemoteMarketDataRepository(
      extractSymbols("symbols.txt"),
      TRADE_DATE,
      ZonedDateTime.of(LocalDate.of(2015, 12, 1), LocalTime.MIDNIGHT, ZoneId.of("America/New_York")).toInstant(),
      ZonedDateTime.of(LocalDate.of(2022, 9, 1), LocalTime.MIDNIGHT, ZoneId.of("America/New_York")).toInstant()
    );
    final var perf = runBacktest(marketDataRepository);
  }

  private double runBacktest(
    MarketDataRepository marketDataRepository
  ) {
    final var es = new EquityScreener(marketDataRepository);
    Double portfolioValue = STARTING_CAPITAL;
    int tradedDays = 0;
    long totalDays = 0;

    for (LocalDate i = TRADE_DATE; i.isBefore(TRADE_DATE.plusDays(1200)); i = i.plusDays(1)) {
      final LocalDate day = i;
      final var selection = es.screenEquities();
      final var stockReturns = marketDataRepository.getNewStockReturns(selection);
      final var indexReturns = marketDataRepository.getNewIndexReturns();

      final var missing = stockReturns.values().stream().filter(returns -> !returns.containsKey(day)).count();
      try {
        if (missing > 0 || !indexReturns.containsKey(day)) {
          throw new RuntimeException("Missing or extraneous data points");
        }
      } catch (RuntimeException e) {
        logger.warn(e.getMessage() + ", stocks: {} index: {}", missing, indexReturns.containsKey(day));
        continue;
      }

      final var orp = new OptimalRiskyPortfolio(marketDataRepository, selection);
      var allocation = orp.calculate();
      allocation = allocation.entrySet().stream()
        .collect(Collectors.toMap(
          Map.Entry::getKey,
          entry -> {
            if (entry.getKey().equals(MarketDataRepository.INDEX_NAME)) {
              return entry.getValue() * indexReturns.get(day);
            } else {
              return entry.getValue() * stockReturns.get(entry.getKey()).get(day);
            }
          }
        ));
      final var oldPortfolioValue = portfolioValue;
      portfolioValue = oldPortfolioValue + allocation.values().stream().mapToDouble(d -> d * oldPortfolioValue).sum();
      totalDays = ChronoUnit.DAYS.between(TRADE_DATE, day);

      System.out.println(allocation.entrySet());
      System.out.printf(
        "Date: %s(%s/%s), Value: %s, Benefit: %.2f$ (%.2f$), Rate (d): %.3f%n%n",
        day,
        ++tradedDays,
        totalDays,
        portfolioValue,
        portfolioValue - STARTING_CAPITAL,
        portfolioValue - oldPortfolioValue,
        (portfolioValue / oldPortfolioValue) - 1.0
      );

      marketDataRepository.increment();
    }
    return computePerformance(portfolioValue, totalDays / 365.0);
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