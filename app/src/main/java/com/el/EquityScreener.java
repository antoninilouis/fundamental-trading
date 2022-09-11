package com.el;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

public class EquityScreener {

    private final SymbolStatisticsRepository symbolStatisticsRepository;

    public EquityScreener(SymbolStatisticsRepository symbolStatisticsRepository) {
        this.symbolStatisticsRepository = symbolStatisticsRepository;
    }

    public Collection<String> screenEquities() {
        final var symbols = symbolStatisticsRepository.getSymbols();
        return symbols.stream().filter(this::testSymbol).collect(Collectors.toSet());
    }

    private boolean testSymbol(String symbol) {
        // capm
        final var indexPrices = symbolStatisticsRepository.getIndexPrices();
        final var tbReturns = symbolStatisticsRepository.getTbReturns();
        final var stockPrices = symbolStatisticsRepository.getStockPrices();
        final var stockDividends = symbolStatisticsRepository.getStockDividends();
        final var k = CAPM.compute(indexPrices, stockPrices, tbReturns, LocalDate.of(2022,5, 31));

        // expected return
        final var returnOnEquity = symbolStatisticsRepository.getReturnOnEquity();
        final var dividendPayoutRatio = symbolStatisticsRepository.getDividendPayoutRatio();
        final Double growthRate = computeGrowthRate(returnOnEquity, dividendPayoutRatio);
        final var er = computeExpectedReturnsOnShare(stockPrices, stockDividends, growthRate);

        // intrinsic value
        final var v0 = computeIntrinsicValueOfShare(stockPrices, stockDividends, growthRate, k);

        // market price
        final var optLatestPriceDate = stockPrices.keySet().stream().sorted()
                .filter(localDate -> localDate.isBefore(LocalDate.of(2022, 5, 31).plusDays(1)))
                .max(LocalDate::compareTo);
        final var m = stockPrices.get(optLatestPriceDate.get());

        // todo: test g - P/E ~= 0
        return er > k && v0 > m;
    }

    private static double computeIntrinsicValueOfShare(
            Map<LocalDate, Double> stockPrices,
            Map<LocalDate, Double> stockDividends,
            Double growthRate,
            Double k
    ) {
        // E(P0)
        final var optLatestPriceDate = stockPrices.keySet().stream().sorted()
                .filter(localDate -> localDate.isBefore(LocalDate.of(2022, 5, 31).plusDays(1)))
                .max(LocalDate::compareTo);
        final var latestPrice = stockPrices.get(optLatestPriceDate.get());
        // E(D0)
        final var optLatestDividendDate = stockDividends.keySet().stream().sorted()
                .filter(localDate -> localDate.isBefore(LocalDate.of(2022, 5, 31).plusDays(1)))
                .max(LocalDate::compareTo);
        final var latestDividend = stockDividends.get(optLatestDividendDate.get());
        // E(P1)
        final var forecastedPrice = latestPrice * (1 + growthRate);
        // E(D1)
        final var forecastedDividends = latestDividend * (1 + growthRate);
        // todo: verify V0=E(D1)+E(P1)/(1+k) VS V0=(E(D1)+E(P1))/(1+k)
        return forecastedDividends + forecastedPrice / (1 + k);
    }

    private static double computeExpectedReturnsOnShare(
            Map<LocalDate, Double> stockPrices,
            Map<LocalDate, Double> stockDividends,
            Double growthRate
    ) {
        // E(P0)
        final var optLatestPriceDate = stockPrices.keySet().stream().sorted()
                .filter(localDate -> localDate.isBefore(LocalDate.of(2022, 5, 31).plusDays(1)))
                .max(LocalDate::compareTo);
        final var latestPrice = stockPrices.get(optLatestPriceDate.get());
        // E(D0)
        final var optLatestDividendDate = stockDividends.keySet().stream().sorted()
                .filter(localDate -> localDate.isBefore(LocalDate.of(2022, 5, 31).plusDays(1)))
                .max(LocalDate::compareTo);
        final var latestDividend = stockDividends.get(optLatestDividendDate.get());
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
