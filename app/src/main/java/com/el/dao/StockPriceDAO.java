package com.el.dao;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindMethods;
import org.jdbi.v3.sqlobject.statement.SqlBatch;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public interface StockPriceDAO {
  @SqlQuery("select * from APP.STOCK_PRICES where SYMBOL = :symbol and TIMESTAMP between :from and :to")
  @RegisterRowMapper(LocalDateMapper.class)
  @RegisterRowMapper(DoubleMapper.class)
  TreeMap<LocalDate, Double> getPricesBetween(@Bind("symbol") String symbol, @Bind("from") Instant from, @Bind("to") Instant to);

  @SqlBatch("insert into APP.STOCK_PRICES (SYMBOL, TIMESTAMP, PRICE) VALUES (:symbol, :prices.getKey, :prices.getValue)")
  int[] insertStockPrices(@Bind("symbol") String symbol, @BindMethods("prices") Collection<Map.Entry<LocalDate, Double>> prices);

  @SqlBatch("insert into APP.INDEX_PRICES (INDEX, TIMESTAMP, PRICE) VALUES (:index, :prices.getKey, :prices.getValue)")
  int[] insertIndexPrices(@Bind("index") String index, @BindMethods("prices") Set<Map.Entry<LocalDate, Double>> entrySet);

  class LocalDateMapper implements RowMapper<LocalDate> {

    @Override
    public LocalDate map(ResultSet rs, StatementContext ctx) throws SQLException {
      return LocalDateTime.ofInstant(rs.getTimestamp("TIMESTAMP").toInstant(), ZoneId.of("America/New_York")).toLocalDate();
    }
  }

  class DoubleMapper implements RowMapper<Double> {

    @Override
    public Double map(ResultSet rs, StatementContext ctx) throws SQLException {
      return rs.getDouble("PRICE");
    }
  }
}
