package com.el;

import net.jacobpeterson.alpaca.AlpacaAPI;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.common.historical.bar.enums.BarTimePeriod;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.stock.historical.bar.MultiStockBarsResponse;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.stock.historical.bar.StockBar;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.stock.historical.bar.enums.BarAdjustment;
import net.jacobpeterson.alpaca.rest.AlpacaClientException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AlpacaService {
    
    private final AlpacaAPI alpacaAPI;

    public AlpacaService() {
        alpacaAPI = new AlpacaAPI();
    }

    public Map<String, LinkedHashMap<LocalDate, Double>> getMultiBars(
        Set<String> symbols,
        Instant from,
        Instant to
    ) {
        try {
            final var multiBars = symbols.stream().collect(Collectors.toMap(
                Function.identity(),
                symbol -> new LinkedHashMap<LocalDate, Double>()
            ));
            MultiStockBarsResponse nextPage = null;
            String nextPageToken = null;
            while (nextPage == null || nextPageToken != null) {
                nextPage = alpacaAPI.stockMarketData().getBars(
                    symbols,
                    from.atZone(ZoneId.of("America/New_York")),
                    to.atZone(ZoneId.of("America/New_York")),
                    null,
                    nextPageToken,
                    1,
                    BarTimePeriod.DAY,
                    BarAdjustment.SPLIT,
                    null
                );
                nextPage.getBars().forEach((key, value) -> {
                    final LinkedHashMap<LocalDate, Double> map = value.stream()
                        .collect(Collectors.toMap(
                            bar -> bar.getTimestamp().toLocalDate(),
                            StockBar::getClose,
                            (o1, o2) -> o1,
                            LinkedHashMap::new
                        ));
                    multiBars.get(key).putAll(map);
                });
                nextPageToken = nextPage.getNextPageToken();
            }
            return multiBars;
        } catch (AlpacaClientException e) {
            throw new RuntimeException(e);
        }
    }
}
