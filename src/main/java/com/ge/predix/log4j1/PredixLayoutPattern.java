package com.ge.predix.log4j1;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TimeZone;

import org.apache.log4j.helpers.PatternConverter;
import org.apache.log4j.spi.LoggingEvent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

public class PredixLayoutPattern extends PatternConverter {

    public static final SimpleDateFormat ISO_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    private static final ObjectWriter JSON_WRITER = new ObjectMapper().writer();

    public PredixLayoutPattern() {
        ISO_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    @Override
    protected String convert(final LoggingEvent event) {
        // need LinkedHashMap to preserve order of log fields
        Map<String, Object> logFormat = new LinkedHashMap<>();
        logFormat.put("time", ISO_DATE_FORMAT.format(new Date(event.getTimeStamp())));
        logFormat.put("tnt", event.getMDC("Zone-Id"));
        logFormat.put("corr", event.getMDC("X-B3-TraceId"));
        logFormat.put("appn", event.getMDC("APP_NAME"));
        logFormat.put("dpmt", event.getMDC("APP_ID"));
        logFormat.put("inst", event.getMDC("INSTANCE_ID"));
        logFormat.put("tid", event.getThreadName());
        // getLocationInformation() always returns a value so a NullPointerException will not happen
        logFormat.put("mod", event.getLocationInformation().getFileName());
        if (null != event.getLevel()) {
            logFormat.put("lvl", event.getLevel().toString());
        }
        logFormat.put("msg", event.getMessage());
        if (null != event.getThrowableInformation()) {
            String[] stack = event.getThrowableStrRep();
            for(int i = 0; i < stack.length; i++) {
                stack[i] = stack[i].trim();
            }
            logFormat.put("stck", stack);
        }
        try {
            return JSON_WRITER.writeValueAsString(logFormat) + "\n";
        } catch (IOException e) {
            return "Failed to convert log to json for event: "
                    + event.getMessage();
        }
    }
}
