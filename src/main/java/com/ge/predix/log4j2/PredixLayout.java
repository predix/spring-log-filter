/*******************************************************************************
 * Copyright 2020 General Electric Company
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

package com.ge.predix.log4j2;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.AbstractStringLayout;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.ObjectMessage;
import org.apache.logging.log4j.status.StatusLogger;
import org.springframework.util.StringUtils;

@Plugin(name = "PredixLayout", category = Core.CATEGORY_NAME, elementType = Layout.ELEMENT_TYPE)
public final class PredixLayout extends AbstractStringLayout {

    private static final StatusLogger STATUS_LOGGER = StatusLogger.getLogger();

    private static final SimpleDateFormat ISO_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    static {
        ISO_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    private static final ObjectWriter JSON_WRITER = new ObjectMapper().writer();

    private final Pattern messageLineSeparatorPattern;

    private PredixLayout(final Pattern messageLineSeparatorPattern) {
        super(StandardCharsets.UTF_8);
        this.messageLineSeparatorPattern = messageLineSeparatorPattern;
    }

    @PluginFactory
    public static PredixLayout createLayout(
            @PluginAttribute("messageLineSeparatorRegex") final String messageLineSeparatorRegex) {

        Pattern messageLineSeparatorRegexPattern = null;
        if (messageLineSeparatorRegex != null) {
            try {
                messageLineSeparatorRegexPattern = Pattern.compile(messageLineSeparatorRegex);
            } catch (PatternSyntaxException pse) {
                STATUS_LOGGER.warn("Invalid message line separator: " + pse.getMessage());
                STATUS_LOGGER.warn("Log message lines will not be separated.");
            }
        }
        return new PredixLayout(messageLineSeparatorRegexPattern);
    }

    @Override
    public String toSerializable(final LogEvent event) {
        // need LinkedHashMap to preserve order of log fields
        Map<String, Object> logFormat = new LinkedHashMap<>();

        logFormat.put("time", ISO_DATE_FORMAT.format(new Date(event.getTimeMillis())));
        logFormat.put("tnt", event.getContextData().getValue("Zone-Id"));
        logFormat.put("corr", event.getContextData().getValue("X-B3-TraceId"));
        logFormat.put("appn", event.getContextData().getValue("APP_NAME"));
        logFormat.put("dpmt", event.getContextData().getValue("APP_ID"));
        logFormat.put("inst", event.getContextData().getValue("INSTANCE_ID"));
        logFormat.put("tid", event.getThreadName());
        logFormat.put("mod", event.getLoggerName());
        if (event.getLevel() != null && event.getLevel() != Level.OFF) {
            logFormat.put("lvl", event.getLevel().toString());
        }
        final Message message = event.getMessage();
        if (message == null) {
            logFormat.put("msg", null);
        } else if (message instanceof ObjectMessage) {
            logFormat.put("msg", message.getParameters()[0]);
        } else if (messageLineSeparatorPattern == null) {
            logFormat.put("msg", message.getFormattedMessage());
        } else {
            logFormat.put("msgLines", messageLineSeparatorPattern.split(message.getFormattedMessage()));
        }
        if (null != event.getThrown()) {
            logFormat.put("stck", getStackTrace(event));
        }
        try {
            return JSON_WRITER.writeValueAsString(logFormat) + "\n";
        } catch (IOException e) {
            return "Failed to convert log to json for event: " + event.getMessage();
        }
    }

    private List<List<String>> getStackTrace(final LogEvent event) {
        List<List<String>> exceptions = new ArrayList<>();
        Throwable throwable = event.getThrown();
        while (throwable != null) {
            List<String> stack = new ArrayList<>();
            stack.add(throwable.getClass().getName()
                    + (!StringUtils.isEmpty(throwable.getMessage()) ? ": " + throwable.getMessage() : ""));
            for (StackTraceElement element : throwable.getStackTrace()) {
                stack.add("at " + element.toString());
            }
            exceptions.add(stack);
            throwable = throwable.getCause();
        }

        return exceptions;
    }
}
