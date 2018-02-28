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
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import org.testng.Assert;
import org.testng.annotations.Test;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;

public class PredixEncoderTest {

    private static final SimpleDateFormat ISO_DATE_FORMAT;
    static {
        ISO_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        ISO_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    private static final String CLASS_NAME = "com.example.math.Geometry";
    private static final String METHOD_NAME = "calculateVolume";
    private static final String THREAD_NAME = "Thread1";

    private static final String NOT_AVAILABLE_FILE_NAME = null;
    private static final int NOT_AVAILABLE_LINE_NUMBER = -1;

    private static final String ZONE_VALUE = "test-zone";
    private static final String CORRELATION_VALUE = "5678";
    private static final String APP_NAME_VALUE = "uaa";
    private static final String APP_ID_VALUE = "098877475";
    private static final String INSTANCE_ID_VALUE = "6758302";
    private static final String INSTANCE_INDEX_VALUE = "5";
    private static final Map<String, String> MDC;
    static {
        MDC = new HashMap<>();
        MDC.put("Zone-Id", ZONE_VALUE);
        MDC.put("X-B3-TraceId", CORRELATION_VALUE);
        MDC.put("APP_NAME", APP_NAME_VALUE);
        MDC.put("APP_ID", APP_ID_VALUE);
        MDC.put("INSTANCE_ID", INSTANCE_ID_VALUE);
        MDC.put("INSTANCE_INDEX", INSTANCE_INDEX_VALUE);
    }

    private static final Throwable THROWABLE;
    static {
        THROWABLE = new NullPointerException();
        THROWABLE.setStackTrace(new StackTraceElement[] {
                new StackTraceElement("com.ge.predix.some.Class", "method", "Class.java", 234),
                new StackTraceElement("com.ge.predix.some.other.OtherClass", "otherMethod", "OtherClass.java", 45) });
    }

    private static final String MESSAGE_TEXT = "{length=5, width=4, height=3, units=inches}";
    private static final String MESSAGE_FORMAT = "{length={}, width={}, height={}, units={}}";
    private static final Object[] MESSAGE_ARGS = { 5, 4, 3, "inches" };
    private static final Object[] MESSAGE_ARGS_WITH_THROWABLE = { 5, 4, 3, "inches", THROWABLE };
    private static final String MESSAGE_OUTPUT = "\"" + MESSAGE_TEXT + "\"";

    private static final String MULTI_LINE_MESSAGE_FORMAT = "L1\nL2" + System.lineSeparator() + "L3\n{}"
            + System.lineSeparator() + "L5{}L6{}L7\n{}";
    private static final Object[] MULTI_LINE_MESSAGE_ARGS = { "L4", "\n", System.lineSeparator(),
            "L8\nL9" + System.lineSeparator() + "L10" };

    @Test
    public void testPredixEncoderWithRegularMessage() throws IOException {

        ILoggingEvent input = createLogEvent(MESSAGE_TEXT, null, null);

        String expected = getExpectedOutput(input.getTimeStamp(), MESSAGE_OUTPUT, null);

        String actual = encodeToPredixFormat(input);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testPredixEncoderWithSpecialChars() throws IOException {

        ILoggingEvent input = createLogEvent("\"{}\n,\"\\", null, null);

        String expected = getExpectedOutput(input.getTimeStamp(), "\"\\\"{}\\n,\\\"\\\\\"", null);

        String actual = encodeToPredixFormat(input);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testPredixEncoderWithException() throws IOException {

        ILoggingEvent input = createLogEvent(MESSAGE_TEXT, null, THROWABLE);

        String expected = getExpectedOutput(input.getTimeStamp(), MESSAGE_OUTPUT,
                "[[\"java.lang.NullPointerException\"," + "\"at com.ge.predix.some.Class.method(Class.java:234)\","
                        + "\"at com.ge.predix.some.other.OtherClass.otherMethod(OtherClass.java:45)\"]]");

        String actual = encodeToPredixFormat(input);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testPredixEncoderWithExceptionChain() throws IOException {

        Throwable exceptionRoot = new Exception(THROWABLE);
        exceptionRoot.setStackTrace(new StackTraceElement[] {
                new StackTraceElement("com.ge.predix.some.Clazz", "method", "Clazz.java", 473),
                new StackTraceElement("com.ge.predix.some.other.OtherClazz", "otherMethod", "OtherClazz.java", 55) });
        ILoggingEvent input = createLogEvent(MESSAGE_TEXT, null, exceptionRoot);

        String expected = getExpectedOutput(input.getTimeStamp(), MESSAGE_OUTPUT,
                "[[\"java.lang.Exception: java.lang.NullPointerException\","
                        + "\"at com.ge.predix.some.Clazz.method(Clazz.java:473)\","
                        + "\"at com.ge.predix.some.other.OtherClazz.otherMethod(OtherClazz.java:55)\"],"
                        + "[\"java.lang.NullPointerException\","
                        + "\"at com.ge.predix.some.Class.method(Class.java:234)\","
                        + "\"at com.ge.predix.some.other.OtherClass.otherMethod(OtherClass.java:45)\"]]");

        String actual = encodeToPredixFormat(input);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testPredixEncoderWithImplicitException() throws IOException {

        ILoggingEvent input = createLogEvent(MESSAGE_TEXT, new Object[] { THROWABLE }, null);

        String expected = getExpectedOutput(input.getTimeStamp(), MESSAGE_OUTPUT,
                "[[\"java.lang.NullPointerException\"," + "\"at com.ge.predix.some.Class.method(Class.java:234)\","
                        + "\"at com.ge.predix.some.other.OtherClass.otherMethod(OtherClass.java:45)\"]]");

        String actual = encodeToPredixFormat(input);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testPredixEncoderWithVariables() throws IOException {

        ILoggingEvent input = createLogEvent(MESSAGE_FORMAT, MESSAGE_ARGS, null);

        String expected = getExpectedOutput(input.getTimeStamp(), MESSAGE_OUTPUT, null);

        String actual = encodeToPredixFormat(input);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testPredixEncoderWithVariablesAndException() throws IOException {

        ILoggingEvent input = createLogEvent(MESSAGE_FORMAT, MESSAGE_ARGS, THROWABLE);

        String expected = getExpectedOutput(input.getTimeStamp(), MESSAGE_OUTPUT,
                "[[\"java.lang.NullPointerException\"," + "\"at com.ge.predix.some.Class.method(Class.java:234)\","
                        + "\"at com.ge.predix.some.other.OtherClass.otherMethod(OtherClass.java:45)\"]]");

        String actual = encodeToPredixFormat(input);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testPredixEncoderWithVariablesAndImplicitException() throws IOException {

        ILoggingEvent input = createLogEvent(MESSAGE_FORMAT, MESSAGE_ARGS_WITH_THROWABLE, null);

        String expected = getExpectedOutput(input.getTimeStamp(), MESSAGE_OUTPUT,
                "[[\"java.lang.NullPointerException\"," + "\"at com.ge.predix.some.Class.method(Class.java:234)\","
                        + "\"at com.ge.predix.some.other.OtherClass.otherMethod(OtherClass.java:45)\"]]");

        String actual = encodeToPredixFormat(input);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testPredixEncoderWithMissingInfo() throws IOException {

        Logger logger = new LoggerContext().getLogger(CLASS_NAME);
        LoggingEvent input = new LoggingEvent(null, logger, null, null, null, null);
        input.setTimeStamp(Instant.now().toEpochMilli());
        input.setThreadName(THREAD_NAME);
        input.setCallerData(null);
        input.setMDCPropertyMap(MDC);

        String expectedTimestamp = ISO_DATE_FORMAT.format(new Date(input.getTimeStamp()));
        String expected = "{\"time\":\"" + expectedTimestamp + "\",\"tnt\":\"" + ZONE_VALUE + "\",\"corr\":\""
                + CORRELATION_VALUE + "\",\"appn\":\"" + APP_NAME_VALUE + "\",\"dpmt\":\"" + APP_ID_VALUE
                + "\",\"inst\":\"" + INSTANCE_ID_VALUE + "\",\"tid\":\"" + THREAD_NAME + "\",\"mod\":\"" + CLASS_NAME
                + "\",\"msg\":null}";

        String actual = encodeToPredixFormat(input);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testPredixEncoderWithNoMessageLineSeparator() throws IOException {

        ILoggingEvent input = createLogEvent(MULTI_LINE_MESSAGE_FORMAT, MULTI_LINE_MESSAGE_ARGS, null);

        String expected = getExpectedOutput(input.getTimeStamp(),
                "\"L1\\nL2\\nL3\\nL4\\nL5\\nL6\\nL7\\nL8\\nL9\\nL10\"", null);

        String actual = encodeToPredixFormat(input);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testPredixEncoderWithInvalidMessageLineSeparator() throws IOException {

        ILoggingEvent input = createLogEvent(MULTI_LINE_MESSAGE_FORMAT, MULTI_LINE_MESSAGE_ARGS, null);

        // If an invalid regex is detected, the encoder will switch it off.
        String expected = getExpectedOutput(input.getTimeStamp(),
                "\"L1\\nL2\\nL3\\nL4\\nL5\\nL6\\nL7\\nL8\\nL9\\nL10\"", null);

        String actual = encodeToPredixFormat(input, "("); // Malformed regex
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testPredixEncoderWithSimpleMessageLineSeparator() throws IOException {

        ILoggingEvent input = createLogEvent(MULTI_LINE_MESSAGE_FORMAT, MULTI_LINE_MESSAGE_ARGS, null);

        String expected = getExpectedOutput(input.getTimeStamp(),
                "[\"L1\",\"L2\",\"L3\",\"L4\",\"L5\",\"L6\",\"L7\",\"L8\",\"L9\",\"L10\"]", true, null);

        String actual = encodeToPredixFormat(input, System.lineSeparator());
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testPredixEncoderWithRegexMessageLineSeparator() throws IOException {

        ILoggingEvent input = createLogEvent(MULTI_LINE_MESSAGE_FORMAT, MULTI_LINE_MESSAGE_ARGS, null);

        String expected = getExpectedOutput(input.getTimeStamp(),
                "[\"L\",\"L\",\"L\",\"L\",\"L\",\"L\",\"L\",\"L\",\"L\",\"L\"]", true, null);

        String actual = encodeToPredixFormat(input, "[0-9]+\n?");
        Assert.assertEquals(expected, actual);
    }

    private static ILoggingEvent createLogEvent(final String message, final Object[] messageArgs,
            final Throwable throwable) {

        Logger logger = new LoggerContext().getLogger(CLASS_NAME);
        LoggingEvent logEvent = new LoggingEvent(null, logger, Level.INFO, message, throwable, messageArgs);
        logEvent.setTimeStamp(Instant.now().toEpochMilli());
        logEvent.setThreadName(THREAD_NAME);
        logEvent.setCallerData(new StackTraceElement[] {
                new StackTraceElement(CLASS_NAME, METHOD_NAME, NOT_AVAILABLE_FILE_NAME, NOT_AVAILABLE_LINE_NUMBER) });
        logEvent.setMDCPropertyMap(MDC);
        return logEvent;
    }

    private String encodeToPredixFormat(final ILoggingEvent logEvent) throws IOException {

        return encodeToPredixFormat(logEvent, null);
    }

    private String encodeToPredixFormat(final ILoggingEvent logEvent, final String messageLineSeparator)
            throws IOException {

        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream()) {
            try (PrintStream printStream = new PrintStream(byteStream)) {
                PredixEncoder<ILoggingEvent> predixEncoder = new PredixEncoder<>();
                predixEncoder.setMessageLineSeparatorRegex(messageLineSeparator);
                predixEncoder.init(printStream);
                predixEncoder.doEncode(logEvent);
                return byteStream.toString();
            }
        }
    }

    private static String getExpectedOutput(final long timestamp, final String message, final String stack) {

        return getExpectedOutput(timestamp, message, false, stack);
    }

    private static String getExpectedOutput(final long timestamp, final String message, final boolean multiLine,
            final String stack) {

        String expectedOutput = "{\"time\":\"" + ISO_DATE_FORMAT.format(new Date(timestamp)) + "\",\"tnt\":\""
                + ZONE_VALUE + "\",\"corr\":\"" + CORRELATION_VALUE + "\",\"appn\":\"" + APP_NAME_VALUE
                + "\",\"dpmt\":\"" + APP_ID_VALUE + "\",\"inst\":\"" + INSTANCE_ID_VALUE + "\",\"tid\":\"" + THREAD_NAME
                + "\",\"mod\":\"" + CLASS_NAME + "\",\"lvl\":\"" + Level.INFO + "\"";
        if (multiLine) {
            expectedOutput += ",\"msgLines\":" + message;
        } else {
            expectedOutput += ",\"msg\":" + message;
        }
        if (stack != null) {
            expectedOutput += ",\"stck\":" + stack;
        }
        expectedOutput += "}";
        return expectedOutput;
    }
}
