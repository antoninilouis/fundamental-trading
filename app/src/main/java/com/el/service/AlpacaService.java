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

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Fixme:
 * 1. Alpaca doesn't support fractional short orders
 * 2. No order placement error handling
 * 3. No price validation with db values
 * 4. No positions verification
 */
public class AlpacaService {

  private final Logger logger = LoggerFactory.getLogger(AlpacaService.class);
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final AlpacaAPI alpacaAPI = new AlpacaAPI();
  private final FundamentalTradingDbFacade fundamentalTradingDbFacade = new FundamentalTradingDbFacade("fundamental-tradingDB");

  public void buyPortfolio(Map<String, Double> portfolio) {
    try {
      var cash = getCash();
      showExpectedPositions(portfolio, cash);
      portfolio.entrySet().stream()
        .filter(entry -> entry.getValue() >= 0)
        .forEach(entry -> {
          try {
            final var symbol = entry.getKey().equals("GSPC") ? "SPY" : entry.getKey();
            final var optBar = getStockBar(symbol);

            optBar.map(bar -> {
              final var price = bar.getClose();
              final var quantity = (cash * entry.getValue()) / price;
              logger.info("BUY \\{symbol: {}, price: {}, quantity: {}\\}", symbol, price, quantity);
              return buyStock(symbol, quantity);
            });
          } catch (AlpacaClientException e) {
            throw new RuntimeException(e);
          }
        });
    } catch (AlpacaClientException | JsonProcessingException e) {
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

  private Double getCash() throws AlpacaClientException {
    Account account = alpacaAPI.account().get();
    return Double.valueOf(account.getCash());
  }

  private Optional<StockBar> getStockBar(String symbol) throws AlpacaClientException {
    return alpacaAPI.stockMarketData().getBars(
      symbol,
      ZonedDateTime.now().minusHours(2),
      ZonedDateTime.now().minusHours(1),
      null,
      null,
      1,
      BarTimePeriod.HOUR,
      BarAdjustment.SPLIT,
      null
    ).getBars().stream().findFirst();
  }
}
