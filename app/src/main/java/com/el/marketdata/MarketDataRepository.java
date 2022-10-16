package com.el.marketdata;

import org.apache.commons.math3.stat.regression.SimpleRegression;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public abstract class MarketDataRepository {

  public static final String INDEX_NAME = "GSPC";
  private static final int MIN_DATA_POINTS = 750;

  private final Map<String, RegressionResults> stockRegressionResults = new HashMap<>();

  private LocalDate tradeDate;
  private Set<String> symbols;
  private TreeMap<LocalDate, Double> indexPrices;
  private TreeMap<LocalDate, Double> indexReturns;
  private TreeMap<LocalDate, Double> tbReturns;
  private Map<String, TreeMap<LocalDate, Double>> stockPrices;
  private Map<String, TreeMap<LocalDate, Double>> stockReturns;
  private Map<String, TreeMap<LocalDate, Double>> stockDividends;
  private Map<String, TreeMap<LocalDate, Double>> stockReturnOnEquity;
  private Map<String, TreeMap<LocalDate, Double>> stockDividendPayoutRatio;

  public MarketDataRepository(final Set<String> symbols, final LocalDate tradeDate) {
    this.symbols = symbols;
    this.tradeDate = tradeDate;
  }

  public void initialize(final Instant from, final Instant to) {
    if (from.isAfter(to)) {
      throw new IllegalArgumentException("Start date is after end date");
    }

    this.stockPrices = getStockPrices(this.symbols, from, to);
    this.symbols = this.symbols.stream().filter(s -> getPastStockPrices(s).size() >= MIN_DATA_POINTS).collect(Collectors.toSet());

    this.stockReturns = getStockReturns(stockPrices);
    this.stockDividends = getStockDividends(this.symbols, from, to);
    this.indexPrices = getIndexPrices(from, to);
    this.indexReturns = toReturnPercents(indexPrices);
    this.tbReturns = getTbReturns(from, to);
    this.stockReturnOnEquity = getStockReturnOnEquity(this.symbols, from, to);
    this.stockDividendPayoutRatio = getStockDividendPayoutRatio(this.symbols, from, to);

    if (indexPrices.size() < MIN_DATA_POINTS) {
      throw new RuntimeException("Missing index data");
    }

    if (getPastTbReturns().isEmpty()) {
      throw new RuntimeException("No T-bill returns");
    }

    this.symbols.forEach(this::computeStockRegressionResult);
  }

  protected abstract Map<String, TreeMap<LocalDate, Double>> getStockPrices(Set<String> symbols, Instant from, Instant to);

  protected abstract TreeMap<LocalDate, Double> getIndexPrices(Instant from, Instant to);

  protected abstract TreeMap<LocalDate, Double> getTbReturns(Instant from, Instant to);

  protected abstract Map<String, TreeMap<LocalDate, Double>> getStockDividends(Set<String> symbols, Instant from, Instant to);

  protected abstract Map<String, TreeMap<LocalDate, Double>> getStockReturnOnEquity(Set<String> symbols, Instant from, Instant to);

  protected abstract Map<String, TreeMap<LocalDate, Double>> getStockDividendPayoutRatio(Set<String> symbols, Instant from, Instant to);

  private Map<String, TreeMap<LocalDate, Double>> getStockReturns(Map<String, TreeMap<LocalDate, Double>> stockPrices) {
    final var stockReturns = new HashMap<String, TreeMap<LocalDate, Double>>();
    this.symbols.forEach(symbol -> stockReturns.put(symbol, toReturnPercents(stockPrices.get(symbol))));
    return stockReturns;
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

  // Getters

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

  public double getLatestDividend(String symbol) {
    if (!stockDividends.containsKey(symbol)) {
      return 0.0;
    }
    var latest = this.stockDividends.get(symbol).floorEntry(tradeDate);
    if (latest == null) {
      return 0.0;
    }
    return latest.getValue();
  }

  public TreeMap<LocalDate, Double> getPastTbReturns() {
    return tbReturns.entrySet().stream()
      .filter(e -> e.getKey().isBefore(tradeDate))
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (o1, o2) -> o1, TreeMap::new));
  }

  public Double getLatestStockReturnOnEquity(String symbol) {
    if (!stockReturnOnEquity.containsKey(symbol)) {
      return 0.0;
    }
    var latest = stockReturnOnEquity.get(symbol).floorEntry(tradeDate);
    if (latest == null) {
      return 0.0;
    }
    return latest.getValue();
  }

  public Double getLatestStockDividendPayoutRatio(String symbol) {
    if (!stockDividendPayoutRatio.containsKey(symbol)) {
      return 0.0;
    }
    var latest = stockDividendPayoutRatio.get(symbol).floorEntry(tradeDate);
    if (latest == null) {
      return 0.0;
    }
    return latest.getValue();
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
}
