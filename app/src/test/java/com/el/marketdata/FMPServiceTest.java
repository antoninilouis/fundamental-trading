package com.el.marketdata;

import com.el.service.FMPService;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

class FMPServiceTest {

  @Test
  void getIndexPrice() {
    var res = new FMPService().getIndexPrices("GSPC",
      Instant.now().minus(4, ChronoUnit.DAYS),
      Instant.now().minus(4, ChronoUnit.DAYS)
    );
  }
}