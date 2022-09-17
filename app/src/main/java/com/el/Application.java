package com.el;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Algorithm to trade equities of an optimal risky portfolio based on the CAPM... every minute.
 *
 */
public class Application
{

    public static void main( String[] args )
    {
        final var symbolStatisticsRepository = new SymbolStatisticsRepository(LocalDate.of(2019, 9, 5));
        final var es = new EquityScreener(symbolStatisticsRepository);
        final var selection = es.screenEquities();
        final var orp = new OptimalRiskyPortfolio(symbolStatisticsRepository, selection);
        final var optimalAllocation = orp.calculate();
    }
}
