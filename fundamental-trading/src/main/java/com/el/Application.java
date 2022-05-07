package com.el;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
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

    public static Map<String, String> extractSPReturns() {
        // Get prices
        return byBufferedReader(
                "SP500 prices (daily)",
                DupKeyOption.OVERWRITE
        );
        // todo: Get returns
    }

    public static void extractStockReturns() {
        // Get prices
        // todo: Get returns
    }

    public static void calculateStockBeta() {
        // todo: do linear regression on stock/s&p returns
        //  /!\ caveats only include days which have returns in both SP500 & stock in the regression
    }

    public static void calculateExpectedReturnsOnMarket() {
        // todo: geometric average on the period of evaluation
    }

    public static Map<String, String> extractTBillsReturns() {
        return byBufferedReader(
                "t-bills returns (daily)",
                DupKeyOption.OVERWRITE
        );
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

    public static Map<String, String> byBufferedReader(String filePath, DupKeyOption dupKeyOption) {
        HashMap<String, String> map = new HashMap<>();
        String line;
        final var inputStreamReader = new InputStreamReader(getFileFromResourceAsStream(filePath));
        try (BufferedReader reader = new BufferedReader(inputStreamReader)) {
            while ((line = reader.readLine()) != null) {
                String[] keyValuePair = line.split("\t", 2);
                if (keyValuePair.length > 1) {
                    String key = keyValuePair[0];
                    String value = keyValuePair[1];
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
