package com.el;

import org.apache.commons.math3.stat.regression.SimpleRegression;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class SymbolStatisticsRepository {

    private final List<String> symbols;
    private final LinkedHashMap<LocalDate, Double> indexPrices;
    private final LinkedHashMap<LocalDate, Double> indexReturns;
    private final LinkedHashMap<LocalDate, Double> tbReturns;
    private final Map<String, Double> stockReturnOnEquity;
    private final Map<String, Double> stockDividendPayoutRatio;
    private final Map<String, RegressionResults> stockRegressionResults;
    private final Map<String, LinkedHashMap<LocalDate, Double>> stockPrices;
    private final Map<String, LinkedHashMap<LocalDate, Double>> stockReturns;
    private final Map<String, LinkedHashMap<LocalDate, Double>> stockDividends;

    public final static String INDEX_NAME = "^GSPC";

    public SymbolStatisticsRepository() {
        this.symbols = extractSymbols();
        this.indexPrices = extractDatedValues(INDEX_NAME, ResourceTypes.PRICES);
        this.indexReturns = toReturnPercents(indexPrices);
        this.tbReturns = extractTBillsReturns();
        this.stockReturnOnEquity = new HashMap<>();
        this.stockDividendPayoutRatio = new HashMap<>();
        this.stockRegressionResults = new HashMap<>();
        this.stockPrices = new HashMap<>();
        this.stockReturns = new HashMap<>();
        this.stockDividends = new HashMap<>();

        symbols.forEach(symbol -> {
            this.stockPrices.put(symbol, extractDatedValues(symbol, ResourceTypes.PRICES));
            this.stockReturns.put(symbol, toReturnPercents(stockPrices.get(symbol)));
            this.stockDividends.put(symbol, extractDatedValues(symbol, ResourceTypes.DIVIDENDS));
            try {
                this.stockReturnOnEquity.put(symbol, extractSingleValue("ROEs/" + symbol));
                this.stockDividendPayoutRatio.put(symbol, extractSingleValue("payoutRatios/" + symbol));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            final var reg = new SimpleRegression();
            for (var entry : this.stockReturns.get(symbol).entrySet()) {
                if (!this.indexReturns.containsKey(entry.getKey())) {
                    continue;
                }
                reg.addData(entry.getValue(), this.indexReturns.get(entry.getKey()));
            }
            this.stockRegressionResults.put(symbol, new RegressionResults(
                reg.getSlope(),
                reg.getIntercept(),
                reg.getSumSquaredErrors(),
                reg.getMeanSquareError()
            ));
        });

    }

    // Compute

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
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (o1, o2) -> o1, LinkedHashMap::new));
    }

    // Read

    private List<String> extractSymbols() {
        final List<String> symbols = new ArrayList<>();
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

    private static Double extractSingleValue(final String path) throws IOException {
        final var inputStreamReader = new InputStreamReader(getFileFromResourceAsStream(path));
        try (BufferedReader reader = new BufferedReader(inputStreamReader)) {
            final var line = reader.readLine();
            // todo: verify where NaN single values will be used
            return line != null ? Double.parseDouble(line) : Double.NaN;
        }
    }

    private static LinkedHashMap<LocalDate, Double> extractDatedValues(final String symbol, final ResourceTypes type) {
        return byBufferedReader(
            type.getPath() + symbol + ".csv",
            DupKeyOption.OVERWRITE
        );
    }

    private static LinkedHashMap<LocalDate, Double> extractTBillsReturns() {
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

    private enum ResourceTypes {
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

    public List<String> getSymbols() {
        return symbols;
    }

    public LinkedHashMap<LocalDate, Double> getIndexPrices() {
        return indexPrices;
    }

    public LinkedHashMap<LocalDate, Double> getStockPrices(String symbol) {
        return stockPrices.get(symbol);
    }

    public LinkedHashMap<LocalDate, Double> getStockDividends(String symbol) {
        return stockDividends.get(symbol);
    }

    public LinkedHashMap<LocalDate, Double> getTbReturns() {
        return tbReturns;
    }

    public Double getStockReturnOnEquity(String symbol) {
        return stockReturnOnEquity.get(symbol);
    }

    public Double getStockDividendPayoutRatio(String symbol) {
        return stockDividendPayoutRatio.get(symbol);
    }

    public LinkedHashMap<LocalDate, Double> getIndexReturns() {
        return indexReturns;
    }

    public LinkedHashMap<LocalDate, Double> getStockReturns(String symbol) {
        return stockReturns.get(symbol);
    }

    public Map<String, LinkedHashMap<LocalDate, Double>> getStockReturns(Set<String> symbols) {
        return stockReturns.entrySet().stream()
                .filter(entry -> symbols.contains(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public RegressionResults getStockRegressionResults(String symbol) {
        return stockRegressionResults.get(symbol);
    }

    public Map<String, RegressionResults> getStockRegressionResults(Set<String> symbols) {
        return stockRegressionResults.entrySet().stream()
                .filter(entry -> symbols.contains(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
