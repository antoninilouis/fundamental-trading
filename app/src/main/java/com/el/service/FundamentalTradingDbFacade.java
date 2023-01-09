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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class FundamentalTradingDbFacade {

  private static final Logger logger = LoggerFactory.getLogger(FundamentalTradingDbFacade.class);
  private final Jdbi jdbi;

  public FundamentalTradingDbFacade(String dbpath) {
    try {
      final Properties appProps = new Properties();

      appProps.load(FundamentalTradingDbFacade.class.getResourceAsStream("/fundamental-tradingDB.properties"));

      final var user = appProps.getProperty("user");
      final var pwd = appProps.getProperty("password");
      if (dbpath == null) {
        dbpath = appProps.getProperty("dbpath");
      }
      this.jdbi = Jdbi.create("jdbc:derby:" + dbpath + ";create=true", user, pwd);
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

  public void insertStockReturnOnEquity(String symbol, TreeMap<LocalDate, Double> roes) {
    try {
      final int[] batchInserts = jdbi.withExtension(MarketDataDAO.class, dao -> dao.insertStockReturnOnEquity(symbol, roes.entrySet()));
      logger.info("Inserted {} roe entries for symbol {}", Arrays.stream(batchInserts).sum(), symbol);
    } catch (JdbiException e) {
      throw new RuntimeException(e.getMessage());
    }
  }

  public void insertStockDividendPayoutRatio(String symbol, TreeMap<LocalDate, Double> dividendPayoutRatios) {
    try {
      final int[] batchInserts = jdbi.withExtension(MarketDataDAO.class, dao -> dao.insertStockDividendPayoutRatios(symbol, dividendPayoutRatios.entrySet()));
      logger.info("Inserted {} dividend payout ratios entries for symbol {}", Arrays.stream(batchInserts).sum(), symbol);
    } catch (JdbiException e) {
      throw new RuntimeException(e.getMessage());
    }
  }

  public void insertRefreshHistoryEntry(String name) {
    try {
      jdbi.useExtension(MarketDataDAO.class, dao -> dao.insertRefreshHistoryEntry(name, Instant.now()));
    } catch (JdbiException e) {
      throw new RuntimeException(e.getMessage());
    }
  }

  public Optional<Timestamp> getLatestRefreshTimestamp(final String name) {
    return jdbi.withHandle(handle ->
      handle.createQuery("select TIMESTAMP from APP.REFRESH_HISTORY where NAME = :name order by TIMESTAMP desc fetch first 1 rows only")
        .bind("name", name)
        .mapTo(Timestamp.class)
        .findFirst());
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
            .bind("from", LocalDate.ofInstant(from, ZoneId.of("America/New_York")))
            .bind("to", LocalDate.ofInstant(to, ZoneId.of("America/New_York")))
            .collectInto(new GenericType<TreeMap<LocalDate, Double>>() {}))
      ))
      .entrySet().stream().filter(entry -> !entry.getValue().isEmpty()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  public TreeMap<LocalDate, Double> getCachedIndexPrices(String index, Instant from, Instant to) {
    return jdbi.withHandle(handle ->
      handle.createQuery("select * from APP.INDEX_PRICES where INDEX = :index and TIMESTAMP between :from and :to")
        .registerRowMapper(new DoubleMapper("PRICE"))
        .registerRowMapper(new MarketDataDAO.LocalDateMapper())
        .bind("index", index)
        .bind("from", LocalDate.ofInstant(from, ZoneId.of("America/New_York")))
        .bind("to", LocalDate.ofInstant(to, ZoneId.of("America/New_York")))
        .collectInto(new GenericType<TreeMap<LocalDate, Double>>() {}));
  }

  public TreeMap<LocalDate, Double> getCachedTbReturns(Instant from, Instant to) {
    return jdbi.withHandle(handle ->
      handle.createQuery("select * from APP.TB_RETURNS where TIMESTAMP between :from and :to")
        .registerRowMapper(new DoubleMapper("RETURN"))
        .registerRowMapper(new MarketDataDAO.LocalDateMapper())
        .bind("from", LocalDate.ofInstant(from, ZoneId.of("America/New_York")))
        .bind("to", LocalDate.ofInstant(to, ZoneId.of("America/New_York")))
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
            .bind("from", LocalDate.ofInstant(from, ZoneId.of("America/New_York")))
            .bind("to", LocalDate.ofInstant(to, ZoneId.of("America/New_York")))
            .collectInto(new GenericType<TreeMap<LocalDate, Double>>() {}))
      ))
      .entrySet().stream().filter(entry -> !entry.getValue().isEmpty()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  public Map<String, TreeMap<LocalDate, Double>> getCachedStockReturnOnEquity(Set<String> symbols, Instant from, Instant to) {
    return symbols.stream()
      .collect(Collectors.toMap(
        Function.identity(),
        symbol -> jdbi.withHandle(handle ->
          handle.createQuery("select * from APP.STOCK_RETURN_ON_EQUITY where SYMBOL = :symbol and TIMESTAMP between :from and :to")
            .registerRowMapper(new DoubleMapper("RETURN"))
            .registerRowMapper(new MarketDataDAO.LocalDateMapper())
            .bind("symbol", symbol)
            .bind("from", LocalDate.ofInstant(from, ZoneId.of("America/New_York")))
            .bind("to", LocalDate.ofInstant(to, ZoneId.of("America/New_York")))
            .collectInto(new GenericType<TreeMap<LocalDate, Double>>() {}))
      ))
      .entrySet().stream().filter(entry -> !entry.getValue().isEmpty()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  public Map<String, TreeMap<LocalDate, Double>> getCachedStockDividendPayoutRatio(Set<String> symbols, Instant from, Instant to) {
    return symbols.stream()
      .collect(Collectors.toMap(
        Function.identity(),
        symbol -> jdbi.withHandle(handle ->
          handle.createQuery("select * from APP.STOCK_DIVIDEND_PAYOUT_RATIO where SYMBOL = :symbol and TIMESTAMP between :from and :to")
            .registerRowMapper(new DoubleMapper("DIVIDEND_PAYOUT_RATIO"))
            .registerRowMapper(new MarketDataDAO.LocalDateMapper())
            .bind("symbol", symbol)
            .bind("from", LocalDate.ofInstant(from, ZoneId.of("America/New_York")))
            .bind("to", LocalDate.ofInstant(to, ZoneId.of("America/New_York")))
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
