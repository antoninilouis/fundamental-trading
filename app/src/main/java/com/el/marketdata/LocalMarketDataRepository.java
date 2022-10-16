package com.el.marketdata;

import com.el.Application;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class LocalMarketDataRepository extends MarketDataRepository {

  public LocalMarketDataRepository(final Set<String> symbols, LocalDate tradeDate, final Instant from, final Instant to) {
    super(symbols, tradeDate);
    this.initialize(from, to);
  }

  @Override
  protected Map<String, TreeMap<LocalDate, Double>> getStockPrices(Set<String> symbols, Instant from, Instant to) {
    final Map<String, TreeMap<LocalDate, Double>> stockPrices = new HashMap<>();
    symbols.forEach(symbol -> stockPrices.put(symbol, extractDatedValues(symbol, ResourceTypes.PRICES, from, to)));
    return stockPrices;
  }

  @Override
  protected TreeMap<LocalDate, Double> getIndexPrices(Instant from, Instant to) {
    return extractDatedValues(INDEX_NAME, ResourceTypes.PRICES, from, to);
  }

  @Override
  protected TreeMap<LocalDate, Double> getTbReturns(Instant from, Instant to) {
    return byBufferedReader(
      "daily-treasury-rates.csv",
      DupKeyOption.OVERWRITE
    );
  }

  @Override
  protected Map<String, TreeMap<LocalDate, Double>> getStockDividends(Set<String> symbols, Instant from, Instant to) {
    final var stockDividends = new HashMap<String, TreeMap<LocalDate, Double>>();
    symbols.forEach(symbol -> {
       stockDividends.put(symbol, extractDatedValues(symbol, ResourceTypes.DIVIDENDS, from, to));
    });
    return stockDividends;
  }

  @Override
  protected Map<String, TreeMap<LocalDate, Double>> getStockReturnOnEquity(Set<String> symbols, Instant from, Instant to) {
    final var stockReturnOnEquity = new HashMap<String, TreeMap<LocalDate, Double>>();
    symbols.forEach(symbol -> {
      try {
        final var tm = new TreeMap<LocalDate, Double>();
        // Hack date of extracted single value to 'from', so value is always available in calculations
        tm.put(LocalDate.ofInstant(from, ZoneId.of("America/New_York")), extractSingleValue(symbol, ResourceTypes.ROES));
        stockReturnOnEquity.put(symbol, tm);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });
    return stockReturnOnEquity;
  }

  @Override
  protected Map<String, TreeMap<LocalDate, Double>> getStockDividendPayoutRatio(Set<String> symbols, Instant from, Instant to) {
    final var stockDividendPayoutRatio = new HashMap<String, TreeMap<LocalDate, Double>>();
    symbols.forEach(symbol -> {
      try {
        final var tm = new TreeMap<LocalDate, Double>();
        // Hack date of extracted single value to 'from', so value is always available in calculations
        tm.put(LocalDate.ofInstant(from, ZoneId.of("America/New_York")), extractSingleValue(symbol, ResourceTypes.ROES));
        stockDividendPayoutRatio.put(symbol, tm);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });
    return stockDividendPayoutRatio;
  }

  protected static Double extractSingleValue(String symbol, final ResourceTypes type) throws IOException {
    final var inputStreamReader = new InputStreamReader(getFileFromResourceAsStream(type.getPath() + symbol));
    try (BufferedReader reader = new BufferedReader(inputStreamReader)) {
      final var line = reader.readLine();
      // todo: verify where NaN single values will be used
      return line != null ? Double.parseDouble(line) : Double.NaN;
    }
  }

  protected static TreeMap<LocalDate, Double> extractDatedValues(final String symbol, final ResourceTypes type, Instant from, Instant to) {
    return byBufferedReader(type.getPath() + symbol + ".csv", DupKeyOption.OVERWRITE).entrySet().stream()
      .filter(e -> !e.getKey().isBefore(LocalDate.ofInstant(from, ZoneId.of("America/New_York")))
        || e.getKey().isAfter(LocalDate.ofInstant(to, ZoneId.of("America/New_York"))))
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (o1, o2) -> o1, TreeMap::new));
  }

  private static TreeMap<LocalDate, Double> byBufferedReader(String filePath, DupKeyOption dupKeyOption) {
    TreeMap<LocalDate, Double> map = new TreeMap<>();
    String line;
    final var inputStreamReader = new InputStreamReader(getFileFromResourceAsStream(filePath));
    try (BufferedReader reader = new BufferedReader(inputStreamReader)) {
      while ((line = reader.readLine()) != null) {
        String[] keyValuePair = line.split(",", 2);
        if (keyValuePair.length > 1) {
          var key = LocalDate.parse(keyValuePair[0]);
          var value = Double.valueOf(keyValuePair[1]);
          if (value.isNaN()) {
            continue;
          }
          if (DupKeyOption.OVERWRITE == dupKeyOption) {
            map.put(key, value);
          } else if (DupKeyOption.DISCARD == dupKeyOption) {
            map.putIfAbsent(key, value);
          }
        } else {
          System.out.println("No Key:Value found in line, ignoring: " + line);
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return map;
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

  private enum DupKeyOption {
    OVERWRITE, DISCARD
  }

  protected enum ResourceTypes {
    DIVIDENDS("dividends/"),
    PAYOUT_RATIOS("payoutRatios/"),
    PRICES("prices/"),
    ROES("ROEs/");

    private final String prefix;

    ResourceTypes(final String prefix) {
      this.prefix = prefix;
    }

    public String getPath() {
      return prefix;
    }
  }
}
