package com.ge.predix.log4j1;

import org.apache.log4j.Layout;
import org.apache.log4j.spi.LoggingEvent;

public class PredixLayout extends Layout {

    private final PredixLayoutPattern predixLayoutPattern = new PredixLayoutPattern();

    private final boolean handlesExceptions;

    public PredixLayout() {
        this.handlesExceptions = false;
    }

    @Override
    public String format(final LoggingEvent event) {
        return this.predixLayoutPattern.convert(event);
    }

    @Override
    public void activateOptions() {
        /**/
    }

    @Override
    public boolean ignoresThrowable() {
        return !this.handlesExceptions;
    }
}