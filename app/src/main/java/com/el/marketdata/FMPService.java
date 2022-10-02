package com.el.marketdata;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.FileInputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class FMPService {

  private static final String BASE_URL = "https://financialmodelingprep.com/api/v3";
  private final ObjectMapper om = new ObjectMapper();
  private final OkHttpClient client;
  private final String apikey;

  FMPService() {
    client = new OkHttpClient();
    try {
      final Properties appProps = new Properties();
      final String appConfigPath = Objects.requireNonNull(getClass().getClassLoader().getResource("fmp.properties")).getPath();

      appProps.load(new FileInputStream(appConfigPath));
      this.apikey = appProps.getProperty("apikey");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void getFinancialRatios(final String symbol) {
    final Request request = new Request.Builder()
      .url(BASE_URL + "/ratios/" + symbol + "?apikey=" + apikey)
      .method("GET", null)
      .build();
    extract(request);
  }

  private JsonNode extract(Request request) {
    try (Response response = client.newCall(request).execute()) {
      return om.readTree(Objects.requireNonNull(response.body()).string());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public TreeMap<LocalDate, Double> getIndexPrices(String indexName, Instant from, Instant to) {
    final Request request = new Request.Builder()
      .url(BASE_URL + "/historical-price-full/%5EGSPC?apikey=" + apikey + "&from=" + LocalDate.ofInstant(from, ZoneId.of("America/New_York")) + "&to=" + LocalDate.ofInstant(to, ZoneId.of("America/New_York")))
      .method("GET", null)
      .build();
    final var jsonNode = extract(request);
    return StreamSupport.stream(jsonNode.get("historical").spliterator(), false)
      .collect(Collectors.toMap(
        n -> LocalDate.parse(n.get("date").toString().replaceAll("\"", "")),
        n -> Double.valueOf(n.get("close").toString()),
        (o1, o2) -> o1,
        TreeMap::new
      ));
  }

  public Map<String, TreeMap<LocalDate, Double>> getStockPrices(Set<String> symbols, Instant from, Instant to) {
    return symbols.stream().collect(Collectors.toMap(
      Function.identity(),
      symbol -> {
        final Request request = new Request.Builder()
          .url(BASE_URL + "/historical-price-full/" + symbol + "?apikey=" + apikey + "&serietype=line&from=" + LocalDate.ofInstant(from, ZoneId.of("America/New_York")) + "&to=" + LocalDate.ofInstant(to, ZoneId.of("America/New_York")))
          .method("GET", null)
          .build();
        final var jsonNode = extract(request);
        return StreamSupport.stream(jsonNode.get("historical").spliterator(), false)
          .collect(Collectors.toMap(
            n -> LocalDate.parse(n.get("date").toString().replaceAll("\"", "")),
            n -> Double.valueOf(n.get("close").toString()),
            (o1, o2) -> o1,
            TreeMap::new
          ));
      }
    ));
  }
}
