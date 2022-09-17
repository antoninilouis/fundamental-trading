package com.el;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class EquityScreener {

    private final SymbolStatisticsRepository symbolStatisticsRepository;

    public EquityScreener(
        SymbolStatisticsRepository symbolStatisticsRepository
    ) {
        this.symbolStatisticsRepository = symbolStatisticsRepository;
    }

    public Set<String> screenEquities() {
        final var symbols = symbolStatisticsRepository.getSymbols();
        return symbols.stream().filter(this::testSymbol).collect(Collectors.toSet());
    }

    private boolean testSymbol(String symbol) {
        // capm
        final var stockPrices = symbolStatisticsRepository.getPastStockPrices(symbol);
        final var stockDividends = symbolStatisticsRepository.getPastStockDividends(symbol);
        final var k = CAPM.compute(symbolStatisticsRepository, symbol);

        // expected return
        final var returnOnEquity = symbolStatisticsRepository.getStockReturnOnEquity(symbol);
        final var dividendPayoutRatio = symbolStatisticsRepository.getStockDividendPayoutRatio(symbol);
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
        LinkedHashMap<LocalDate, Double> stockPrices,
        LinkedHashMap<LocalDate, Double> stockDividends,
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
        LinkedHashMap<LocalDate, Double> stockPrices,
        LinkedHashMap<LocalDate, Double> stockDividends,
        Double growthRate
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
        // E(r)
        return (forecastedDividends + forecastedPrice - latestPrice) / latestPrice;
    }

    private static Double computeGrowthRate(Double returnOnEquity, Double dividendPayoutRatio) {
        return returnOnEquity * (1.0 - dividendPayoutRatio);
    }
}
