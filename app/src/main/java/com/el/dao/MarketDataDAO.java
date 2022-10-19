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
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public interface MarketDataDAO {
  @SqlQuery("select * from APP.STOCK_PRICES where SYMBOL = :symbol and TIMESTAMP between :from and :to")
  @RegisterRowMapper(LocalDateMapper.class)
  @RegisterRowMapper(PriceDoubleMapper.class)
  TreeMap<LocalDate, Double> getPricesBetween(@Bind("symbol") String symbol, @Bind("from") Instant from, @Bind("to") Instant to);

  @SqlBatch("insert into APP.STOCK_PRICES (SYMBOL, TIMESTAMP, PRICE) VALUES (:symbol, :prices.getKey, :prices.getValue)")
  int[] insertStockPrices(@Bind("symbol") String symbol, @BindMethods("prices") Collection<Map.Entry<LocalDate, Double>> prices);

  @SqlBatch("insert into APP.INDEX_PRICES (INDEX, TIMESTAMP, PRICE) VALUES (:index, :prices.getKey, :prices.getValue)")
  int[] insertIndexPrices(@Bind("index") String index, @BindMethods("prices") Set<Map.Entry<LocalDate, Double>> entrySet);

  @SqlBatch("insert into APP.TB_RETURNS (TIMESTAMP, RETURN) VALUES (:returns.getKey, :returns.getValue)")
  int[] insertTbReturns(@BindMethods("returns") Set<Map.Entry<LocalDate, Double>> entrySet);

  @SqlBatch("insert into APP.STOCK_DIVIDENDS (SYMBOL, TIMESTAMP, DIVIDEND) VALUES (:symbol, :dividends.getKey, :dividends.getValue)")
  int[] insertStockDividends(@Bind("symbol") String symbol, @BindMethods("dividends") Set<Map.Entry<LocalDate, Double>> dividends);

  @SqlBatch("insert into APP.STOCK_RETURN_ON_EQUITY (SYMBOL, TIMESTAMP, RETURN) VALUES (:symbol, :roes.getKey, :roes.getValue)")
  int[] insertStockReturnOnEquity(@Bind("symbol") String symbol, @BindMethods("roes") Set<Map.Entry<LocalDate, Double>> roes);

  @SqlBatch("insert into APP.STOCK_DIVIDEND_PAYOUT_RATIO (SYMBOL, TIMESTAMP, DIVIDEND_PAYOUT_RATIO) VALUES (:symbol, :dividendPayoutRatios.getKey, :dividendPayoutRatios.getValue)")
  int[] insertStockDividendPayoutRatios(@Bind("symbol") String symbol, @BindMethods("dividendPayoutRatios") Set<Map.Entry<LocalDate, Double>> dividendPayoutRatios);

  /**
   * Important: db TIMESTAMP's represent ZonedDateTime for America/New_York, not Instants!
   */
  class LocalDateMapper implements RowMapper<LocalDate> {

    @Override
    public LocalDate map(ResultSet rs, StatementContext ctx) throws SQLException {
      return rs.getTimestamp("TIMESTAMP").toLocalDateTime().toLocalDate();
    }
  }

  class PriceDoubleMapper implements RowMapper<Double> {

    @Override
    public Double map(ResultSet rs, StatementContext ctx) throws SQLException {
      return rs.getDouble("PRICE");
    }
  }
}
