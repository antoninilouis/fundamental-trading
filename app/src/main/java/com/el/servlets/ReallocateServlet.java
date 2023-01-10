package com.el.servlets;

import com.el.Application;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.jacobpeterson.alpaca.rest.AlpacaClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalTime;
import java.time.ZoneId;

public class ReallocateServlet extends HttpServlet {
  final private Logger logger = LoggerFactory.getLogger(ReallocateServlet.class);

  private final String dbpath;

  public ReallocateServlet(final String dbpath) {
    this.dbpath = dbpath;
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    try {
      if (!insideTradingHours()) {
        logger.info("Request outside trading hours, aborting");
        response.getWriter().println("Request outside trading hours. The NYSE is open from Monday through Friday 9:30 a.m. to 4:00 p.m. Eastern time.");
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        return;
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    try {
      logger.info("Starting reallocation");
      Application.runOnce(dbpath);
      logger.info("Reallocation succeeded");
      response.setStatus(HttpServletResponse.SC_OK);
    } catch (AlpacaClientException e) {
      logger.info("Reallocation failed", e);
      throw new RuntimeException(e);
    }
  }

  private boolean insideTradingHours() {
    // fail if the stock market is not open
    final LocalTime now = LocalTime.now(ZoneId.of("America/New_York"));
    return now.isAfter(LocalTime.of(9, 30)) && now.isBefore(LocalTime.of(16, 0));
  }
}