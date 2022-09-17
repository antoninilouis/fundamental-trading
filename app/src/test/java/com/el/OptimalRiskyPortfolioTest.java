package com.el;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.lang.Math.pow;

class OptimalRiskyPortfolioTest {

    // Min: 2019-1-3 max 2022-09-01
    public static final LocalDate TRADE_DATE = LocalDate.of(2019, 1, 3);
    public static final Double STARTING_CAPITAL = 10_000.0;

    @Test
    @Disabled
    public void tryOptimalAllocation()
    {
        final var symbolStatisticsRepository = new SymbolStatisticsRepository(TRADE_DATE);
//        Map<String, Double> allocation;
//        final var es = new EquityScreener(symbolStatisticsRepository);
//        final var selection = es.screenEquities();
//        final var orp = new OptimalRiskyPortfolio(symbolStatisticsRepository, selection, finalExpectedReturn);
//        allocation = orp.calculate();
//        computePortfolioValue(allocation, symbolStatisticsRepository);
        computeDynamicPortfolioValue(symbolStatisticsRepository);
    }

    private double computePortfolioValue(
        Map<String, Double> allocation,
        SymbolStatisticsRepository symbolStatisticsRepository
    ) {
        var portfolioValue = STARTING_CAPITAL;
        final var indexReturns = symbolStatisticsRepository.getNewIndexReturns();
        final var stockReturns = symbolStatisticsRepository.getNewStockReturns(allocation.keySet());
        var valuation = getCopy(allocation);
        for (LocalDate i = TRADE_DATE.plusDays(1); i.isBefore(TRADE_DATE.plusDays(1339)); i = i.plusDays(1)) {

            final LocalDate day = i;
            final var missing = stockReturns.values().stream()
                .filter(returns -> !returns.containsKey(day)).count() + (indexReturns.containsKey(day) ? 0 : 1);
            if (missing > 0 && missing != stockReturns.size() + 1) {
                throw new RuntimeException("Missing or extraneous datapoints");
            }
            if (missing > 0) {
                continue;
            }
            valuation = valuation.entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> {
                        if (entry.getKey().equals(SymbolStatisticsRepository.INDEX_NAME)) {
                            return entry.getValue() * (1.0 + indexReturns.get(day));
                        } else {
                            return entry.getValue() * (1.0 + stockReturns.get(entry.getKey()).get(day));
                        }
                    }
                ));
            final var oldPortfolioValue = portfolioValue;
            portfolioValue = valuation.values().stream().mapToDouble(d -> d * STARTING_CAPITAL).sum();
            System.out.println(allocation.entrySet());
            System.out.printf(
                "Starting capital: %s, Day: %s(%s), Benefit: %.2f$(%.2f$), Rate (y): %.3f%n%n",
                STARTING_CAPITAL,
                ChronoUnit.DAYS.between(TRADE_DATE, day),
                day,
                portfolioValue - STARTING_CAPITAL,
                portfolioValue - oldPortfolioValue,
                100 * ((portfolioValue / oldPortfolioValue) - 1.0)
            );

            symbolStatisticsRepository.increment();
        }
        return portfolioValue;
    }

    private double computeDynamicPortfolioValue(
        SymbolStatisticsRepository symbolStatisticsRepository
    ) {
        final var indexReturns = symbolStatisticsRepository.getNewIndexReturns();
        Double portfolioValue = STARTING_CAPITAL;
        Map<String, java.util.LinkedHashMap<LocalDate, Double>> stockReturns;
        Map<String, Double> allocation;
        Map<String, Double> valuation;

        for (LocalDate i = TRADE_DATE; i.isBefore(TRADE_DATE.plusDays(720)); i = i.plusDays(1)) {
            final LocalDate day = i;

            final var es = new EquityScreener(symbolStatisticsRepository);
            final var selection = es.screenEquities();
            final var orp = new OptimalRiskyPortfolio(symbolStatisticsRepository, selection);
            allocation = orp.calculate();
            stockReturns = symbolStatisticsRepository.getNewStockReturns(allocation.keySet());

            final var missing = stockReturns.values().stream()
                .filter(returns -> !returns.containsKey(day)).count() + (indexReturns.containsKey(day) ? 0 : 1);

            if (missing > 0 && missing != stockReturns.size() + 1) {
                throw new RuntimeException("Missing or extraneous datapoints");
            }
            if (missing > 0) {
                continue;
            }
            final Map<String, LinkedHashMap<LocalDate, Double>> returnCopy = stockReturns;

            valuation = getCopy(allocation);
            valuation = valuation.entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> {
                        if (entry.getKey().equals(SymbolStatisticsRepository.INDEX_NAME)) {
                            return entry.getValue() * (1.0 + indexReturns.get(day));
                        } else {
                            return entry.getValue() * (1.0 + returnCopy.get(entry.getKey()).get(day));
                        }
                    }
                ));

            final var oldPortfolioValue = portfolioValue;
            portfolioValue = valuation.values().stream().mapToDouble(d -> d * oldPortfolioValue).sum();
            System.out.printf(
                "Starting capital: %s, Day: %s(%s), Benefit: %.2f$(%.2f$), Rate (d): %.3f%n",
                STARTING_CAPITAL,
                ChronoUnit.DAYS.between(TRADE_DATE, day),
                day,
                portfolioValue - STARTING_CAPITAL,
                portfolioValue - oldPortfolioValue,
                100 * ((portfolioValue / oldPortfolioValue) - 1.0)
            );

            symbolStatisticsRepository.increment();
        }
        var perf = computePerformance(portfolioValue, 720.0 / 365.0);
        return portfolioValue;
    }

    private static double computePerformance(
            double portfolioValue,
            double time
    ) {
        // r = n[(A/P)1/nt - 1]
        return pow((portfolioValue / OptimalRiskyPortfolioTest.STARTING_CAPITAL), 1.0 / time) - 1.0;
    }

    private static <K, V> Map<K, V> getCopy(Map<K, V> map) {
        return map.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}