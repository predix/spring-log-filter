/*******************************************************************************
 * Copyright 2021 General Electric Company
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

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.JdkMapAdapterStringMap;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.SimpleMessageFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

public class PredixLayoutTest {

    private static final SimpleDateFormat ISO_DATE_FORMAT;
    static {
        ISO_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        ISO_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    private static final String CORRELATION_KEY = "traceId";
    private static final String CORRELATION_KEY_OTHER = CORRELATION_KEY + "-Other";
    private static final String CORRELATION_VALUE = "5678";
    private static final String CORRELATION_VALUE_OTHER = CORRELATION_VALUE + "-Other";

    private static final String APP_ID = "APP_ID";
    private static final String APP_NAME = "APP_NAME";
    private static final String INSTANCE_ID = "INSTANCE_ID";
    private static final String INSTANCE_INDEX = "INSTANCE_INDEX";
    private static final String INSTANCE_ID_VALUE = "6758302";
    private static final String ZONE_VALUE = "test-zone";
    private static final String INSTANCE_INDEX_VALUE = "5";
    private static final String APP_NAME_VALUE = "uaa";
    private static final String APP_ID_VALUE = "098877475";
    private static final String LOGGER_NAME = "com.ge.predix.TestLogger";
    private static final String THREAD_NAME = "Thread1";
    private static final String ZONE_HEADER = "Zone-Id";

    private static final Message OBJECT_MESSAGE = SimpleMessageFactory.INSTANCE.newMessage(buildObjectMessage());
    private static final Message MULTI_LINE_TEXT_MESSAGE = SimpleMessageFactory.INSTANCE.newMessage(
            "L1\nL2" + System.lineSeparator() + "L3");

    private final PredixLayout predixLayout = PredixLayout.createLayout(null, null);

    @Test
    public void testPredixLayoutRegularLog() {
        long timeStamp = Instant.now().toEpochMilli();
        String expectedTimeStamp = ISO_DATE_FORMAT.format(new Date(timeStamp));
        HashMap<String, String> mdc = getMDC();
        LogEvent logEvent = Log4jLogEvent.newBuilder()
                .setLoggerName(LOGGER_NAME)
                .setTimeMillis(timeStamp)
                .setLevel(Level.INFO)
                .setMessage(OBJECT_MESSAGE)
                .setThreadName(THREAD_NAME)
                .setContextData(new JdkMapAdapterStringMap(mdc))
                .build();
        String actual = predixLayout.toSerializable(logEvent);
        String expected = "{\"time\":\"" + expectedTimeStamp + "\",\"tnt\":\"" + ZONE_VALUE + "\",\"corr\":\""
                + CORRELATION_VALUE + "\",\"appn\":\"" + APP_NAME_VALUE + "\",\"dpmt\":\"" + APP_ID_VALUE
                + "\",\"inst\":\"" + INSTANCE_ID_VALUE + "\",\"tid\":\"" + THREAD_NAME + "\",\"mod\":\"" + LOGGER_NAME
                + "\",\"lvl\":\"" + Level.INFO.toString()
                + "\",\"msg\":{\"width\":4,\"length\":3,\"units\":\"inches\",\"height\":5}}\n";
        System.out.println(actual);
        Assert.assertEquals(actual, expected);
    }

    @Test
    public void testPredixLayoutSpecialCharsLog() {
        long timeStamp = Instant.now().toEpochMilli();
        String expectedTimeStamp = ISO_DATE_FORMAT.format(new Date(timeStamp));
        HashMap<String, String> mdc = getMDC();
        HashMap<String, Object> msg = new HashMap<>();
        msg.put("quote", '"');
        msg.put("backslash", (char) 92);
        LogEvent logEvent = Log4jLogEvent.newBuilder()
                .setLoggerName(LOGGER_NAME)
                .setTimeMillis(timeStamp)
                .setLevel(Level.INFO)
                .setMessage(SimpleMessageFactory.INSTANCE.newMessage(msg))
                .setThreadName(THREAD_NAME)
                .setContextData(new JdkMapAdapterStringMap(mdc))
                .build();
        String actual = predixLayout.toSerializable(logEvent);
        String expected = "{\"time\":\"" + expectedTimeStamp + "\",\"tnt\":\"" + ZONE_VALUE + "\",\"corr\":\""
                + CORRELATION_VALUE + "\",\"appn\":\"" + APP_NAME_VALUE + "\",\"dpmt\":\"" + APP_ID_VALUE
                + "\",\"inst\":\"" + INSTANCE_ID_VALUE + "\",\"tid\":\"" + THREAD_NAME + "\",\"mod\":\"" + LOGGER_NAME
                + "\",\"lvl\":\"" + Level.INFO.toString() + "\",\"msg\":{\"quote\":\"\\\"\",\"backslash\":\"\\\\\"}}\n";
        Assert.assertEquals(actual, expected);
    }

    @Test
    public void testPredixLayoutExceptionLog() {
        long timeStamp = Instant.now().toEpochMilli();
        String expectedTimeStamp = ISO_DATE_FORMAT.format(new Date(timeStamp));
        HashMap<String, String> mdc = getMDC();
        Throwable exceptionThrowable = new Exception();
        exceptionThrowable.setStackTrace(new StackTraceElement[] {
                new StackTraceElement("com.ge.predix.some.package.Class", "method", "Class.java", 234),
                new StackTraceElement("com.ge.predix.some.other.package.OtherClass", "diffMethod", "OtherClass.java",
                        45) });

        LogEvent logEvent = Log4jLogEvent.newBuilder()
                .setLoggerName(LOGGER_NAME)
                .setTimeMillis(timeStamp)
                .setLevel(Level.ERROR)
                .setMessage(OBJECT_MESSAGE)
                .setThreadName(THREAD_NAME)
                .setContextData(new JdkMapAdapterStringMap(mdc))
                .setThrown(exceptionThrowable)
                .build();
        String actual = predixLayout.toSerializable(logEvent);
        String expected = "{\"time\":\"" + expectedTimeStamp + "\",\"tnt\":\"" + ZONE_VALUE + "\",\"corr\":\""
                + CORRELATION_VALUE + "\",\"appn\":\"" + APP_NAME_VALUE + "\",\"dpmt\":\"" + APP_ID_VALUE
                + "\",\"inst\":\"" + INSTANCE_ID_VALUE + "\",\"tid\":\"" + THREAD_NAME + "\",\"mod\":\"" + LOGGER_NAME
                + "\",\"lvl\":\"" + Level.ERROR.toString()
                + "\",\"msg\":{\"width\":4,\"length\":3,\"units\":\"inches\",\"height\":5},\"stck\":"
                + "[[\"java.lang.Exception\",\"at com.ge.predix.some.package.Class.method(Class.java:234)\",\""
                + "at com.ge.predix.some.other.package.OtherClass.diffMethod(OtherClass.java:45)\"]]}\n";
        Assert.assertEquals(actual, expected);
        // check that a logEvent without a stack trace is not polluted from previous logEvent with a stack trace.
        logEvent = Log4jLogEvent.newBuilder()
                .setLoggerName(LOGGER_NAME)
                .setTimeMillis(timeStamp)
                .setLevel(Level.INFO)
                .setMessage(OBJECT_MESSAGE)
                .setThreadName(THREAD_NAME)
                .setContextData(new JdkMapAdapterStringMap(mdc))
                .build();
        actual = predixLayout.toSerializable(logEvent);
        expected = "{\"time\":\"" + expectedTimeStamp + "\",\"tnt\":\"" + ZONE_VALUE + "\",\"corr\":\""
                + CORRELATION_VALUE + "\",\"appn\":\"" + APP_NAME_VALUE + "\",\"dpmt\":\"" + APP_ID_VALUE
                + "\",\"inst\":\"" + INSTANCE_ID_VALUE + "\",\"tid\":\"" + THREAD_NAME + "\",\"mod\":\"" + LOGGER_NAME
                + "\",\"lvl\":\"" + Level.INFO.toString()
                + "\",\"msg\":{\"width\":4,\"length\":3,\"units\":\"inches\",\"height\":5}}\n";
        Assert.assertEquals(actual, expected);
    }

    @Test
    public void testPredixLayoutExceptionChainLog() {

        long timeStamp = Instant.now().toEpochMilli();
        String expectedTimeStamp = ISO_DATE_FORMAT.format(new Date(timeStamp));
        HashMap<String, String> mdc = getMDC();
        Throwable exceptionCause = new NullPointerException("example NullPointerException");
        exceptionCause.setStackTrace(new StackTraceElement[] {
                new StackTraceElement("com.ge.predix.some.package.Class", "method", "Class.java", 234),
                new StackTraceElement("com.ge.predix.some.other.package.OtherClass", "diffMethod", "OtherClass.java",
                        45) });
        Throwable exceptionRoot = new Exception(exceptionCause);
        exceptionRoot.setStackTrace(new StackTraceElement[] {
                new StackTraceElement("com.ge.predix.some.package.Clazz", "method", "Clazz.java", 473),
                new StackTraceElement("com.ge.predix.some.other.package.OtherClazz", "diffMethod", "OtherClazz.java",
                        55) });

        LogEvent logEvent = Log4jLogEvent.newBuilder()
                .setLoggerName(LOGGER_NAME)
                .setTimeMillis(timeStamp)
                .setLevel(Level.ERROR)
                .setMessage(OBJECT_MESSAGE)
                .setThreadName(THREAD_NAME)
                .setContextData(new JdkMapAdapterStringMap(mdc))
                .setThrown(exceptionRoot)
                .build();
        String actual = predixLayout.toSerializable(logEvent);

        String expected = "{\"time\":\"" + expectedTimeStamp + "\",\"tnt\":\"" + ZONE_VALUE + "\",\"corr\":\""
                + CORRELATION_VALUE + "\",\"appn\":\"" + APP_NAME_VALUE + "\",\"dpmt\":\"" + APP_ID_VALUE
                + "\",\"inst\":\"" + INSTANCE_ID_VALUE + "\",\"tid\":\"" + THREAD_NAME + "\",\"mod\":\"" + LOGGER_NAME
                + "\",\"lvl\":\"" + Level.ERROR.toString()
                + "\",\"msg\":{\"width\":4,\"length\":3,\"units\":\"inches\",\"height\":5},\"stck\":["
                + "[\"java.lang.Exception: java.lang.NullPointerException: example NullPointerException\","
                + "\"at com.ge.predix.some.package.Clazz.method(Clazz.java:473)\","
                + "\"at com.ge.predix.some.other.package.OtherClazz.diffMethod(OtherClazz.java:55)\"],"
                + "[\"java.lang.NullPointerException: example NullPointerException\","
                + "\"at com.ge.predix.some.package.Class.method(Class.java:234)\","
                + "\"at com.ge.predix.some.other.package.OtherClass.diffMethod(OtherClass.java:45)\"]]}\n";
        Assert.assertEquals(actual, expected);
    }

    @Test
    public void testPredixLayoutMissingInfoLog() {
        long timeStamp = Instant.now().toEpochMilli();
        String expectedTimeStamp = ISO_DATE_FORMAT.format(new Date(timeStamp));
        HashMap<String, String> mdc = getMDC();
        LogEvent logEvent = Log4jLogEvent.newBuilder()
                .setTimeMillis(timeStamp)
                .setThreadName(THREAD_NAME)
                .setContextData(new JdkMapAdapterStringMap(mdc))
                .build();
        String actual = predixLayout.toSerializable(logEvent);
        System.out.println(actual);
        String expected = "{\"time\":\"" + expectedTimeStamp + "\",\"tnt\":\"" + ZONE_VALUE + "\",\"corr\":\""
                + CORRELATION_VALUE + "\",\"appn\":\"" + APP_NAME_VALUE + "\",\"dpmt\":\"" + APP_ID_VALUE
                + "\",\"inst\":\"" + INSTANCE_ID_VALUE + "\",\"tid\":\"" + THREAD_NAME
                + "\",\"mod\":null,\"msg\":null}\n";
        Assert.assertEquals(actual, expected);
    }

    @Test
    public void testPredixLayoutWithNoMessageLineSeparator() {

        LogEvent input = createLogEvent(MULTI_LINE_TEXT_MESSAGE);

        String expected = "{\"time\":\"" + ISO_DATE_FORMAT.format(new Date(input.getTimeMillis())) + "\",\"tnt\":\""
                + ZONE_VALUE + "\",\"corr\":\"" + CORRELATION_VALUE + "\",\"appn\":\"" + APP_NAME_VALUE
                + "\",\"dpmt\":\"" + APP_ID_VALUE + "\",\"inst\":\"" + INSTANCE_ID_VALUE + "\",\"tid\":\"" + THREAD_NAME
                + "\",\"mod\":\"" + LOGGER_NAME + "\",\"lvl\":\"" + Level.INFO + "\",\"msg\":\"L1\\nL2\\nL3\"}\n";

        PredixLayout multiLinePredixLayout = PredixLayout.createLayout(null, null);
        String actual = multiLinePredixLayout.toSerializable(input);

        Assert.assertEquals(actual, expected);
    }

    @Test
    public void testPredixLayoutWithInvalidMessageLineSeparator() {

        LogEvent input = createLogEvent(MULTI_LINE_TEXT_MESSAGE);

        // If an invalid regex is detected, the layout will switch it off.
        String expected = "{\"time\":\"" + ISO_DATE_FORMAT.format(new Date(input.getTimeMillis())) + "\",\"tnt\":\""
                + ZONE_VALUE + "\",\"corr\":\"" + CORRELATION_VALUE + "\",\"appn\":\"" + APP_NAME_VALUE
                + "\",\"dpmt\":\"" + APP_ID_VALUE + "\",\"inst\":\"" + INSTANCE_ID_VALUE + "\",\"tid\":\"" + THREAD_NAME
                + "\",\"mod\":\"" + LOGGER_NAME + "\",\"lvl\":\"" + Level.INFO + "\",\"msg\":\"L1\\nL2\\nL3\"}\n";

        PredixLayout multiLinePredixLayout = PredixLayout.createLayout(null, "("); // Malformed regex
        String actual = multiLinePredixLayout.toSerializable(input);

        Assert.assertEquals(actual, expected);
    }

    @Test
    public void testPredixLayoutWithSimpleMessageLineSeparator() {

        LogEvent input = createLogEvent(MULTI_LINE_TEXT_MESSAGE);

        String expected = "{\"time\":\"" + ISO_DATE_FORMAT.format(new Date(input.getTimeMillis())) + "\",\"tnt\":\""
                + ZONE_VALUE + "\",\"corr\":\"" + CORRELATION_VALUE + "\",\"appn\":\"" + APP_NAME_VALUE
                + "\",\"dpmt\":\"" + APP_ID_VALUE + "\",\"inst\":\"" + INSTANCE_ID_VALUE + "\",\"tid\":\"" + THREAD_NAME
                + "\",\"mod\":\"" + LOGGER_NAME + "\",\"lvl\":\"" + Level.INFO
                + "\",\"msgLines\":[\"L1\",\"L2\",\"L3\"]}\n";

        PredixLayout multiLinePredixLayout = PredixLayout.createLayout(null, System.lineSeparator());
        String actual = multiLinePredixLayout.toSerializable(input);

        Assert.assertEquals(actual, expected);
    }

    @Test
    public void testPredixLayoutWithRegexMessageLineSeparator() {

        LogEvent input = createLogEvent(MULTI_LINE_TEXT_MESSAGE);

        String expected = "{\"time\":\"" + ISO_DATE_FORMAT.format(new Date(input.getTimeMillis())) + "\",\"tnt\":\""
                + ZONE_VALUE + "\",\"corr\":\"" + CORRELATION_VALUE + "\",\"appn\":\"" + APP_NAME_VALUE
                + "\",\"dpmt\":\"" + APP_ID_VALUE + "\",\"inst\":\"" + INSTANCE_ID_VALUE + "\",\"tid\":\"" + THREAD_NAME
                + "\",\"mod\":\"" + LOGGER_NAME + "\",\"lvl\":\"" + Level.INFO + "\",\"msgLines\":[\"L\",\"L\",\"L\"]}\n";

        PredixLayout multiLinePredixLayout = PredixLayout.createLayout(null, "[0-9]+\n?");
        String actual = multiLinePredixLayout.toSerializable(input);

        Assert.assertEquals(actual, expected);
    }

    @Test
    public void testPredixLayoutWithNonStringMessageAndMessageLineSeparator() {

        // When something other than a String is logged, the message line separator has no effect.
        LogEvent input = createLogEvent(OBJECT_MESSAGE);

        String expected = "{\"time\":\"" + ISO_DATE_FORMAT.format(new Date(input.getTimeMillis())) + "\",\"tnt\":\""
                + ZONE_VALUE + "\",\"corr\":\"" + CORRELATION_VALUE + "\",\"appn\":\"" + APP_NAME_VALUE
                + "\",\"dpmt\":\"" + APP_ID_VALUE + "\",\"inst\":\"" + INSTANCE_ID_VALUE + "\",\"tid\":\"" + THREAD_NAME
                + "\",\"mod\":\"" + LOGGER_NAME + "\",\"lvl\":\"" + Level.INFO.toString()
                + "\",\"msg\":{\"width\":4,\"length\":3,\"units\":\"inches\",\"height\":5}}\n";

        PredixLayout multiLinePredixLayout = PredixLayout.createLayout(null, System.lineSeparator());
        String actual = multiLinePredixLayout.toSerializable(input);

        Assert.assertEquals(actual, expected);
    }

    @Test
    public void testPredixLayoutWithCustomCorrelationKey() {

        HashMap<String, String> mdc = getMDC();
        mdc.remove(CORRELATION_KEY);
        mdc.put(CORRELATION_KEY_OTHER, CORRELATION_VALUE_OTHER);

        LogEvent input = createLogEvent(OBJECT_MESSAGE, mdc);

        String expected = "{\"time\":\"" + ISO_DATE_FORMAT.format(new Date(input.getTimeMillis())) + "\",\"tnt\":\""
                + ZONE_VALUE + "\",\"corr\":\"" + CORRELATION_VALUE_OTHER + "\",\"appn\":\"" + APP_NAME_VALUE
                + "\",\"dpmt\":\"" + APP_ID_VALUE + "\",\"inst\":\"" + INSTANCE_ID_VALUE + "\",\"tid\":\"" + THREAD_NAME
                + "\",\"mod\":\"" + LOGGER_NAME + "\",\"lvl\":\"" + Level.INFO.toString()
                + "\",\"msg\":{\"width\":4,\"length\":3,\"units\":\"inches\",\"height\":5}}\n";

        PredixLayout predixLayout = PredixLayout.createLayout(CORRELATION_KEY_OTHER, null);
        String actual = predixLayout.toSerializable(input);
        System.out.println(actual);
        Assert.assertEquals(actual, expected);
    }

    @Test
    public void testPredixLayoutWithMissingCustomCorrelationKey() {

        HashMap<String, String> mdc = getMDC();
        mdc.remove(CORRELATION_KEY);

        LogEvent input = createLogEvent(OBJECT_MESSAGE, mdc);

        String expected = "{\"time\":\"" + ISO_DATE_FORMAT.format(new Date(input.getTimeMillis())) + "\",\"tnt\":\""
                + ZONE_VALUE + "\",\"corr\":null,\"appn\":\"" + APP_NAME_VALUE
                + "\",\"dpmt\":\"" + APP_ID_VALUE + "\",\"inst\":\"" + INSTANCE_ID_VALUE + "\",\"tid\":\"" + THREAD_NAME
                + "\",\"mod\":\"" + LOGGER_NAME + "\",\"lvl\":\"" + Level.INFO.toString()
                + "\",\"msg\":{\"width\":4,\"length\":3,\"units\":\"inches\",\"height\":5}}\n";

        PredixLayout predixLayout = PredixLayout.createLayout(CORRELATION_KEY_OTHER, null);
        String actual = predixLayout.toSerializable(input);
        System.out.println(actual);
        Assert.assertEquals(actual, expected);
    }

    @Test
    public void testPredixLayoutWithMissingCustomCorrelationKeyDoesNotUseDefaultCorrelationKey() {

        LogEvent input = createLogEvent(OBJECT_MESSAGE);

        String expected = "{\"time\":\"" + ISO_DATE_FORMAT.format(new Date(input.getTimeMillis())) + "\",\"tnt\":\""
                + ZONE_VALUE + "\",\"corr\":null,\"appn\":\"" + APP_NAME_VALUE
                + "\",\"dpmt\":\"" + APP_ID_VALUE + "\",\"inst\":\"" + INSTANCE_ID_VALUE + "\",\"tid\":\"" + THREAD_NAME
                + "\",\"mod\":\"" + LOGGER_NAME + "\",\"lvl\":\"" + Level.INFO.toString()
                + "\",\"msg\":{\"width\":4,\"length\":3,\"units\":\"inches\",\"height\":5}}\n";

        PredixLayout predixLayout = PredixLayout.createLayout(CORRELATION_KEY_OTHER, null);
        String actual = predixLayout.toSerializable(input);
        System.out.println(actual);
        Assert.assertEquals(actual, expected);
    }

    @Test
    public void testPredixLayoutWithEmptyCustomCorrelationKeyUsesDefaultCorrelationKey() {

        LogEvent input = createLogEvent(OBJECT_MESSAGE);

        String expected = "{\"time\":\"" + ISO_DATE_FORMAT.format(new Date(input.getTimeMillis())) + "\",\"tnt\":\""
                + ZONE_VALUE + "\",\"corr\":\"" + CORRELATION_VALUE + "\",\"appn\":\"" + APP_NAME_VALUE
                + "\",\"dpmt\":\"" + APP_ID_VALUE + "\",\"inst\":\"" + INSTANCE_ID_VALUE + "\",\"tid\":\"" + THREAD_NAME
                + "\",\"mod\":\"" + LOGGER_NAME + "\",\"lvl\":\"" + Level.INFO.toString()
                + "\",\"msg\":{\"width\":4,\"length\":3,\"units\":\"inches\",\"height\":5}}\n";

        PredixLayout predixLayout = PredixLayout.createLayout("", null);
        String actual = predixLayout.toSerializable(input);
        System.out.println(actual);
        Assert.assertEquals(actual, expected);
    }

    @Test
    public void testPredixLayoutWithBlankCustomCorrelationKeyUsesDefaultCorrelationKey() {

        LogEvent input = createLogEvent(OBJECT_MESSAGE);

        String expected = "{\"time\":\"" + ISO_DATE_FORMAT.format(new Date(input.getTimeMillis())) + "\",\"tnt\":\""
                + ZONE_VALUE + "\",\"corr\":\"" + CORRELATION_VALUE + "\",\"appn\":\"" + APP_NAME_VALUE
                + "\",\"dpmt\":\"" + APP_ID_VALUE + "\",\"inst\":\"" + INSTANCE_ID_VALUE + "\",\"tid\":\"" + THREAD_NAME
                + "\",\"mod\":\"" + LOGGER_NAME + "\",\"lvl\":\"" + Level.INFO.toString()
                + "\",\"msg\":{\"width\":4,\"length\":3,\"units\":\"inches\",\"height\":5}}\n";

        PredixLayout predixLayout = PredixLayout.createLayout("    ", null);
        String actual = predixLayout.toSerializable(input);
        System.out.println(actual);
        Assert.assertEquals(actual, expected);
    }

    private static LogEvent createLogEvent(final Message message) {
        return createLogEvent(message, getMDC());
    }

    private static LogEvent createLogEvent(final Message message, HashMap<String, String> mdc) {
        return Log4jLogEvent.newBuilder()
                .setLoggerName(LOGGER_NAME)
                .setTimeMillis(Instant.now().toEpochMilli())
                .setLevel(Level.INFO)
                .setMessage(message)
                .setThreadName(THREAD_NAME)
                .setContextData(new JdkMapAdapterStringMap(mdc))
                .build();
    }

    private static HashMap<String, Object> buildObjectMessage() {
        HashMap<String, Object> msg = new HashMap<>();
        msg.put("height", 5);
        msg.put("width", 4);
        msg.put("length", 3);
        msg.put("units", "inches");
        return msg;
    }

    private static HashMap<String, String> getMDC() {
        HashMap<String, String> mdc = new HashMap<>();
        mdc.put(CORRELATION_KEY, CORRELATION_VALUE);
        mdc.put(APP_ID, APP_ID_VALUE);
        mdc.put(APP_NAME, APP_NAME_VALUE);
        mdc.put(INSTANCE_ID, INSTANCE_ID_VALUE);
        mdc.put(INSTANCE_INDEX, INSTANCE_INDEX_VALUE);
        mdc.put(ZONE_HEADER, ZONE_VALUE);
        return mdc;
    }
}
