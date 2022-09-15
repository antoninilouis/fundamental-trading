package com.el;

import java.time.LocalDate;

/**
 * Algorithm to trade equities of an optimal risky portfolio based on the CAPM... every minute.
 *
 */
public class Application
{

    public static void main( String[] args )
    {
        final var symbolStatisticsRepository = new SymbolStatisticsRepository();
        final var es = new EquityScreener(symbolStatisticsRepository, LocalDate.of(2022, 5, 5));
        final var selection = es.screenEquities();
        final var sim = new SingleIndexModel(symbolStatisticsRepository, selection);
        final var optimalAllocation = sim.getOptimalAllocation();
    }
}
