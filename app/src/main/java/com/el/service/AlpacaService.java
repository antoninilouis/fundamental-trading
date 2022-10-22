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

import java.time.*;
import java.time.temporal.ChronoUnit;
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

  public void buyPortfolio(final Map<String, Double> portfolio, final Double cash) {
    try {
      showExpectedPositions(portfolio, cash);
      portfolio.entrySet().stream()
        .filter(entry -> entry.getValue() >= 0)
        .forEach(entry -> {
          try {
            final var symbol = entry.getKey().equals("GSPC") ? "SPY" : entry.getKey();
            final var optBar = getStockBar(symbol);

            // todo: log if order could not be placed because of missing bar
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
    } catch (JsonProcessingException e) {
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
    return Optional.ofNullable(response.getBars().get(response.getBars().size() - 1));
  }
}
