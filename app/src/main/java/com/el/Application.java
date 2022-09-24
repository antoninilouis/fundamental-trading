package com.el;

import com.el.marketdata.MarketDataRepository;
import com.el.stockselection.EquityScreener;

import java.time.*;

/**
 * Algorithm to trade equities of an optimal risky portfolio based on the CAPM... every minute.
 *
 */
public class Application
{

    public static void main( String[] args )
    {
        final var symbolStatisticsRepository = new MarketDataRepository(
            LocalDate.of(2020, 9, 1),
            ZonedDateTime.of(LocalDate.of(2013, 9, 1), LocalTime.MIDNIGHT, ZoneId.of("America/New_York")).toInstant(),
            ZonedDateTime.of(LocalDate.of(2021, 9, 1), LocalTime.MIDNIGHT, ZoneId.of("America/New_York")).toInstant()
        );
        final var es = new EquityScreener(symbolStatisticsRepository);
        final var selection = es.screenEquities();
        final var orp = new OptimalRiskyPortfolio(symbolStatisticsRepository, selection);
        final var optimalAllocation = orp.calculate();
    }
}
