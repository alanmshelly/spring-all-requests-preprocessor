package com.example.requestlogger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
class TransactionLoggerFactoryImpl implements TransactionLoggerFactory {
    final static private Logger log = LoggerFactory.getLogger("TransactionLogger");

    public Logger getLogger() {
        return log;
    }
}
