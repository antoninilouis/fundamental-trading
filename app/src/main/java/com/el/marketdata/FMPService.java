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

  private static final String BASE_URL = "https://financialmodelingprep.com/api";
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
      .url(BASE_URL + "/v3/ratios/" + symbol + "?apikey=" + apikey)
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
      .url(BASE_URL + "/v3/historical-price-full/%5E" + indexName + "?apikey=" + apikey + "&from=" + LocalDate.ofInstant(from, ZoneId.of("America/New_York")) + "&to=" + LocalDate.ofInstant(to, ZoneId.of("America/New_York")))
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
          .url(BASE_URL + "/v3/historical-price-full/" + symbol + "?apikey=" + apikey + "&serietype=line&from=" + LocalDate.ofInstant(from, ZoneId.of("America/New_York")) + "&to=" + LocalDate.ofInstant(to, ZoneId.of("America/New_York")))
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

  public TreeMap<LocalDate, Double> getTbReturns(Instant from, Instant to) {
    // todo: handle cases where loop can be infinite
    var tmpFrom = LocalDate.ofInstant(from, ZoneId.of("America/New_York"));
    var tmpTo = LocalDate.ofInstant(to, ZoneId.of("America/New_York"));
    var map = new TreeMap<LocalDate, Double>();
    while (map.isEmpty() || map.firstKey().isAfter(tmpFrom)) {
      final Request request = new Request.Builder()
        .url(BASE_URL + "/v4/treasury/?apikey=" + apikey + "&from=" + tmpFrom + "&to=" + tmpTo)
        .method("GET", null)
        .build();
      final var jsonNode = extract(request);
      StreamSupport.stream(jsonNode.spliterator(), false)
        .forEach(e -> map.put(LocalDate.parse(e.get("date").toString().replaceAll("\"", "")),
          Double.valueOf(e.get("month3").toString())));
      tmpTo = map.firstKey().minusDays(1);
    }
    return map;
  }

  public Map<String, TreeMap<LocalDate, Double>> getStockDividends(Set<String> symbols, Instant from, Instant to) {
    return symbols.stream().collect(Collectors.toMap(
      Function.identity(),
      symbol -> {
        final Request request = new Request.Builder()
          .url(BASE_URL + "/v3/historical-price-full/stock_dividend/" + symbol + "?apikey=" + apikey + "&from=" + LocalDate.ofInstant(from, ZoneId.of("America/New_York")) + "&to=" + LocalDate.ofInstant(to, ZoneId.of("America/New_York")))
          .method("GET", null)
          .build();
        final var jsonNode = extract(request);
        if (jsonNode.get("historical") == null) {
          return new TreeMap<>();
        }
        return StreamSupport.stream(jsonNode.get("historical").spliterator(), false)
          .collect(Collectors.toMap(
            n -> LocalDate.parse(n.get("date").toString().replaceAll("\"", "")),
            n -> Double.valueOf(n.get("dividend").toString()),
            (o1, o2) -> o1,
            TreeMap::new
          ));
      }
    ));
  }

  public Map<String, TreeMap<LocalDate, Double>> getStockReturnOnEquity(Set<String> symbols, Instant from, Instant to) {
    // todo: handle cases where no return on equity is present in the response
    return symbols.stream().collect(Collectors.toMap(
      Function.identity(),
      symbol -> {
        final Request request = new Request.Builder()
          .url(BASE_URL + "/v3/ratios/" + symbol + "?apikey=" + apikey + "&from=" + LocalDate.ofInstant(from, ZoneId.of("America/New_York")) + "&to=" + LocalDate.ofInstant(to, ZoneId.of("America/New_York")))
          .method("GET", null)
          .build();
        final var jsonNode = extract(request);
        return StreamSupport.stream(jsonNode.spliterator(), false)
          .collect(Collectors.toMap(
            n -> LocalDate.parse(n.get("date").toString().replaceAll("\"", "")),
            n -> Double.valueOf(n.get("returnOnEquity").toString()),
            (o1, o2) -> o1,
            TreeMap::new
          ));
      }
    ));
  }

  public Map<String, TreeMap<LocalDate, Double>> getStockDividendPayoutRatio(Set<String> symbols, Instant from, Instant to) {
    return symbols.stream().collect(Collectors.toMap(
      Function.identity(),
      symbol -> {
        final Request request = new Request.Builder()
          .url(BASE_URL + "/v3/ratios/" + symbol + "?apikey=" + apikey + "&from=" + LocalDate.ofInstant(from, ZoneId.of("America/New_York")) + "&to=" + LocalDate.ofInstant(to, ZoneId.of("America/New_York")))
          .method("GET", null)
          .build();
        final var jsonNode = extract(request);
        return StreamSupport.stream(jsonNode.spliterator(), false)
          .collect(Collectors.toMap(
            n -> LocalDate.parse(n.get("date").toString().replaceAll("\"", "")),
            n -> n.get("dividendPayoutRatio").toString().equals("null") ? Double.NaN : Double.parseDouble(n.get("dividendPayoutRatio").toString()),
            (o1, o2) -> o1,
            TreeMap::new
          ));
      }
    ));
  }
}
