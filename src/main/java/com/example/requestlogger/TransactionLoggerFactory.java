package com.example.requestlogger;

import org.slf4j.Logger;

interface TransactionLoggerFactory {
    Logger getLogger();
}
