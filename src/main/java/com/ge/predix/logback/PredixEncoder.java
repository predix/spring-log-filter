package com.ge.predix.logback;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.apache.commons.lang.StringUtils;

import ch.qos.logback.classic.pattern.FileOfCallerConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.core.encoder.EncoderBase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

public class PredixEncoder<E extends ILoggingEvent> extends EncoderBase<E> {

    public static final SimpleDateFormat ISO_DATE_FORMAT;
    static {
        ISO_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        ISO_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    private static final FileOfCallerConverter FILE_OF_CALLER_CONVERTER = new FileOfCallerConverter();
    private static final ObjectWriter JSON_WRITER = new ObjectMapper().writer();

    @Override
    public void doEncode(final E event) throws IOException {
        Map<String, String> mdc = event.getMDCPropertyMap();

        // need LinkedHashMap to preserve order of log fields
        Map<String, Object> logFormat = new LinkedHashMap<>();
        logFormat.put("time", ISO_DATE_FORMAT.format(new Date(event.getTimeStamp())));
        logFormat.put("tnt", mdc.getOrDefault("Zone-Id", ""));
        logFormat.put("corr", mdc.getOrDefault("X-B3-TraceId", ""));
        logFormat.put("appn", mdc.get("APP_NAME"));
        logFormat.put("dpmt", mdc.getOrDefault("APP_ID", ""));
        logFormat.put("inst", mdc.getOrDefault("INSTANCE_ID", ""));
        logFormat.put("tid", event.getThreadName());
        logFormat.put("mod", FILE_OF_CALLER_CONVERTER.convert(event));
        if (null != event.getLevel()) {
            logFormat.put("lvl", event.getLevel().toString());
        }
        logFormat.put("msg", event.getMessage());
        if (null != event.getThrowableProxy()) {
            logFormat.put("stck", getStackTrace(event));
        }
        try {
            JSON_WRITER.writeValue(this.outputStream, logFormat);
        } catch (IOException e) {
            JSON_WRITER.writeValue(this.outputStream, "Failed to convert log to json for event: " + event.getMessage());
        }
        this.outputStream.write(System.lineSeparator().getBytes());
    }

    @Override
    public void close() throws IOException {
        // do nothing
    }

    private List<List<String>> getStackTrace(final E event) {
        List<List<String>> exceptions = new ArrayList<>();
        IThrowableProxy throwable = event.getThrowableProxy();
        while (throwable != null) {
            List<String> stack = new ArrayList<>();
            stack.add(throwable.getClassName()
                    + (!StringUtils.isEmpty(throwable.getMessage()) ? ": " + throwable.getMessage() : ""));
            for (StackTraceElementProxy element : throwable.getStackTraceElementProxyArray()) {
                stack.add(element.toString());
            }
            exceptions.add(stack);
            throwable = throwable.getCause();
        }

        return exceptions;
    }

}
