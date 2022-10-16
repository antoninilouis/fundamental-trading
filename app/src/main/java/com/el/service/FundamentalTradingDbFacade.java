package com.el.service;

import com.el.dao.MarketDataDAO;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.JdbiException;
import org.jdbi.v3.core.generic.GenericType;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.Slf4JSqlLogger;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
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
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public void insertStockPrices(String symbol, TreeMap<LocalDate, Double> prices) {
    try {
      final int[] batchInserts = jdbi.withExtension(MarketDataDAO.class, dao -> dao.insertStockPrices(symbol, prices.entrySet()));
      logger.info("Inserted {} prices entries for symbol {}", Arrays.stream(batchInserts).sum(), symbol);
    } catch (JdbiException e) {
      throw new RuntimeException(e.getMessage());
    }
  }

  public void insertIndexPrices(String index, TreeMap<LocalDate, Double> prices) {
    try {
      final int[] batchInserts = jdbi.withExtension(MarketDataDAO.class, dao -> dao.insertIndexPrices(index, prices.entrySet()));
      logger.info("Inserted {} entries for index {}", Arrays.stream(batchInserts).sum(), index);
    } catch (JdbiException e) {
      throw new RuntimeException(e.getMessage());
    }
  }

  public void insertTbReturns(TreeMap<LocalDate, Double> tbReturns) {
    try {
      final int[] batchInserts = jdbi.withExtension(MarketDataDAO.class, dao -> dao.insertTbReturns(tbReturns.entrySet()));
      logger.info("Inserted {} entries for TB-Returns", Arrays.stream(batchInserts).sum());
    } catch (JdbiException e) {
      throw new RuntimeException(e.getMessage());
    }
  }

  public void insertStockDividends(String symbol, TreeMap<LocalDate, Double> dividends) {
    try {
      final int[] batchInserts = jdbi.withExtension(MarketDataDAO.class, dao -> dao.insertStockDividends(symbol, dividends.entrySet()));
      logger.info("Inserted {} dividends entries for symbol {}", Arrays.stream(batchInserts).sum(), symbol);
    } catch (JdbiException e) {
      throw new RuntimeException(e.getMessage());
    }
  }

  public Map<String, TreeMap<LocalDate, Double>> getCachedStockPrices(Set<String> symbols, Instant from, Instant to) {
    return symbols.stream()
      .collect(Collectors.toMap(
        Function.identity(),
        symbol -> jdbi.withHandle(handle ->
          handle.createQuery("select * from APP.STOCK_PRICES where SYMBOL = :symbol and TIMESTAMP between :from and :to")
            .registerRowMapper(new DoubleMapper("PRICE"))
            .registerRowMapper(new MarketDataDAO.LocalDateMapper())
            .bind("symbol", symbol)
            .bind("from", Timestamp.from(from))
            .bind("to", Timestamp.from(to))
            .collectInto(new GenericType<TreeMap<LocalDate, Double>>() {}))
      ))
      .entrySet().stream().filter(entry -> !entry.getValue().isEmpty()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  public TreeMap<LocalDate, Double> getCachedTbReturns(String index, Instant from, Instant to) {
    return jdbi.withHandle(handle ->
      handle.createQuery("select * from APP.INDEX_PRICES where INDEX = :index and TIMESTAMP between :from and :to")
        .registerRowMapper(new DoubleMapper("PRICE"))
        .registerRowMapper(new MarketDataDAO.LocalDateMapper())
        .bind("index", index)
        .bind("from", Timestamp.from(from))
        .bind("to", Timestamp.from(to))
        .collectInto(new GenericType<TreeMap<LocalDate, Double>>() {}));
  }

  public TreeMap<LocalDate, Double> getCachedTbReturns(Instant from, Instant to) {
    return jdbi.withHandle(handle ->
      handle.createQuery("select * from APP.TB_RETURNS where TIMESTAMP between :from and :to")
        .registerRowMapper(new DoubleMapper("RETURN"))
        .registerRowMapper(new MarketDataDAO.LocalDateMapper())
        .bind("from", Timestamp.from(from))
        .bind("to", Timestamp.from(to))
        .collectInto(new GenericType<TreeMap<LocalDate, Double>>() {}));
  }

  public Map<String, TreeMap<LocalDate, Double>> getCachedStockDividends(Set<String> symbols, Instant from, Instant to) {
    return symbols.stream()
      .collect(Collectors.toMap(
        Function.identity(),
        symbol -> jdbi.withHandle(handle ->
          handle.createQuery("select * from APP.STOCK_DIVIDENDS where SYMBOL = :symbol and TIMESTAMP between :from and :to")
            .registerRowMapper(new DoubleMapper("DIVIDEND"))
            .registerRowMapper(new MarketDataDAO.LocalDateMapper())
            .bind("symbol", symbol)
            .bind("from", Timestamp.from(from))
            .bind("to", Timestamp.from(to))
            .collectInto(new GenericType<TreeMap<LocalDate, Double>>() {}))
      ))
      .entrySet().stream().filter(entry -> !entry.getValue().isEmpty()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  private static class DoubleMapper implements RowMapper<Double> {

    private final String columnLabel;

    public DoubleMapper(final String columnLabel) {
      this.columnLabel = columnLabel;
    }

    @Override
    public Double map(ResultSet rs, StatementContext ctx) throws SQLException {
      return rs.getDouble(this.columnLabel);
    }
  }
}
