package com.el;

import java.io.*;
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

    public static double computeCAPM() {
        return 0.0;
    }

    public static Map<String, Double> extractSPReturns() {
        // Get prices
        return byBufferedReader(
            "SP500 prices (daily)",
            DupKeyOption.OVERWRITE
        );
        // todo: Get returns
    }

    public static Map<String, Double> extractStockReturns() {
        // Get prices
        return byBufferedReader(
            "BRK-B prices (daily)",
            DupKeyOption.OVERWRITE
        );
    }

    public static Map<String, Double> extractTBillsReturns() {
        return byBufferedReader(
                "t-bills returns (daily)",
                DupKeyOption.OVERWRITE
        );
    }

    public static void toReturnPercents(final Map<String, Double> prices) {
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

    public static void calculateStockBeta() {
        // todo: do linear regression on stock/s&p returns
        //  /!\ caveats only include days which have returns in both SP500 & stock in the regression
    }

    public static void calculateExpectedReturnsOnMarket() {
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

    public static Map<String, Double> byBufferedReader(String filePath, DupKeyOption dupKeyOption) {
        LinkedHashMap<String, Double> map = new LinkedHashMap<>();
        String line;
        final var inputStreamReader = new InputStreamReader(getFileFromResourceAsStream(filePath));
        try (BufferedReader reader = new BufferedReader(inputStreamReader)) {
            while ((line = reader.readLine()) != null) {
                String[] keyValuePair = line.split("\t", 2);
                if (keyValuePair.length > 1) {
                    String key = keyValuePair[0];
                    String value = keyValuePair[1];
                    var converted = Double.valueOf(value);
                    if (converted.isNaN()) {
                        continue;
                    }
                    if (DupKeyOption.OVERWRITE == dupKeyOption) {
                        map.put(key, converted);
                    } else if (DupKeyOption.DISCARD == dupKeyOption) {
                        map.putIfAbsent(key, converted);
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
