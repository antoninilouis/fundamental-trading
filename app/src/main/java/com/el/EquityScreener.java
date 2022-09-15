package com.el;

import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

public class EquityScreener {

    private final SymbolStatisticsRepository symbolStatisticsRepository;
    private final LocalDate lastDate;

    public EquityScreener(
        SymbolStatisticsRepository symbolStatisticsRepository,
        LocalDate lastDate
    ) {
        this.symbolStatisticsRepository = symbolStatisticsRepository;
        this.lastDate = lastDate;
    }

    public Collection<String> screenEquities() {
        final var symbols = symbolStatisticsRepository.getSymbols();
        return symbols.stream().filter(this::testSymbol).collect(Collectors.toSet());
    }

    private boolean testSymbol(String symbol) {
        // capm
        final var indexPrices = symbolStatisticsRepository.getIndexPrices();
        final var tbReturns = symbolStatisticsRepository.getTbReturns();
        final var stockPrices = symbolStatisticsRepository.getStockPrices().get(symbol);
        final var stockDividends = symbolStatisticsRepository.getStockDividends().get(symbol);
        final var k = CAPM.compute(indexPrices, stockPrices, tbReturns, this.lastDate);

        // expected return
        final var returnOnEquity = symbolStatisticsRepository.getReturnOnEquity().get(symbol);
        final var dividendPayoutRatio = symbolStatisticsRepository.getDividendPayoutRatio().get(symbol);
        final Double growthRate = computeGrowthRate(returnOnEquity, dividendPayoutRatio);
        final var er = computeExpectedReturnsOnShare(stockPrices, stockDividends, growthRate);

        // intrinsic value
        final var v0 = computeIntrinsicValueOfShare(stockPrices, stockDividends, growthRate, k);

        // market price
        final var m = getLatestInDate(stockPrices);

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
        final double latestPrice = getLatestInDate(stockPrices);
        // E(D0)
        final double latestDividend = getLatestInDate(stockDividends);
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
        final double latestPrice = getLatestInDate(stockPrices);
        // E(D0)
        final double latestDividend = getLatestInDate(stockDividends);
        // E(P1)
        final var forecastedPrice = latestPrice * (1 + growthRate);
        // E(D1)
        final var forecastedDividends = latestDividend * (1 + growthRate);
        // E(r)
        return (forecastedDividends + forecastedPrice - latestPrice) / latestPrice;
    }

    private Double getLatestInDate(LinkedHashMap<LocalDate, Double> values) {
        final var optLatestValue = values.keySet().stream()
                .filter(localDate -> localDate.isBefore(this.lastDate.plusDays(1)))
                .max(LocalDate::compareTo);
        // todo: return optional instead
        if (optLatestValue.isEmpty()) {
            return 0.0;
        } else {
            return values.get(optLatestValue.get());
        }
    }

    private static Double computeGrowthRate(Double returnOnEquity, Double dividendPayoutRatio) {
        return returnOnEquity * (1.0 - dividendPayoutRatio);
    }
}
