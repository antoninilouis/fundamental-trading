package com.el.stockselection;

import com.el.marketdata.MarketDataRepository;

import java.time.LocalDate;
import java.util.TreeMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class EquityScreener {

    private final MarketDataRepository marketDataRepository;

    public EquityScreener(
        MarketDataRepository marketDataRepository
    ) {
        this.marketDataRepository = marketDataRepository;
    }

    public Set<String> screenEquities() {
        final var symbols = marketDataRepository.getSymbols();
        return symbols.stream().filter(this::testSymbol).collect(Collectors.toSet());
    }

    private boolean testSymbol(String symbol) {
        final var stockPrices = marketDataRepository.getPastStockPrices(symbol);
        final var stockDividends = marketDataRepository.getPastStockDividends(symbol);

        var k = CAPM.compute(marketDataRepository, symbol);

        // expected return
        final var returnOnEquity = marketDataRepository.getStockReturnOnEquity(symbol);
        final var dividendPayoutRatio = marketDataRepository.getStockDividendPayoutRatio(symbol);
        final Double growthRate = computeGrowthRate(returnOnEquity, dividendPayoutRatio);
        final var er = computeExpectedReturnsOnShare(stockPrices, stockDividends, growthRate);

        // intrinsic value
        final var v0 = computeIntrinsicValueOfShare(stockPrices, stockDividends, growthRate, k);

        // market price
        final var m = stockPrices.entrySet().stream()
            .max(Map.Entry.comparingByKey()).orElseThrow().getValue();

        // todo: test g - P/E ~= 0
        return er > k && v0 > m;
    }

    private double computeIntrinsicValueOfShare(
        TreeMap<LocalDate, Double> stockPrices,
        TreeMap<LocalDate, Double> stockDividends,
        Double growthRate,
        Double k
    ) {
        // E(P0)
        final double latestPrice = stockPrices.entrySet().stream()
            .max(Map.Entry.comparingByKey()).orElseThrow().getValue();
        // E(D0)
        final double latestDividend = stockDividends.entrySet().stream()
            .max(Map.Entry.comparingByKey()).orElseThrow().getValue();
        // E(P1)
        final var forecastedPrice = latestPrice * (1 + growthRate);
        // E(D1)
        final var forecastedDividends = latestDividend * (1 + growthRate);
        return (forecastedDividends + forecastedPrice) / (1 + k);
    }

    private double computeExpectedReturnsOnShare(
        TreeMap<LocalDate, Double> stockPrices,
        TreeMap<LocalDate, Double> stockDividends,
        Double growthRate
    ) {
        // E(P0)
        final double latestPrice = stockPrices.entrySet().stream()
            .max(Map.Entry.comparingByKey()).orElseThrow().getValue();
        // E(D0)
        // todo: verify in which units the dividends are
        final double latestDividend = stockDividends.entrySet().stream()
            .max(Map.Entry.comparingByKey()).orElseThrow().getValue();
        // E(P1)
        final var forecastedPrice = latestPrice * (1 + growthRate);
        // E(D1)
        final var forecastedDividends = latestDividend * (1 + growthRate);
        // E(r)
        return (forecastedDividends + forecastedPrice - latestPrice) / latestPrice;
    }

    private static Double computeGrowthRate(Double returnOnEquity, Double dividendPayoutRatio) {
        return returnOnEquity * (1.0 - dividendPayoutRatio);
    }
}
