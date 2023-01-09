package com.el;

import com.el.marketdata.LiveCacheRemoteMarketDataRepository;
import com.el.service.AlpacaService;
import com.el.stockselection.EquityScreener;
import net.jacobpeterson.alpaca.rest.AlpacaClientException;
import org.apache.commons.cli.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Set;

public class Application {

  public static void main(String[] args) throws AlpacaClientException {
    Options options = new Options();

    Option db = new Option("d", "database", true, "derby database path");
    options.addOption(db);

    Option dryrun = new Option("t", "dryrun", false, "run without calls to trading API");
    options.addOption(dryrun);

    CommandLineParser parser = new DefaultParser();
    HelpFormatter formatter = new HelpFormatter();
    CommandLine cmd = null;

    try {
      cmd = parser.parse(options, args);
    } catch (ParseException e) {
      System.out.println(e.getMessage());
      formatter.printHelp("fundamental-trading", options);

      System.exit(1);
    }

    run(cmd);
  }

  private static void run(final CommandLine cmd) throws AlpacaClientException {
    final var dbpath = cmd.getOptionValue("database");
    final var marketDataRepository = new LiveCacheRemoteMarketDataRepository(
      dbpath,
      extractSymbols("symbols.txt"),
      ZonedDateTime.of(LocalDate.of(2015, 12, 1), LocalTime.MIDNIGHT, ZoneId.of("America/New_York")).toInstant()
    );

    final var es = new EquityScreener(marketDataRepository);
    final var selection = es.screenEquities();
    final var orp = new OptimalRiskyPortfolio(marketDataRepository, selection);
    final var alpacaService = new AlpacaService();
    final var cash = alpacaService.getCash();
    final var portfolio = orp.calculateWithAdjustment(cash, 0.2);

    if (cmd.hasOption("dryrun")) {
      return;
    }

    alpacaService.atBestEntryPoint(portfolio, () -> {
      alpacaService.sellPortfolio();
      alpacaService.buyPortfolio(portfolio, cash);
    });
  }

  private static Set<String> extractSymbols(final String fileName) {
    final Set<String> symbols = new HashSet<>();
    final var inputStreamReader = new InputStreamReader(getFileFromResourceAsStream(fileName));
    String line;

    try (BufferedReader reader = new BufferedReader(inputStreamReader)) {
      while ((line = reader.readLine()) != null) {
        symbols.add(line);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return symbols;
  }

  private static InputStream getFileFromResourceAsStream(String fileName) {
    ClassLoader classLoader = Application.class.getClassLoader();
    InputStream inputStream = classLoader.getResourceAsStream(fileName);

    if (inputStream == null) {
      throw new IllegalArgumentException("file not found! " + fileName);
    } else {
      return inputStream;
    }
  }
}
