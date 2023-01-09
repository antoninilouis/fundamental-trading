package com.el.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.jacobpeterson.alpaca.AlpacaAPI;
import net.jacobpeterson.alpaca.model.endpoint.account.Account;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.common.historical.bar.enums.BarTimePeriod;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.stock.historical.bar.StockBar;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.stock.historical.bar.enums.BarAdjustment;
import net.jacobpeterson.alpaca.model.endpoint.orders.Order;
import net.jacobpeterson.alpaca.model.endpoint.orders.enums.OrderClass;
import net.jacobpeterson.alpaca.model.endpoint.orders.enums.OrderSide;
import net.jacobpeterson.alpaca.model.endpoint.orders.enums.OrderTimeInForce;
import net.jacobpeterson.alpaca.model.endpoint.orders.enums.OrderType;
import net.jacobpeterson.alpaca.rest.AlpacaClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Fixme:
 * 1. No order placement error handling
 * 2. No price validation with db values
 * 3. No positions verification
 */
public class AlpacaService {

  private final Logger logger = LoggerFactory.getLogger(AlpacaService.class);
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final AlpacaAPI alpacaAPI = new AlpacaAPI();

  /**
   * Tool to avoid PDT flagging
   * each trade day alternates between B (Buying) day and S (Selling) day (B-S-B-S and so on)
   * the tool uses a strategy to determine best time to buy or sell on a B or S day respectively
   * a possible strategy is to compute Bollinger bands of the portfolio and buy or sell when it hits the low or high band during a trade day
   *
   * Determine when lower bolling band for this portfolio is hit today, refresh every 15min
   */
  public void atBestEntryPoint(Map<String, Double> portfolio, Runnable callback) {
//    final var queue = new CircularFifoQueue<>(20);
//    queue.add()
//
//    double[] values = ArrayUtils.toPrimitive(queue.toArray(new Double[0]));
//    StatUtils.mean(values, 0, 20);

    callback.run();
  }

  public void buyPortfolio(final Map<String, Double> portfolio, final Double cash) {
    try {
      showExpectedPositions(portfolio, cash);
      portfolio.forEach((key, value) -> {
        try {
          final var symbol = key.equals("GSPC") ? "SPY" : key;
          final var optBar = getStockBar(symbol);

          if (optBar.isEmpty()) {
            throw new RuntimeException("Order could not be placed because of missing bar for symbol %s".formatted(symbol));
          }

          optBar.map(bar -> {
            final var price = bar.getClose();
            if (value >= 0.0) {
              final var quantity = (cash * value) / price;
              logger.info("BUY {symbol: {}, price: {}, quantity: {}}", symbol, price, quantity);
              return buyStock(symbol, quantity);
            } else {
              final var quantity = (double) Math.abs(Math.round((cash * value) / price));
              logger.info("SELL {symbol: {}, price: {}, quantity: {}}", symbol, price, quantity);
              return sellStock(symbol, quantity);
            }
          });
        } catch (AlpacaClientException e) {
          throw new RuntimeException(e);
        }
      });
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public void sellPortfolio() {
    try {
      alpacaAPI.positions().closeAll(true);
    } catch (AlpacaClientException e) {
      throw new RuntimeException(e);
    }
  }

  private void showExpectedPositions(Map<String, Double> portfolio, Double cash) throws JsonProcessingException {
    final var targetPortfolio = portfolio.entrySet().stream().collect(Collectors.toMap(
      Map.Entry::getKey,
      entry -> cash * entry.getValue()
    ));
    logger.info(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(targetPortfolio));
  }

  private Order buyStock(String symbol, Double quantity) {
    try {
      return alpacaAPI.orders().requestOrder(
        symbol,
        quantity,
        null,
        OrderSide.BUY,
        OrderType.MARKET,
        OrderTimeInForce.DAY,
        null,
        null,
        null,
        null,
        false,
        null,
        OrderClass.SIMPLE,
        null,
        null,
        null
      );
    } catch (AlpacaClientException e) {
      throw new RuntimeException(e);
    }
  }

  private Order sellStock(String symbol, Double quantity) {
    try {
      return alpacaAPI.orders().requestOrder(
        symbol,
        quantity,
        null,
        OrderSide.SELL,
        OrderType.MARKET,
        OrderTimeInForce.DAY,
        null,
        null,
        null,
        null,
        false,
        null,
        OrderClass.SIMPLE,
        null,
        null,
        null
      );
    } catch (AlpacaClientException e) {
      throw new RuntimeException(e);
    }
  }

  public Double getCash() throws AlpacaClientException {
    Account account = alpacaAPI.account().get();
    return Double.valueOf(account.getCash());
  }

  public Optional<StockBar> getStockBar(final String symbol) throws AlpacaClientException {
    final var response = alpacaAPI.stockMarketData().getBars(
      symbol.equals("GSPC") ? "SPY" : symbol,
      Instant.now().minus(24, ChronoUnit.HOURS).atZone(ZoneId.of("America/New_York")),
      Instant.now().minus(1, ChronoUnit.HOURS).atZone(ZoneId.of("America/New_York")),
      null,
      null,
      1,
      BarTimePeriod.HOUR,
      BarAdjustment.SPLIT,
      null
    );
    if (response.getBars() == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(response.getBars().get(response.getBars().size() - 1));
  }
}
