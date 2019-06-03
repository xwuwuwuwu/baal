package com.battlelab;

import java.util.logging.Level;
import java.util.logging.Logger;

public class TraceHelper {

    private Logger logger;

    public TraceHelper(Logger logger) {
        this.logger = logger;
    }

    public void trace(String message) {
        //this.logger.log(Level.FINE, message);
        //System.out.println(message);
    }

    public void info(String message) {
        this.logger.log(Level.WARNING, message);
    }

    public void error(String message, Throwable e) {
        this.logger.log(Level.SEVERE, message, e);
    }

    public void warning(String message) {
        this.logger.log(Level.WARNING, message);
    }
}
