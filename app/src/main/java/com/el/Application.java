package com.el;

import com.el.marketdata.LiveCacheRemoteMarketDataRepository;
import com.el.service.AlpacaService;
import com.el.servlets.HealthCheckServlet;
import com.el.servlets.ReallocateServlet;
import com.el.stockselection.EquityScreener;
import jakarta.servlet.DispatcherType;
import net.jacobpeterson.alpaca.rest.AlpacaClientException;
import org.apache.commons.cli.*;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

public class Application {
  final private Logger logger = LoggerFactory.getLogger(Application.class);

  public static void main(String[] args) throws AlpacaClientException {
    Options options = new Options();

    Option db = new Option("d", "database", true, "derby database path");
    options.addOption(db);

    Option server = new Option("s", "server", false, "run in server mode");
    options.addOption(server);

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

    try {
      run(cmd);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static void run(CommandLine cmd) throws Exception {
    final var dbpath = cmd.getOptionValue("database");

    if (cmd.hasOption("dryrun")) {
      runOnce(dbpath);
    }

    if (cmd.hasOption("server")) {
      startServer(dbpath);
    }
  }

  private static void startServer(String dbpath) throws Exception {
    final var server = new Server();
    try (ServerConnector connector = new ServerConnector(server)) {
      connector.setPort(5000);
      server.setConnectors(new Connector[]{connector});

      ServletContextHandler handler = new ServletContextHandler();
      handler.addServlet(HealthCheckServlet.class, "/");
      final var runServlet = new ReallocateServlet(dbpath);
      handler.addServlet(new ServletHolder(runServlet), "/reallocate");

      FilterHolder filterHolder = handler.addFilter(CrossOriginFilter.class, "/*", EnumSet.of(DispatcherType.REQUEST));
      filterHolder.setInitParameter("allowedOrigins", "*");

      server.setHandler(handler);
      server.start();
      server.join();
    }
  }

  public static void runOnce(final String dbpath) throws AlpacaClientException {
    final var marketDataRepository = new LiveCacheRemoteMarketDataRepository(
      dbpath,
      extractSymbols("symbols.txt"),
      ZonedDateTime.of(LocalDate.of(2015, 12, 1), LocalTime.MIDNIGHT, ZoneId.of("America/New_York")).toInstant()
    );

    final var es = new EquityScreener(marketDataRepository);
    final var selection = es.screenEquities();
    final var orp = new OptimalRiskyPortfolio(marketDataRepository, selection);
    final var alpacaService = new AlpacaService();
    alpacaService.sellPortfolio();

    final var cash = alpacaService.getCash();
    final var portfolio = orp.calculateWithAdjustment(cash, 0.2);
    alpacaService.buyPortfolio(portfolio, cash);
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
