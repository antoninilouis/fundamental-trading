package com.el;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.util.*;

public class SymbolStatisticsRepository {

    private final Collection<String> symbols;
    private final LinkedHashMap<LocalDate, Double> indexPrices;
    private final LinkedHashMap<LocalDate, Double> stockPrices;
    private final LinkedHashMap<LocalDate, Double> stockDividends;
    private final Map<LocalDate, Double> tbReturns;
    private final double returnOnEquity;
    private final double dividendPayoutRatio;

    public SymbolStatisticsRepository() {
        this.symbols = extractSymbols();
        this.indexPrices = extractDatedValues("^GSPC", ResourceTypes.PRICES);
        this.stockPrices = extractDatedValues("MSFT", ResourceTypes.PRICES);
        this.stockDividends = extractDatedValues("MSFT", ResourceTypes.DIVIDENDS);
        this.tbReturns = extractTBillsReturns();
        try {
            this.returnOnEquity = extractSingleValue("ROEs/" + "MSFT");
            this.dividendPayoutRatio = extractSingleValue("payoutRatios/" + "MSFT");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Collection<String> extractSymbols() {
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

    public static Double extractSingleValue(final String path) throws IOException {
        final var inputStreamReader = new InputStreamReader(getFileFromResourceAsStream(path));
        try (BufferedReader reader = new BufferedReader(inputStreamReader)) {
            return Double.valueOf(reader.readLine());
        }
    }

    public static LinkedHashMap<LocalDate, Double> extractDatedValues(final String symbol, final ResourceTypes type) {
        return byBufferedReader(
            type.getPath() + symbol + ".csv",
            DupKeyOption.OVERWRITE
        );
    }

    public static Map<LocalDate, Double> extractTBillsReturns() {
        return byBufferedReader(
            "daily-treasury-rates.csv",
            DupKeyOption.OVERWRITE
        );
    }

    public static LinkedHashMap<LocalDate, Double> byBufferedReader(String filePath, DupKeyOption dupKeyOption) {
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

    public static InputStream getFileFromResourceAsStream(String fileName) {
        ClassLoader classLoader = Application.class.getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream(fileName);

        if (inputStream == null) {
            throw new IllegalArgumentException("file not found! " + fileName);
        } else {
            return inputStream;
        }
    }

    public enum DupKeyOption {
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

    public Collection<String> getSymbols() {
        return symbols;
    }

    public LinkedHashMap<LocalDate, Double> getIndexPrices() {
        return indexPrices;
    }

    public LinkedHashMap<LocalDate, Double> getStockPrices() {
        return stockPrices;
    }

    public LinkedHashMap<LocalDate, Double> getStockDividends() {
        return stockDividends;
    }

    public Map<LocalDate, Double> getTbReturns() {
        return tbReturns;
    }

    public double getReturnOnEquity() {
        return returnOnEquity;
    }

    public double getDividendPayoutRatio() {
        return dividendPayoutRatio;
    }
}
