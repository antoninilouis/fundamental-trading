package com.el.marketdata;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;

import java.io.FileInputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.Objects;
import java.util.Properties;

public class FundamentalTradingDbFacade {

  private final Handle handle;

  public FundamentalTradingDbFacade() {
    try {
      final Properties appProps = new Properties();
      final String appConfigPath = Objects.requireNonNull(getClass().getClassLoader().getResource("fundamental-tradingDB.properties")).getPath();

      appProps.load(new FileInputStream(appConfigPath));

      final var user = appProps.getProperty("user");
      final var pwd = appProps.getProperty("password");
      final var jdbi = Jdbi.create("jdbc:derby:fundamental-tradingDB", user, pwd);
      handle = jdbi.open();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void insertStockPrice(String symbol, Double price, Instant timestamp) {
    handle.execute("insert into APP.STOCK_PRICES (symbol, price, timestamp) values (?,?,?)",
      symbol,
      price,
      timestamp);
  }
}
