package com.el.service;

import com.el.dao.StockPriceDAO;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;

import java.io.FileInputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class FundamentalTradingDbFacade {

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
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void insertStockPrice(String symbol, Double price, Instant timestamp) {
    jdbi.withHandle(handle -> handle.execute("insert into APP.STOCK_PRICES (symbol, price, timestamp) values (?,?,?)",
      symbol,
      price,
      timestamp
    ));
  }

  public boolean contains(String symbol) {
    return jdbi.withHandle(handle -> !handle.select("select * from APP.STOCK_PRICES LIMIT 1").mapTo(String.class).findOnly().isEmpty());
  }

  public Map<String, TreeMap<LocalDate, Double>> getCachedStockPrices(Set<String> symbols, Instant from, Instant to) {
    return symbols.stream().collect(Collectors.toMap(
      Function.identity(),
      symbol -> jdbi.withExtension(StockPriceDAO.class, dao -> dao.getPricesBetween(symbol, from, to))
    ));
  }
}
