package com.el;

import org.apache.commons.math3.stat.regression.SimpleRegression;

import java.io.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Algorithm to trade equities of an optimal risky portfolio based on the CAPM... every minute.
 *
 */
public class Application
{
    /**
     * - Logic
     * - Integration (to IBKR)
     */
    public static void main( String[] args )
    {
    }

    static double computeCAPM() {
        return 0.0;
    }

    static Map<LocalDate, Double> extractSPReturns() {
        // Get prices
        final Map<LocalDate, Double> prices = byBufferedReader(
                "SP500 prices (daily)",
                DupKeyOption.OVERWRITE
        );
        toReturnPercents(prices);
        return prices;
    }

    static Map<LocalDate, Double> extractStockReturns() {
        // Get prices
        final Map<LocalDate, Double> prices = byBufferedReader(
            "BRK-B prices (daily)",
            DupKeyOption.OVERWRITE
        );
        toReturnPercents(prices);
        return prices;
    }

    static Map<LocalDate, Double> extractTBillsReturns() {
        return byBufferedReader(
            "t-bills returns (daily)",
            DupKeyOption.OVERWRITE
        );
    }

    static void toReturnPercents(final Map<LocalDate, Double> prices) {
        final var iterator = prices.entrySet().iterator();
        if (!iterator.hasNext()) {
            return;
        }
        var firstEntry = iterator.next();
        var previousValue = firstEntry.getValue();
        firstEntry.setValue(0.0);
        while (iterator.hasNext()) {
            var entry = iterator.next();
            var tmp = entry.getValue();
            entry.setValue(entry.getValue() / previousValue - 1);
            previousValue = tmp;
        }
    }

    static double calculateStockBeta(
        final Map<LocalDate, Double> stockReturns,
        final Map<LocalDate, Double> marketReturns
    ) {
        final var reg = new SimpleRegression();
        for (var entry : stockReturns.entrySet()) {
            if (!marketReturns.containsKey(entry.getKey())) {
                continue;
            }
            // SimpleRegression.addData
            reg.addData(entry.getValue(), marketReturns.get(entry.getKey()));
        }
        return reg.getSlope();
    }

    static void calculateExpectedReturnsOnMarket() {
        // todo: geometric average on the period of evaluation
    }

    // Utils

    public static List<String> byBufferedReader(String filePath) {
        List<String> list = new ArrayList<>();
        String line;
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            while ((line = reader.readLine()) != null) {
                list.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return list;
    }

    private static Map<LocalDate, Double> byBufferedReader(String filePath, DupKeyOption dupKeyOption) {
        LinkedHashMap<LocalDate, Double> map = new LinkedHashMap<>();
        String line;
        final var inputStreamReader = new InputStreamReader(getFileFromResourceAsStream(filePath));
        try (BufferedReader reader = new BufferedReader(inputStreamReader)) {
            while ((line = reader.readLine()) != null) {
                String[] keyValuePair = line.split("\t", 2);
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
}
