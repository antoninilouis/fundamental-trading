package com.el.marketdata;

import com.el.Application;
import org.apache.commons.math3.stat.regression.SimpleRegression;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public abstract class MarketDataRepository {

    private LocalDate tradeDate;

    private final LinkedHashMap<LocalDate, Double> indexPrices;
    private final LinkedHashMap<LocalDate, Double> indexReturns;
    private final LinkedHashMap<LocalDate, Double> tbReturns;
    private final Set<String> symbols;
    private final Map<String, LinkedHashMap<LocalDate, Double>> stockPrices;
    private final Map<String, RegressionResults> stockRegressionResults = new HashMap<>();
    private final Map<String, LinkedHashMap<LocalDate, Double>> stockReturns = new HashMap<>();
    private final Map<String, LinkedHashMap<LocalDate, Double>> stockDividends = new HashMap<>();
    private final Map<String, Double> stockReturnOnEquity = new HashMap<>();
    private final Map<String, Double> stockDividendPayoutRatio = new HashMap<>();

    public final static String INDEX_NAME = "GSPC";

    public MarketDataRepository(final LocalDate tradeDate) {
        this.tradeDate = tradeDate;
        final var allSymbols = extractSymbols();
        this.stockPrices = getStockPrices(allSymbols);
        this.symbols = allSymbols.stream().filter(s -> getPastStockPrices(s).size() >= 750).collect(Collectors.toSet());
        this.indexPrices = extractDatedValues(INDEX_NAME, ResourceTypes.PRICES);
        this.indexReturns = toReturnPercents(indexPrices);
        this.tbReturns = extractTBillsReturns();

        if (indexPrices.size() < 750) {
            throw new RuntimeException("Missing index data");
        }

        if (getPastTbReturns().size() < 1) {
            throw new RuntimeException("No T-bill returns");
        }

        this.symbols.forEach(symbol -> {
            this.stockReturns.put(symbol, toReturnPercents(this.stockPrices.get(symbol)));
            this.stockDividends.put(symbol, extractDatedValues(symbol, ResourceTypes.DIVIDENDS));
            try {
                this.stockReturnOnEquity.put(symbol, extractSingleValue(symbol, ResourceTypes.ROES));
                this.stockDividendPayoutRatio.put(symbol, extractSingleValue(symbol, ResourceTypes.PAYOUT_RATIOS));
                this.computeStockRegressionResult(symbol);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    // Compute

    abstract Map<String, LinkedHashMap<LocalDate, Double>> getStockPrices(Set<String> symbols);

    static LinkedHashMap<LocalDate, Double> toReturnPercents(final Map<LocalDate, Double> prices) {
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

    private static LinkedHashMap<LocalDate, Double> getCopy(Map<LocalDate, Double> prices) {
        return prices.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (o1, o2) -> o1,
                LinkedHashMap::new
            ));
    }

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

    protected static Double extractSingleValue(String symbol, final ResourceTypes type) throws IOException {
        final var inputStreamReader = new InputStreamReader(getFileFromResourceAsStream(type.getPath() + symbol));
        try (BufferedReader reader = new BufferedReader(inputStreamReader)) {
            final var line = reader.readLine();
            // todo: verify where NaN single values will be used
            return line != null ? Double.parseDouble(line) : Double.NaN;
        }
    }

    protected static LinkedHashMap<LocalDate, Double> extractDatedValues(final String symbol, final ResourceTypes type) {
        return byBufferedReader(
            type.getPath() + symbol + ".csv",
            DupKeyOption.OVERWRITE
        );
    }

    protected static LinkedHashMap<LocalDate, Double> extractTBillsReturns() {
        return byBufferedReader(
            "daily-treasury-rates.csv",
            DupKeyOption.OVERWRITE
        );
    }

    private static LinkedHashMap<LocalDate, Double> byBufferedReader(String filePath, DupKeyOption dupKeyOption) {
        LinkedHashMap<LocalDate, Double> map = new LinkedHashMap<>();
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

    public Set<String> getSymbols() {
        return symbols;
    }

    public LinkedHashMap<LocalDate, Double> getIndexPrices() {
        return indexPrices;
    }

    public LinkedHashMap<LocalDate, Double> getPastStockPrices(String symbol) {
        return stockPrices.get(symbol).entrySet().stream()
            .filter(e -> e.getKey().isBefore(tradeDate))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (o1, o2) -> o1, LinkedHashMap::new));
    }

    public LinkedHashMap<LocalDate, Double> getPastStockDividends(String symbol) {
        final var stockDividends = this.stockDividends.get(symbol).entrySet().stream()
            .filter(e -> e.getKey().isBefore(tradeDate))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (o1, o2) -> o1, LinkedHashMap::new));
        if (stockDividends.isEmpty()) {
            stockDividends.put(tradeDate.minusDays(1), 0.0);
        }
        return stockDividends;
    }

    public LinkedHashMap<LocalDate, Double> getPastTbReturns() {
        return tbReturns.entrySet().stream()
            .filter(e -> e.getKey().isBefore(tradeDate))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (o1, o2) -> o1, LinkedHashMap::new));
    }

    public Double getStockReturnOnEquity(String symbol) {
        return stockReturnOnEquity.get(symbol);
    }

    public Double getStockDividendPayoutRatio(String symbol) {
        return stockDividendPayoutRatio.get(symbol);
    }

    public LinkedHashMap<LocalDate, Double> getPastIndexReturns() {
        return indexReturns.entrySet().stream()
            .filter(e -> e.getKey().isBefore(tradeDate))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (o1, o2) -> o1, LinkedHashMap::new));
    }

    public LinkedHashMap<LocalDate, Double> getNewIndexReturns() {
        return indexReturns.entrySet().stream()
            .filter(e -> e.getKey().isAfter(tradeDate.minusDays(1)))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (o1, o2) -> o1, LinkedHashMap::new));
    }

    public LinkedHashMap<LocalDate, Double> getPastStockReturns(String symbol) {
        return stockReturns.get(symbol).entrySet().stream()
            .filter(e -> e.getKey().isBefore(tradeDate))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (o1, o2) -> o1, LinkedHashMap::new));
    }

    public LinkedHashMap<LocalDate, Double> getNewStockReturns(String symbol) {
        return stockReturns.get(symbol).entrySet().stream()
            .filter(e -> e.getKey().isAfter(tradeDate.minusDays(1)))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (o1, o2) -> o1, LinkedHashMap::new));
    }

    public Map<String, LinkedHashMap<LocalDate, Double>> getNewStockReturns(Set<String> symbols) {
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
}
