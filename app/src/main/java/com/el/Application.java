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
        final var es = new EquityScreener(symbolStatisticsRepository, LocalDate.of(2022, 5, 31));
        final var res = es.screenEquities();
    }
}
