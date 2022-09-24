package com.el.marketdata;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FMPServiceTest {

    @Test
    void get() {
        new FMPService().loadApiKey();
    }
}