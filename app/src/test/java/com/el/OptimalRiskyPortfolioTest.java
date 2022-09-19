package com.el;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static java.lang.Math.pow;

class OptimalRiskyPortfolioTest {

    public static final LocalDate TRADE_DATE = LocalDate.of(2019, 1, 3);
    public static final Double STARTING_CAPITAL = 10_000.0;

    @Test
    @Disabled
    public void tryOptimalAllocation()
    {
        final var symbolStatisticsRepository = new SymbolStatisticsRepository(TRADE_DATE);
        computePortfolioValue(symbolStatisticsRepository);
    }

    @Test
    @Disabled
    public void tryAlpacaDataAPI()
    {
        final var symbolStatisticsRepository = new SymbolStatisticsRepository(
            TRADE_DATE,
            ZonedDateTime.of(LocalDate.of(2012, 9, 1), LocalTime.MIDNIGHT, ZoneId.of("America/New_York")).toInstant(),
            ZonedDateTime.of(LocalDate.of(2021, 9, 1), LocalTime.MIDNIGHT, ZoneId.of("America/New_York")).toInstant()
        );
        computePortfolioValue(symbolStatisticsRepository);
    }

    private double computePortfolioValue(
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