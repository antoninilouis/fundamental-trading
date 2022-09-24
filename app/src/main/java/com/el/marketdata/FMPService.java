package com.el.marketdata;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Properties;

public class FMPService {

    private final String apikey;

    FMPService() {
        try {
            final Properties appProps = new Properties();
            final String appConfigPath = Objects.requireNonNull(getClass().getClassLoader().getResource("fmp.properties")).getPath();

            appProps.load(new FileInputStream(appConfigPath));
            this.apikey = appProps.getProperty("apikey");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void loadApiKey() {
        try {
            URL url = new URL("https://financialmodelingprep.com/api/v3/income-statement/AAPL?apikey=" + apikey);

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8))) {
                for (String line; (line = reader.readLine()) != null;) {
                    System.out.println(line);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
