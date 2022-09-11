package com.el;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Algorithm to trade equities of an optimal risky portfolio based on the CAPM... every minute.
 *
 */
public class Application
{
    public static void main( String[] args )
    {
        final var indexPrices = extractDatedValues("^GSPC", ResourceTypes.PRICES);
        final var stockPrices = extractDatedValues("MSFT", ResourceTypes.PRICES);
        final var stockDividends = extractDatedValues("MSFT", ResourceTypes.DIVIDENDS);
        final var tbReturns = extractTBillsReturns();

        CAPM.compute(indexPrices, stockPrices, tbReturns, LocalDate.of(2022,5, 31));

        final Double growthRate;
        try {
            final var returnOnEquity = extractSingleValue("payoutRatios/MSFT");
            final var dividendPayoutRatio = extractSingleValue("ROEs/MSFT");
            growthRate = computeGrowthRate(returnOnEquity, dividendPayoutRatio);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        final var forecastedPrice = computeForecastedPrice(stockPrices, growthRate);
        final var forecastedDividends = computeForecastedDividends(stockDividends, growthRate);
    }

    private static Double computeForecastedDividends(Map<LocalDate, Double> stockDividends, Double growthRate) {
        var optLatestDate = stockDividends.keySet().stream().sorted()
                .filter(localDate -> localDate.isBefore(LocalDate.of(2022, 5, 31).plusDays(1)))
                .max(LocalDate::compareTo);
        if (optLatestDate.isEmpty()) {
            return 0.0;
        }
        return stockDividends.get(optLatestDate.get()) * (1 + growthRate);
    }

    private static Double computeForecastedPrice(Map<LocalDate, Double> stockPrices, Double growthRate) {
        var optLatestDate = stockPrices.keySet().stream().sorted()
                .filter(localDate -> localDate.isBefore(LocalDate.of(2022, 5, 31).plusDays(1)))
                .max(LocalDate::compareTo);
        if (optLatestDate.isEmpty()) {
            return 0.0;
        }
        return stockPrices.get(optLatestDate.get()) * (1 + growthRate);
    }

    private static Double computeGrowthRate(Double returnOnEquity, Double dividendPayoutRatio) {
        return returnOnEquity * (1.0 - dividendPayoutRatio);
    }

    // Input extraction

    static Double extractSingleValue(final String path) throws IOException {
        String line;
        final var inputStreamReader = new InputStreamReader(getFileFromResourceAsStream(path));
        try (BufferedReader reader = new BufferedReader(inputStreamReader)) {
            return Double.valueOf(reader.readLine());
        }
    }

    static Map<LocalDate, Double> extractDatedValues(final String symbol, final ResourceTypes type) {
        return byBufferedReader(
            type.getPath() + symbol + ".csv",
            DupKeyOption.OVERWRITE
        );
    }

    static Map<LocalDate, Double> extractTBillsReturns() {
        return byBufferedReader(
            "daily-treasury-rates.csv",
            DupKeyOption.OVERWRITE
        );
    }

    // Utils

    private static Map<LocalDate, Double> byBufferedReader(String filePath, DupKeyOption dupKeyOption) {
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

    enum DupKeyOption {
        OVERWRITE, DISCARD
    }

    enum ResourceTypes {
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
