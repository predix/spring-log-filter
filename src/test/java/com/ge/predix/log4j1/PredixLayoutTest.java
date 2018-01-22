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

package com.ge.predix.log4j1;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;

import org.apache.log4j.Level;
import org.apache.log4j.spi.LocationInfo;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;
import org.junit.Test;

import junit.framework.Assert;

public class PredixLayoutTest {

    private static final SimpleDateFormat ISO_DATE_FORMAT;
    static {
        ISO_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        ISO_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    private static final String APP_ID = "APP_ID";
    private static final String APP_NAME = "APP_NAME";
    private static final String INSTANCE_ID = "INSTANCE_ID";
    private static final String INSTANCE_INDEX = "INSTANCE_INDEX";
    private static final String INSTANCE_ID_VALUE = "6758302";
    private static final String ZONE_VALUE = "test-zone";
    private static final String INSTANCE_INDEX_VALUE = "5";
    private static final String APP_NAME_VALUE = "uaa";
    private static final String APP_ID_VALUE = "098877475";
    private static final String CORRELATION_VALUE = "5678";
    private static final String FILE_NAME = "test.java";
    private static final String CLASS_NAME = "com.ge.predix";
    private static final String METHOD_NAME = "caculateVolume";
    private static final String LINE_NUMBER = "23";
    private static final String THREAD_NAME = "Thread1";
    private static final String CORRELATION_HEADER = "X-B3-TraceId";
    private static final String ZONE_HEADER = "Zone-Id";

    private static final Object MULTI_LINE_MESSAGE_TEXT = "L1\nL2" + System.lineSeparator() + "L3";

    private final PredixLayout predixLayout = new PredixLayout();

    @Test
    public void testPredixLayoutRegularLog() {
        LocationInfo info = new LocationInfo(FILE_NAME, CLASS_NAME, METHOD_NAME, LINE_NUMBER);
        long timeStamp = Instant.now().toEpochMilli();
        String expectedTimeStamp = ISO_DATE_FORMAT.format(new Date(timeStamp));
        HashMap<String, String> mdc = getMDC();
        HashMap<String, Object> msg = getMsg();
        LoggingEvent logEvent = new LoggingEvent(FILE_NAME, null, timeStamp, Level.INFO, msg, THREAD_NAME, null, "ndc",
                info, mdc);
        String actual = predixLayout.format(logEvent);
        String expected = "{\"time\":\"" + expectedTimeStamp + "\",\"tnt\":\"" + ZONE_VALUE + "\",\"corr\":\""
                + CORRELATION_VALUE + "\",\"appn\":\"" + APP_NAME_VALUE + "\",\"dpmt\":\"" + APP_ID_VALUE
                + "\",\"inst\":\"" + INSTANCE_ID_VALUE + "\",\"tid\":\"" + THREAD_NAME + "\",\"mod\":\"" + FILE_NAME
                + "\",\"lvl\":\"" + Level.INFO.toString()
                + "\",\"msg\":{\"width\":4,\"length\":3,\"units\":\"inches\",\"height\":5}}\n";
        System.out.println(actual);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testPredixLayoutSpecialCharsLog() {
        LocationInfo info = new LocationInfo(FILE_NAME, CLASS_NAME, METHOD_NAME, LINE_NUMBER);
        long timeStamp = Instant.now().toEpochMilli();
        String expectedTimeStamp = ISO_DATE_FORMAT.format(new Date(timeStamp));
        HashMap<String, String> mdc = getMDC();
        HashMap<String, Object> msg = new HashMap<>();
        msg.put("quote", '"');
        msg.put("backslash", (char) 92);
        LoggingEvent logEvent = new LoggingEvent(FILE_NAME, null, timeStamp, Level.INFO, msg, THREAD_NAME, null, "ndc",
                info, mdc);
        String actual = predixLayout.format(logEvent);
        String expected = "{\"time\":\"" + expectedTimeStamp + "\",\"tnt\":\"" + ZONE_VALUE + "\",\"corr\":\""
                + CORRELATION_VALUE + "\",\"appn\":\"" + APP_NAME_VALUE + "\",\"dpmt\":\"" + APP_ID_VALUE
                + "\",\"inst\":\"" + INSTANCE_ID_VALUE + "\",\"tid\":\"" + THREAD_NAME + "\",\"mod\":\"" + FILE_NAME
                + "\",\"lvl\":\"" + Level.INFO.toString() + "\",\"msg\":{\"quote\":\"\\\"\",\"backslash\":\"\\\\\"}}\n";
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testPredixLayoutExceptionLog() {
        LocationInfo info = new LocationInfo(FILE_NAME, CLASS_NAME, METHOD_NAME, LINE_NUMBER);
        long timeStamp = Instant.now().toEpochMilli();
        String expectedTimeStamp = ISO_DATE_FORMAT.format(new Date(timeStamp));
        HashMap<String, String> mdc = getMDC();
        HashMap<String, Object> msg = getMsg();
        Throwable exceptionThrowable = new Exception();
        exceptionThrowable.setStackTrace(new StackTraceElement[] {
                new StackTraceElement("com.ge.predix.some.package.Class", "method", "Class.java", 234),
                new StackTraceElement("com.ge.predix.some.other.package.OtherClass", "diffMethod", "OtherClass.java",
                        45) });

        ThrowableInformation throwable = new ThrowableInformation(exceptionThrowable);
        LoggingEvent logEvent = new LoggingEvent(FILE_NAME, null, timeStamp, Level.ERROR, msg, THREAD_NAME, throwable,
                "ndc", info, mdc);
        String actual = predixLayout.format(logEvent);
        String expected = "{\"time\":\"" + expectedTimeStamp + "\",\"tnt\":\"" + ZONE_VALUE + "\",\"corr\":\""
                + CORRELATION_VALUE + "\",\"appn\":\"" + APP_NAME_VALUE + "\",\"dpmt\":\"" + APP_ID_VALUE
                + "\",\"inst\":\"" + INSTANCE_ID_VALUE + "\",\"tid\":\"" + THREAD_NAME + "\",\"mod\":\"" + FILE_NAME
                + "\",\"lvl\":\"" + Level.ERROR.toString()
                + "\",\"msg\":{\"width\":4,\"length\":3,\"units\":\"inches\",\"height\":5},\"stck\":"
                + "[[\"java.lang.Exception\",\"at com.ge.predix.some.package.Class.method(Class.java:234)\",\""
                + "at com.ge.predix.some.other.package.OtherClass.diffMethod(OtherClass.java:45)\"]]}\n";
        Assert.assertEquals(expected, actual);
        // check that a logEvent without a stack trace is not polluted from previous logEvent with a stack trace.
        logEvent = new LoggingEvent(FILE_NAME, null, timeStamp, Level.INFO, msg, THREAD_NAME, null, "ndc", info, mdc);
        actual = predixLayout.format(logEvent);
        expected = "{\"time\":\"" + expectedTimeStamp + "\",\"tnt\":\"" + ZONE_VALUE + "\",\"corr\":\""
                + CORRELATION_VALUE + "\",\"appn\":\"" + APP_NAME_VALUE + "\",\"dpmt\":\"" + APP_ID_VALUE
                + "\",\"inst\":\"" + INSTANCE_ID_VALUE + "\",\"tid\":\"" + THREAD_NAME + "\",\"mod\":\"" + FILE_NAME
                + "\",\"lvl\":\"" + Level.INFO.toString()
                + "\",\"msg\":{\"width\":4,\"length\":3,\"units\":\"inches\",\"height\":5}}\n";
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testPredixLayoutExceptionChainLog() {

        LocationInfo info = new LocationInfo(FILE_NAME, CLASS_NAME, METHOD_NAME, LINE_NUMBER);
        long timeStamp = Instant.now().toEpochMilli();
        String expectedTimeStamp = ISO_DATE_FORMAT.format(new Date(timeStamp));
        HashMap<String, String> mdc = getMDC();
        HashMap<String, Object> msg = getMsg();
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

        ThrowableInformation throwable = new ThrowableInformation(exceptionRoot);
        LoggingEvent logEvent = new LoggingEvent(FILE_NAME, null, timeStamp, Level.ERROR, msg.toString(), THREAD_NAME,
                throwable, "ndc", info, mdc);
        String actual = predixLayout.format(logEvent);

        String expected = "{\"time\":\"" + expectedTimeStamp + "\",\"tnt\":\"" + ZONE_VALUE + "\",\"corr\":\""
                + CORRELATION_VALUE + "\",\"appn\":\"" + APP_NAME_VALUE + "\",\"dpmt\":\"" + APP_ID_VALUE
                + "\",\"inst\":\"" + INSTANCE_ID_VALUE + "\",\"tid\":\"" + THREAD_NAME + "\",\"mod\":\"" + FILE_NAME
                + "\",\"lvl\":\"" + Level.ERROR.toString()
                + "\",\"msg\":\"{width=4, length=3, units=inches, height=5}\",\"stck\":["
                + "[\"java.lang.Exception: java.lang.NullPointerException: example NullPointerException\","
                + "\"at com.ge.predix.some.package.Clazz.method(Clazz.java:473)\","
                + "\"at com.ge.predix.some.other.package.OtherClazz.diffMethod(OtherClazz.java:55)\"],"
                + "[\"java.lang.NullPointerException: example NullPointerException\","
                + "\"at com.ge.predix.some.package.Class.method(Class.java:234)\","
                + "\"at com.ge.predix.some.other.package.OtherClass.diffMethod(OtherClass.java:45)\"]]}\n";
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testPredixLayoutMissingInfoLog() {
        long timeStamp = Instant.now().toEpochMilli();
        String expectedTimeStamp = ISO_DATE_FORMAT.format(new Date(timeStamp));
        HashMap<String, String> mdc = getMDC();
        LoggingEvent logEvent = new LoggingEvent(FILE_NAME, null, timeStamp, null, null, THREAD_NAME, null, "ndc", null,
                mdc);
        String actual = predixLayout.format(logEvent);
        System.out.println(actual);
        String expected = "{\"time\":\"" + expectedTimeStamp + "\",\"tnt\":\"" + ZONE_VALUE + "\",\"corr\":\""
                + CORRELATION_VALUE + "\",\"appn\":\"" + APP_NAME_VALUE + "\",\"dpmt\":\"" + APP_ID_VALUE
                + "\",\"inst\":\"" + INSTANCE_ID_VALUE + "\",\"tid\":\"" + THREAD_NAME
                + "\",\"mod\":\"?\",\"msg\":null}\n";
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testPredixEncoderWithNoMessageLineSeparator() {

        LoggingEvent input = createLogEvent(MULTI_LINE_MESSAGE_TEXT);

        String expected = "{\"time\":\"" + ISO_DATE_FORMAT.format(new Date(input.getTimeStamp())) + "\",\"tnt\":\""
                + ZONE_VALUE + "\",\"corr\":\"" + CORRELATION_VALUE + "\",\"appn\":\"" + APP_NAME_VALUE
                + "\",\"dpmt\":\"" + APP_ID_VALUE + "\",\"inst\":\"" + INSTANCE_ID_VALUE + "\",\"tid\":\"" + THREAD_NAME
                + "\",\"mod\":\"" + FILE_NAME + "\",\"lvl\":\"" + Level.INFO + "\",\"msg\":\"L1\\nL2\\nL3\"}\n";

        PredixLayout multiLinePredixLayout = new PredixLayout();
        multiLinePredixLayout.setMessageLineSeparatorRegex(null);
        String actual = multiLinePredixLayout.format(input);

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testPredixEncoderWithInvalidMessageLineSeparator() {

        LoggingEvent input = createLogEvent(MULTI_LINE_MESSAGE_TEXT);

        // If an invalid regex is detected, the layout will switch it off.
        String expected = "{\"time\":\"" + ISO_DATE_FORMAT.format(new Date(input.getTimeStamp())) + "\",\"tnt\":\""
                + ZONE_VALUE + "\",\"corr\":\"" + CORRELATION_VALUE + "\",\"appn\":\"" + APP_NAME_VALUE
                + "\",\"dpmt\":\"" + APP_ID_VALUE + "\",\"inst\":\"" + INSTANCE_ID_VALUE + "\",\"tid\":\"" + THREAD_NAME
                + "\",\"mod\":\"" + FILE_NAME + "\",\"lvl\":\"" + Level.INFO + "\",\"msg\":\"L1\\nL2\\nL3\"}\n";

        PredixLayout multiLinePredixLayout = new PredixLayout();
        multiLinePredixLayout.setMessageLineSeparatorRegex("("); // Malformed regex
        String actual = multiLinePredixLayout.format(input);

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testPredixEncoderWithSimpleMessageLineSeparator() {

        LoggingEvent input = createLogEvent(MULTI_LINE_MESSAGE_TEXT);

        String expected = "{\"time\":\"" + ISO_DATE_FORMAT.format(new Date(input.getTimeStamp())) + "\",\"tnt\":\""
                + ZONE_VALUE + "\",\"corr\":\"" + CORRELATION_VALUE + "\",\"appn\":\"" + APP_NAME_VALUE
                + "\",\"dpmt\":\"" + APP_ID_VALUE + "\",\"inst\":\"" + INSTANCE_ID_VALUE + "\",\"tid\":\"" + THREAD_NAME
                + "\",\"mod\":\"" + FILE_NAME + "\",\"lvl\":\"" + Level.INFO
                + "\",\"msgLines\":[\"L1\",\"L2\",\"L3\"]}\n";

        PredixLayout multiLinePredixLayout = new PredixLayout();
        multiLinePredixLayout.setMessageLineSeparatorRegex(System.lineSeparator());
        String actual = multiLinePredixLayout.format(input);

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testPredixEncoderWithRegexMessageLineSeparator() {

        LoggingEvent input = createLogEvent(MULTI_LINE_MESSAGE_TEXT);

        String expected = "{\"time\":\"" + ISO_DATE_FORMAT.format(new Date(input.getTimeStamp())) + "\",\"tnt\":\""
                + ZONE_VALUE + "\",\"corr\":\"" + CORRELATION_VALUE + "\",\"appn\":\"" + APP_NAME_VALUE
                + "\",\"dpmt\":\"" + APP_ID_VALUE + "\",\"inst\":\"" + INSTANCE_ID_VALUE + "\",\"tid\":\"" + THREAD_NAME
                + "\",\"mod\":\"" + FILE_NAME + "\",\"lvl\":\"" + Level.INFO + "\",\"msgLines\":[\"L\",\"L\",\"L\"]}\n";

        PredixLayout multiLinePredixLayout = new PredixLayout();
        multiLinePredixLayout.setMessageLineSeparatorRegex("[0-9]+\n?");
        String actual = multiLinePredixLayout.format(input);

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testPredixEncoderWithNonStringMessageAndMessageLineSeparator() {

        // When something other than a String is logged, the message line separator has no effect.
        LoggingEvent input = createLogEvent(getMsg());

        String expected = "{\"time\":\"" + ISO_DATE_FORMAT.format(new Date(input.getTimeStamp())) + "\",\"tnt\":\""
                + ZONE_VALUE + "\",\"corr\":\"" + CORRELATION_VALUE + "\",\"appn\":\"" + APP_NAME_VALUE
                + "\",\"dpmt\":\"" + APP_ID_VALUE + "\",\"inst\":\"" + INSTANCE_ID_VALUE + "\",\"tid\":\"" + THREAD_NAME
                + "\",\"mod\":\"" + FILE_NAME + "\",\"lvl\":\"" + Level.INFO.toString()
                + "\",\"msg\":{\"width\":4,\"length\":3,\"units\":\"inches\",\"height\":5}}\n";

        PredixLayout multiLinePredixLayout = new PredixLayout();
        multiLinePredixLayout.setMessageLineSeparatorRegex(System.lineSeparator());
        String actual = multiLinePredixLayout.format(input);

        Assert.assertEquals(expected, actual);
    }

    private LoggingEvent createLogEvent(final Object message) {
        LocationInfo info = new LocationInfo(FILE_NAME, CLASS_NAME, METHOD_NAME, LINE_NUMBER);
        return new LoggingEvent(FILE_NAME, null, Instant.now().toEpochMilli(), Level.INFO, message, THREAD_NAME, null,
                "ndc", info, getMDC());
    }

    private HashMap<String, Object> getMsg() {
        HashMap<String, Object> msg = new HashMap<>();
        msg.put("height", 5);
        msg.put("width", 4);
        msg.put("length", 3);
        msg.put("units", "inches");
        return msg;
    }

    private HashMap<String, String> getMDC() {
        HashMap<String, String> mdc = new HashMap<>();
        mdc.put(CORRELATION_HEADER, CORRELATION_VALUE);
        mdc.put(APP_ID, APP_ID_VALUE);
        mdc.put(APP_NAME, APP_NAME_VALUE);
        mdc.put(INSTANCE_ID, INSTANCE_ID_VALUE);
        mdc.put(INSTANCE_INDEX, INSTANCE_INDEX_VALUE);
        mdc.put(ZONE_HEADER, ZONE_VALUE);
        return mdc;
    }
}
