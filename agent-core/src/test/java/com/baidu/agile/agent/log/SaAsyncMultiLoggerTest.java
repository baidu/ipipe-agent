package com.baidu.agile.agent.log;

import org.junit.Test;

import static org.junit.Assert.*;

public class SaAsyncMultiLoggerTest {

    private SaAsyncMultiLogger saAsyncMultiLogger = new SaAsyncMultiLogger();

    @Test
    public void start() {
        saAsyncMultiLogger.start();
        saAsyncMultiLogger.end();
    }
}