package com.el;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.stream.Collectors;

class SingleIndexModelTest {

    @Test
    @Disabled
    public void tryOptimalAllocation()
    {
        final var symbolStatisticsRepository = new SymbolStatisticsRepository();
        final var es = new EquityScreener(symbolStatisticsRepository, LocalDate.of(2019, 9, 5));
        final var selection = es.screenEquities();
        final var sim = new SingleIndexModel(symbolStatisticsRepository, selection);
        final var allocation = sim.getOptimalAllocation();
        computeNextPortfolioValue(allocation, symbolStatisticsRepository, LocalDate.of(2019, 9, 5));
    }

    private void computeNextPortfolioValue(
        Map<String, Double> allocation,
        SymbolStatisticsRepository symbolStatisticsRepository,
        LocalDate startDate
    ) {
        final var startingCapital = 1_000_000.0;
        final var indexReturns = symbolStatisticsRepository.getIndexReturns();
        final var stockReturns = symbolStatisticsRepository.getStockReturns(allocation.keySet());

        var valuation = getCopy(allocation);
        for (LocalDate i = startDate.plusDays(1); i.isBefore(startDate.plusDays(520)); i = i.plusDays(1)) {
            final LocalDate day = i;
            final var missing = stockReturns.values().stream()
                .filter(returns -> !returns.containsKey(day)).count() + (indexReturns.containsKey(day) ? 0 : 1);
            if (missing > 0 && missing != stockReturns.size() + 1) {
                throw new IllegalStateException("Missing or extraneous datapoints");
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
            System.out.printf("Starting capital: %s, Day: %s(%s), Benefit: %s%n",
                startingCapital,
                ChronoUnit.DAYS.between(startDate, day),
                day,
                valuation.values().stream().mapToDouble(d -> d * startingCapital).sum() - startingCapital
            );
        }
    }

    private static <K, V> Map<K, V> getCopy(Map<K, V> map) {
        return map.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}