/*******************************************************************************
 * Copyright 2017 General Electric Company
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/

package com.ge.predix.logback;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.core.encoder.EncoderBase;

public class PredixEncoder<E extends ILoggingEvent> extends EncoderBase<E> {

    private static final SimpleDateFormat ISO_DATE_FORMAT;
    static {
        ISO_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        ISO_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    private static final ObjectWriter JSON_WRITER = new ObjectMapper().writer();

    private static final int INITIAL_BUFFER_SIZE = 1024;
    private static final byte[] EMPTY_BYTES = new byte[0];

    private Pattern messageLineSeparatorPattern = null;

    public void setMessageLineSeparatorRegex(final String messageLineSeparatorRegex) {
        messageLineSeparatorPattern = null;
        if (messageLineSeparatorRegex != null) {
            try {
                messageLineSeparatorPattern = Pattern.compile(messageLineSeparatorRegex);
            } catch (PatternSyntaxException pse) {
                addWarn("Invalid message line separator: " + pse.getMessage());
                addWarn("Log message lines will not be separated.");
            }
        }
    }

    public byte[] headerBytes() {
        return EMPTY_BYTES;
    }

    public byte[] encode(final E event) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream(INITIAL_BUFFER_SIZE)) {
            encode(event, outputStream);
            return outputStream.toByteArray();
        } catch (IOException encodingException) {
            addWarn("Cannot convert log event to json: " + event
                    + System.lineSeparator()
                    + encodingException.getMessage());
        }
        return EMPTY_BYTES;
    }

    public byte[] footerBytes() {
        return EMPTY_BYTES;
    }

    private void encode(final E event, final OutputStream outputStream) throws IOException {

        // Use LinkedHashMap to preserve order of log fields
        Map<String, Object> logFormat = new LinkedHashMap<>();

        logFormat.put("time", ISO_DATE_FORMAT.format(new Date(event.getTimeStamp())));

        Map<String, String> mdc = event.getMDCPropertyMap();
        logFormat.put("tnt", mdc.getOrDefault("Zone-Id", ""));
        logFormat.put("corr", mdc.getOrDefault("X-B3-TraceId", ""));
        logFormat.put("appn", mdc.get("APP_NAME"));
        logFormat.put("dpmt", mdc.getOrDefault("APP_ID", ""));
        logFormat.put("inst", mdc.getOrDefault("INSTANCE_ID", ""));

        logFormat.put("tid", event.getThreadName());
        logFormat.put("mod", event.getLoggerName());
        if (null != event.getLevel()) {
            logFormat.put("lvl", event.getLevel().toString());
        }

        final String message = event.getFormattedMessage();
        if (message != null && messageLineSeparatorPattern != null) {
            logFormat.put("msgLines", messageLineSeparatorPattern.split(message));
        } else {
            logFormat.put("msg", message);
        }

        if (null != event.getThrowableProxy()) {
            logFormat.put("stck", getStackTrace(event));
        }

        JSON_WRITER.writeValue(outputStream, logFormat);
        outputStream.write(System.lineSeparator().getBytes());
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
