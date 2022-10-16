package com.el.service;

import com.el.dao.StockPriceDAO;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.generic.GenericType;
import org.jdbi.v3.core.statement.Slf4JSqlLogger;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class FundamentalTradingDbFacade {

  private static final Logger logger = LoggerFactory.getLogger(FundamentalTradingDbFacade.class);
  private final Jdbi jdbi;

  public FundamentalTradingDbFacade() {
    try {
      final Properties appProps = new Properties();
      final String appConfigPath = Objects.requireNonNull(getClass().getClassLoader().getResource("fundamental-tradingDB.properties")).getPath();

      appProps.load(new FileInputStream(appConfigPath));

      final var user = appProps.getProperty("user");
      final var pwd = appProps.getProperty("password");
      this.jdbi = Jdbi.create("jdbc:derby:/Users/louisantonini/Workspace/fundamental-trading/fundamental-tradingDB", user, pwd);
      jdbi.installPlugin(new SqlObjectPlugin());
      jdbi.setSqlLogger(new Slf4JSqlLogger(logger));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void insertStockPrices(String symbol, TreeMap<LocalDate, Double> prices) {
    jdbi.useExtension(StockPriceDAO.class, dao -> dao.insertStockPrices(symbol, prices.entrySet()));
  }

  public boolean contains(String symbol) {
    return jdbi.withHandle(handle -> !handle.select("select * from APP.STOCK_PRICES LIMIT 1").mapTo(String.class).findOnly().isEmpty());
  }

  public Map<String, TreeMap<LocalDate, Double>> getCachedStockPrices(Set<String> symbols, Instant from, Instant to) {
    return symbols.stream()
      .collect(Collectors.toMap(
        Function.identity(),
        symbol -> jdbi.withHandle(handle ->
          handle.createQuery("select * from APP.STOCK_PRICES where SYMBOL = :symbol and TIMESTAMP between :from and :to")
            .registerRowMapper(new StockPriceDAO.DoubleMapper())
            .registerRowMapper(new StockPriceDAO.LocalDateMapper())
            .bind("symbol", symbol)
            .bind("from", Timestamp.from(from))
            .bind("to", Timestamp.from(to))
            .collectInto(new GenericType<TreeMap<LocalDate, Double>>() {}))
      ))
      .entrySet().stream().filter(entry -> !entry.getValue().isEmpty()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }
}
