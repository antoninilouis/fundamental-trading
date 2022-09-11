package com.el;

import java.util.List;

/**
 * Algorithm to trade equities of an optimal risky portfolio based on the CAPM... every minute.
 *
 */
public class Application
{

    public static void main( String[] args )
    {
        final var symbolStatisticsRepository = new SymbolStatisticsRepository();
        final var es = new EquityScreener(symbolStatisticsRepository);
        final var res = es.screenEquities(List.of("MSFT"));
    }
}
