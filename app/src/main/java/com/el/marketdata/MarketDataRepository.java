package com.el.marketdata;

import com.el.Application;
import org.apache.commons.math3.stat.regression.SimpleRegression;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public abstract class MarketDataRepository {

  public static final String INDEX_NAME = "GSPC";
  private static final int MIN_DATA_POINTS = 750;
  private final TreeMap<LocalDate, Double> indexPrices;
  private final TreeMap<LocalDate, Double> indexReturns;
  private final TreeMap<LocalDate, Double> tbReturns;
  private final Set<String> symbols;
  private final Map<String, TreeMap<LocalDate, Double>> stockPrices;
  private final Map<String, RegressionResults> stockRegressionResults = new HashMap<>();
  private final Map<String, TreeMap<LocalDate, Double>> stockReturns = new HashMap<>();
  private final Map<String, TreeMap<LocalDate, Double>> stockDividends;
  private final Map<String, Double> stockReturnOnEquity = new HashMap<>();
  private final Map<String, Double> stockDividendPayoutRatio = new HashMap<>();
  private LocalDate tradeDate;

  public MarketDataRepository(final LocalDate tradeDate, final Instant from, final Instant to) {
    if (from.isAfter(to)) {
      throw new IllegalArgumentException("Start date is after end date");
    }

    this.tradeDate = tradeDate;
    final var allSymbols = extractSymbols();
    this.stockPrices = getStockPrices(allSymbols, from, to);
    this.stockDividends = getStockDividends(allSymbols, from, to);
    this.symbols = allSymbols.stream().filter(s -> getPastStockPrices(s).size() >= MIN_DATA_POINTS).collect(Collectors.toSet());
    this.indexPrices = getIndexPrices(from, to);
    this.indexReturns = toReturnPercents(indexPrices);
    this.tbReturns = getTbReturns(from, to);

    if (indexPrices.size() < MIN_DATA_POINTS) {
      throw new RuntimeException("Missing index data");
    }

    if (getPastTbReturns().size() < 1) {
      throw new RuntimeException("No T-bill returns");
    }

    this.symbols.forEach(symbol -> {
      this.stockReturns.put(symbol, toReturnPercents(this.stockPrices.get(symbol)));
      try {
        this.stockReturnOnEquity.put(symbol, extractSingleValue(symbol, ResourceTypes.ROES));
        this.stockDividendPayoutRatio.put(symbol, extractSingleValue(symbol, ResourceTypes.PAYOUT_RATIOS));
        this.computeStockRegressionResult(symbol);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });
  }

  static TreeMap<LocalDate, Double> toReturnPercents(final Map<LocalDate, Double> prices) {
    final var copy = getCopy(prices);
    final var iterator = copy.entrySet().iterator();
    final var firstEntry = iterator.next();
    var previousValue = firstEntry.getValue();

    firstEntry.setValue(0.0);
    while (iterator.hasNext()) {
      var entry = iterator.next();
      var tmp = entry.getValue();
      entry.setValue(entry.getValue() / previousValue - 1);
      previousValue = tmp;
    }
    return copy;
  }

  private static TreeMap<LocalDate, Double> getCopy(Map<LocalDate, Double> prices) {
    return prices.entrySet().stream()
      .collect(Collectors.toMap(
        Map.Entry::getKey,
        Map.Entry::getValue,
        (o1, o2) -> o1,
        TreeMap::new
      ));
  }

  protected static Double extractSingleValue(String symbol, final ResourceTypes type) throws IOException {
    final var inputStreamReader = new InputStreamReader(getFileFromResourceAsStream(type.getPath() + symbol));
    try (BufferedReader reader = new BufferedReader(inputStreamReader)) {
      final var line = reader.readLine();
      // todo: verify where NaN single values will be used
      return line != null ? Double.parseDouble(line) : Double.NaN;
    }
  }

  // Compute

  private static InputStream getFileFromResourceAsStream(String fileName) {
    ClassLoader classLoader = Application.class.getClassLoader();
    InputStream inputStream = classLoader.getResourceAsStream(fileName);

    if (inputStream == null) {
      throw new IllegalArgumentException("file not found! " + fileName);
    } else {
      return inputStream;
    }
  }

  protected abstract Map<String, TreeMap<LocalDate, Double>> getStockPrices(Set<String> symbols, Instant from, Instant to);

  protected abstract TreeMap<LocalDate, Double> getIndexPrices(Instant from, Instant to);

  protected abstract TreeMap<LocalDate, Double> getTbReturns(Instant from, Instant to);

  protected abstract Map<String, TreeMap<LocalDate, Double>> getStockDividends(Set<String> symbols, Instant from, Instant to);

  // Read

  private Set<String> extractSymbols() {
    final Set<String> symbols = new HashSet<>();
    final var inputStreamReader = new InputStreamReader(getFileFromResourceAsStream("symbols.txt"));
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

  public Set<String> getSymbols() {
    return symbols;
  }

  public TreeMap<LocalDate, Double> getIndexPrices() {
    return indexPrices;
  }

  public TreeMap<LocalDate, Double> getPastStockPrices(String symbol) {
    return stockPrices.get(symbol).entrySet().stream()
      .filter(e -> e.getKey().isBefore(tradeDate))
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (o1, o2) -> o1, TreeMap::new));
  }

  private TreeMap<LocalDate, Double> getPastStockDividends(String symbol) {
    final var stockDividends = this.stockDividends.get(symbol).entrySet().stream()
      .filter(e -> e.getKey().isBefore(tradeDate))
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (o1, o2) -> o1, TreeMap::new));
    if (stockDividends.isEmpty()) {
      stockDividends.put(tradeDate.minusDays(1), 0.0);
    }
    return stockDividends;
  }

  public double getLatestDividend(String symbol) {
    final var optLatestDividend = getPastStockDividends(symbol).entrySet().stream()
      .max(Map.Entry.comparingByKey());
    var latestDividend = 0.0;
    if (optLatestDividend.isPresent()) {
      latestDividend = optLatestDividend.get().getValue();
    }
    return latestDividend;
  }

  public TreeMap<LocalDate, Double> getPastTbReturns() {
    return tbReturns.entrySet().stream()
      .filter(e -> e.getKey().isBefore(tradeDate))
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (o1, o2) -> o1, TreeMap::new));
  }

  public Double getStockReturnOnEquity(String symbol) {
    return stockReturnOnEquity.get(symbol);
  }

  public Double getStockDividendPayoutRatio(String symbol) {
    return stockDividendPayoutRatio.get(symbol);
  }

  public TreeMap<LocalDate, Double> getPastIndexReturns() {
    return indexReturns.entrySet().stream()
      .filter(e -> e.getKey().isBefore(tradeDate))
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (o1, o2) -> o1, TreeMap::new));
  }

  public TreeMap<LocalDate, Double> getNewIndexReturns() {
    return indexReturns.entrySet().stream()
      .filter(e -> e.getKey().isAfter(tradeDate.minusDays(1)))
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (o1, o2) -> o1, TreeMap::new));
  }

  public TreeMap<LocalDate, Double> getPastStockReturns(String symbol) {
    return stockReturns.get(symbol).entrySet().stream()
      .filter(e -> e.getKey().isBefore(tradeDate))
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (o1, o2) -> o1, TreeMap::new));
  }

  public TreeMap<LocalDate, Double> getNewStockReturns(String symbol) {
    return stockReturns.get(symbol).entrySet().stream()
      .filter(e -> e.getKey().isAfter(tradeDate.minusDays(1)))
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (o1, o2) -> o1, TreeMap::new));
  }

  public Map<String, TreeMap<LocalDate, Double>> getNewStockReturns(Set<String> symbols) {
    return stockReturns.entrySet().stream()
      .filter(entry -> symbols.contains(entry.getKey()))
      .collect(Collectors.toMap(Map.Entry::getKey, e -> getNewStockReturns(e.getKey())));
  }

  public void computeStockRegressionResult(String symbol) {
    final var reg = new SimpleRegression();
    final var indexReturns = getPastIndexReturns();
    final var stockReturns = getPastStockReturns(symbol);
    for (var entry : stockReturns.entrySet()) {
      if (!indexReturns.containsKey(entry.getKey())) {
        throw new RuntimeException("Missing or extraneous datapoints");
      }
      reg.addData(entry.getValue(), indexReturns.get(entry.getKey()));
    }
    stockRegressionResults.put(symbol, new RegressionResults(
      reg.getSlope(),
      reg.getIntercept(),
      reg.getSumSquaredErrors(),
      reg.getMeanSquareError()
    ));
  }

  public RegressionResults getStockRegressionResults(String symbol) {
    if (!stockRegressionResults.containsKey(symbol)) {
      throw new RuntimeException("No regression results for " + symbol);
    }
    return stockRegressionResults.get(symbol);
  }

  public Map<String, RegressionResults> getStockRegressionResults(Set<String> symbols) {
    return symbols.stream().collect(Collectors.toMap(symbol -> symbol, this::getStockRegressionResults));
  }

  public void increment() {
    this.tradeDate = tradeDate.plusDays(1);
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
