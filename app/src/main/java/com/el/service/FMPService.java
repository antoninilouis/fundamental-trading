package com.el.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

  private static final Logger logger = LoggerFactory.getLogger(FMPService.class);
  private static final String BASE_URL = "https://financialmodelingprep.com/api";
  private final ObjectMapper om = new ObjectMapper();
  private final OkHttpClient client;
  private final String apikey;

  public FMPService() {
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

  private JsonNode extract(Request request) {
    try (Response response = client.newCall(request).execute()) {
      if (!response.isSuccessful()) {
        logger.warn("Unsuccessful response: {}", response);
      }
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
    logger.info("Calling FMP to get prices of index {}", indexName);
    return getResultAsLocalDateDoubleTreeMap(request, "close");
  }

  public TreeMap<LocalDate, Double> getIndexPricesUpdates(String indexName, AbstractMap.SimpleEntry<LocalDate, LocalDate> periodToFetch) {
    final var from = periodToFetch.getKey();
    final var to = periodToFetch.getValue();
    final Request request = new Request.Builder()
      .url(BASE_URL + "/v3/historical-price-full/%5E" + indexName + "?apikey=" + apikey + "&from=" + from + "&to=" + to)
      .method("GET", null)
      .build();
    logger.info("Calling FMP to get prices updates of index {}", indexName);
    return getResultAsLocalDateDoubleTreeMap(request, "close");
  }

  public Map<String, TreeMap<LocalDate, Double>> getStockPrices(Set<String> symbols, Instant from, Instant to) {
    return symbols.stream().collect(Collectors.toMap(
      Function.identity(),
      symbol -> {
        final Request request = new Request.Builder()
          .url(BASE_URL + "/v3/historical-price-full/" + symbol + "?apikey=" + apikey + "&serietype=line&from=" + LocalDate.ofInstant(from, ZoneId.of("America/New_York")) + "&to=" + LocalDate.ofInstant(to, ZoneId.of("America/New_York")))
          .method("GET", null)
          .build();
        logger.info("Calling FMP to get stock prices for symbol {}", symbol);
        return getResultAsLocalDateDoubleTreeMap(request, "close");
      }
    ));
  }

  public Map<String, TreeMap<LocalDate, Double>> getStockPricesUpdates(Map<String, AbstractMap.SimpleEntry<LocalDate, LocalDate>> periodsToFetch) {
    return periodsToFetch.entrySet().stream().collect(Collectors.toMap(
      Map.Entry::getKey,
      entry -> {
        final var symbol = entry.getKey();
        final var from = entry.getValue().getKey(); // SimpleEntry::getKey
        final var to = entry.getValue().getValue(); // SimpleEntry::getValue
        final Request request = new Request.Builder()
          .url(BASE_URL + "/v3/historical-price-full/" + symbol + "?apikey=" + apikey + "&serietype=line&from=" + from + "&to=" + to)
          .method("GET", null)
          .build();
        logger.info("Calling FMP to get stock prices updates for symbol {}", symbol);
        return getResultAsLocalDateDoubleTreeMap(request, "close");
      }
    ));
  }

  public TreeMap<LocalDate, Double> getTbReturns(Instant from, Instant to) {
    final var tmpFrom = LocalDate.ofInstant(from, ZoneId.of("America/New_York"));
    final var tmpTo = LocalDate.ofInstant(to, ZoneId.of("America/New_York"));
    logger.info("Calling FMP to get TB returns");
    return getTbReturnsFromFMP(tmpFrom, tmpTo);
  }

  public TreeMap<LocalDate, Double> getTbReturnsUpdates(AbstractMap.SimpleEntry<LocalDate, LocalDate> periodToFetch) {
    final var tmpFrom = periodToFetch.getKey();
    final var tmpTo = periodToFetch.getValue();
    logger.info("Calling FMP to get TB returns updates");
    return getTbReturnsFromFMP(tmpFrom, tmpTo);
  }

  @Nullable
  private TreeMap<LocalDate, Double> getTbReturnsFromFMP(LocalDate tmpFrom, LocalDate tmpTo) {
    final var map = new TreeMap<LocalDate, Double>();
    while (map.isEmpty() || map.firstKey().isAfter(tmpFrom)) {
      final Request request = new Request.Builder()
        .url(BASE_URL + "/v4/treasury/?apikey=" + apikey + "&from=" + tmpFrom + "&to=" + tmpTo)
        .method("GET", null)
        .build();
      final var jsonNode = extract(request);
      if (jsonNode.isEmpty()) {
        return map;
      }
      StreamSupport.stream(jsonNode.spliterator(), false)
        .forEach(e -> map.put(LocalDate.parse(e.get("date").toString().replaceAll("\"", "")),
          Double.valueOf(e.get("month3").toString())));
      tmpTo = map.firstKey().minusDays(1);
    }
    return null;
  }

  public Map<String, TreeMap<LocalDate, Double>> getStockDividends(Set<String> symbols, Instant from, Instant to) {
    return symbols.stream().collect(Collectors.toMap(
      Function.identity(),
      symbol -> {
        logger.info("Calling FMP to get stock dividends for symbol {}", symbol);
        final Request request = new Request.Builder()
          .url(BASE_URL + "/v3/historical-price-full/stock_dividend/" + symbol + "?apikey=" + apikey + "&from=" + LocalDate.ofInstant(from, ZoneId.of("America/New_York")) + "&to=" + LocalDate.ofInstant(to, ZoneId.of("America/New_York")))
          .method("GET", null)
          .build();
        return getResultAsLocalDateDoubleTreeMap(request, "dividend");
      }
    ));
  }

  public Map<String, TreeMap<LocalDate, Double>> getStockDividendsUpdates(Map<String, AbstractMap.SimpleEntry<LocalDate, LocalDate>> periodsToFetch) {
    return periodsToFetch.entrySet().stream().collect(Collectors.toMap(
      Map.Entry::getKey,
      entry -> {
        final var symbol = entry.getKey();
        final var tmpFrom = entry.getValue().getKey();
        final var tmpTo = entry.getValue().getValue();
        logger.info("Calling FMP to get stock dividends updates for symbol {}", symbol);
        final Request request = new Request.Builder()
          .url(BASE_URL + "/v3/historical-price-full/stock_dividend/" + symbol + "?apikey=" + apikey + "&from=" + tmpFrom + "&to=" + tmpTo)
          .method("GET", null)
          .build();
        return getResultAsLocalDateDoubleTreeMap(request, "dividend");
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
        logger.info("Calling FMP to get stock return on equity for symbol {}", symbol);
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
        logger.info("Calling FMP to get stock dividend payout ratios for symbol {}", symbol);
        final var jsonNode = extract(request);
        return StreamSupport.stream(jsonNode.spliterator(), false)
          .filter(n -> n.get("date") != null && !n.get("dividendPayoutRatio").toString().equals("null"))
          .collect(Collectors.toMap(
            n -> LocalDate.parse(n.get("date").toString().replaceAll("\"", "")),
            n -> Double.parseDouble(n.get("dividendPayoutRatio").toString()),
            (o1, o2) -> o1,
            TreeMap::new
          ));
      }
    ));
  }

  private TreeMap<LocalDate, Double> getResultAsLocalDateDoubleTreeMap(final Request request, String property) {
    final var jsonNode = extract(request);
    if (!jsonNode.has("historical")) {
      return new TreeMap<>();
    }
    return StreamSupport.stream(jsonNode.get("historical").spliterator(), false)
      .filter(n -> n.get("date") != null && n.get(property) != null)
      .collect(Collectors.toMap(
        n -> LocalDate.parse(n.get("date").toString().replaceAll("\"", "")),
        n -> Double.valueOf(n.get(property).toString()),
        (o1, o2) -> o1,
        TreeMap::new
      ));
  }
}
