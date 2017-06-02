package com.ge.predix.logback;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;

public class PredixEncoderTest {

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
    private static final int LINE_NUMBER = 23;
    private static final String THREAD_NAME = "Thread1";
    private static final String CORRELATION_HEADER = "X-B3-TraceId";
    private static final String ZONE_HEADER = "Zone-Id";

    private final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    private final PredixEncoder<ILoggingEvent> predixLayout = new PredixEncoder<>();

    @Before
    public void beforeSuite() {
        this.simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    @Test
    public void testPredixLayoutRegularLog() throws IOException {
        StackTraceElement[] caller = { new StackTraceElement(CLASS_NAME, METHOD_NAME, FILE_NAME, LINE_NUMBER) };
        long timeStamp = Instant.now().toEpochMilli();
        String expectedTimeStamp = this.simpleDateFormat.format(new Date(timeStamp));
        HashMap<String, String> mdc = getMDC();
        String msg = getMsg();
        Logger logger = new LoggerContext().getLogger(PredixEncoder.class);
        LoggingEvent logEvent = new LoggingEvent(CLASS_NAME, logger, Level.INFO, msg, null, null);
        logEvent.setMDCPropertyMap(mdc);
        logEvent.setThreadName("Thread1");
        logEvent.setCallerData(caller);
        logEvent.setTimeStamp(timeStamp);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream os = new PrintStream(baos);
        this.predixLayout.init(os);
        this.predixLayout.doEncode(logEvent);
        String actual = baos.toString();
        String expected = "{\"time\":\"" + expectedTimeStamp + "\",\"tnt\":\"" + ZONE_VALUE + "\",\"corr\":\""
                + CORRELATION_VALUE + "\",\"appn\":\"" + APP_NAME_VALUE + "\",\"dpmt\":\"" + APP_ID_VALUE
                + "\",\"inst\":\"" + INSTANCE_ID_VALUE + "\",\"tid\":\"" + THREAD_NAME + "\",\"mod\":\"" + FILE_NAME
                + "\",\"lvl\":\"" + Level.INFO.toString()
                + "\",\"msg\":\"{width=4, length=3, units=inches, height=5}\"}";
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testPredixLayoutSpecialCharsLog() throws IOException {
        StackTraceElement[] caller = { new StackTraceElement(CLASS_NAME, METHOD_NAME, FILE_NAME, LINE_NUMBER) };
        long timeStamp = Instant.now().toEpochMilli();
        String expectedTimeStamp = this.simpleDateFormat.format(new Date(timeStamp));
        HashMap<String, String> mdc = getMDC();
        String msg = "\"{}\n,\"\\";
        Logger logger = new LoggerContext().getLogger(PredixEncoder.class);
        LoggingEvent logEvent = new LoggingEvent(CLASS_NAME, logger, Level.INFO, msg.toString(), null, null);
        logEvent.setMDCPropertyMap(mdc);
        logEvent.setThreadName("Thread1");
        logEvent.setCallerData(caller);
        logEvent.setTimeStamp(timeStamp);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream os = new PrintStream(baos);
        this.predixLayout.init(os);
        this.predixLayout.doEncode(logEvent);
        String actual = baos.toString();
        String expected = "{\"time\":\"" + expectedTimeStamp + "\",\"tnt\":\"" + ZONE_VALUE + "\",\"corr\":\""
                + CORRELATION_VALUE + "\",\"appn\":\"" + APP_NAME_VALUE + "\",\"dpmt\":\"" + APP_ID_VALUE
                + "\",\"inst\":\"" + INSTANCE_ID_VALUE + "\",\"tid\":\"" + THREAD_NAME + "\",\"mod\":\"" + FILE_NAME
                + "\",\"lvl\":\"" + Level.INFO.toString() + "\",\"msg\":\"\\\"{}\\n,\\\"\\\\\"}";
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testPredixLayoutExceptionLog() throws IOException {
        StackTraceElement[] caller = { new StackTraceElement(CLASS_NAME, METHOD_NAME, FILE_NAME, LINE_NUMBER) };
        long timeStamp = Instant.now().toEpochMilli();
        String expectedTimeStamp = this.simpleDateFormat.format(new Date(timeStamp));
        HashMap<String, String> mdc = getMDC();
        String msg = getMsg();
        Throwable t = new NullPointerException();
        t.setStackTrace(new StackTraceElement[] {
                new StackTraceElement("com.ge.predix.some.package.Class", "method", "Class.java", 234),
                new StackTraceElement("com.ge.predix.some.other.package.OtherClass", "diffMethod", "OtherClass.java",
                        45) });
        Logger logger = new LoggerContext().getLogger(PredixEncoder.class);
        LoggingEvent logEvent = new LoggingEvent(CLASS_NAME, logger, Level.ERROR, msg.toString(), null, null);
        logEvent.setMDCPropertyMap(mdc);
        logEvent.setThreadName("Thread1");
        logEvent.setCallerData(caller);
        logEvent.setTimeStamp(timeStamp);
        logEvent.setThrowableProxy(new ThrowableProxy(t));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream os = new PrintStream(baos);
        this.predixLayout.init(os);
        this.predixLayout.doEncode(logEvent);
        String actual = baos.toString();

        String expected = "{\"time\":\"" + expectedTimeStamp + "\",\"tnt\":\"" + ZONE_VALUE + "\",\"corr\":\""
                + CORRELATION_VALUE + "\",\"appn\":\"" + APP_NAME_VALUE + "\",\"dpmt\":\"" + APP_ID_VALUE
                + "\",\"inst\":\"" + INSTANCE_ID_VALUE + "\",\"tid\":\"" + THREAD_NAME + "\",\"mod\":\"" + FILE_NAME
                + "\",\"lvl\":\"" + Level.ERROR.toString()
                + "\",\"msg\":\"{width=4, length=3, units=inches, height=5}\",\"stck\":"
                + "[[\"java.lang.NullPointerException\",\"at com.ge.predix.some.package.Class.method(Class.java:234)\","
                + "\"at com.ge.predix.some.other.package.OtherClass.diffMethod(OtherClass.java:45)\"]]}";
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testPredixLayoutExceptionChainLog() throws IOException {
        StackTraceElement[] caller = { new StackTraceElement(CLASS_NAME, METHOD_NAME, FILE_NAME, LINE_NUMBER) };
        long timeStamp = Instant.now().toEpochMilli();
        String expectedTimeStamp = this.simpleDateFormat.format(new Date(timeStamp));
        HashMap<String, String> mdc = getMDC();
        String msg = getMsg();
        Throwable exceptionCause = new NullPointerException();
        exceptionCause.setStackTrace(new StackTraceElement[] {
                new StackTraceElement("com.ge.predix.some.package.Class", "method", "Class.java", 234),
                new StackTraceElement("com.ge.predix.some.other.package.OtherClass", "diffMethod", "OtherClass.java",
                        45) });
        Throwable exceptionRoot = new Exception(exceptionCause);
        exceptionRoot.setStackTrace(new StackTraceElement[] {
                new StackTraceElement("com.ge.predix.some.package.Clazz", "method", "Clazz.java", 473),
                new StackTraceElement("com.ge.predix.some.other.package.OtherClazz", "diffMethod", "OtherClazz.java",
                        55) });
        Logger logger = new LoggerContext().getLogger(PredixEncoder.class);
        LoggingEvent logEvent = new LoggingEvent(CLASS_NAME, logger, Level.ERROR, msg.toString(), null, null);
        logEvent.setMDCPropertyMap(mdc);
        logEvent.setThreadName("Thread1");
        logEvent.setCallerData(caller);
        logEvent.setTimeStamp(timeStamp);
        logEvent.setThrowableProxy(new ThrowableProxy(exceptionRoot));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream os = new PrintStream(baos);
        this.predixLayout.init(os);
        this.predixLayout.doEncode(logEvent);
        String actual = baos.toString();

        String expected = "{\"time\":\"" + expectedTimeStamp + "\",\"tnt\":\"" + ZONE_VALUE + "\",\"corr\":\""
                + CORRELATION_VALUE + "\",\"appn\":\"" + APP_NAME_VALUE + "\",\"dpmt\":\"" + APP_ID_VALUE
                + "\",\"inst\":\"" + INSTANCE_ID_VALUE + "\",\"tid\":\"" + THREAD_NAME + "\",\"mod\":\"" + FILE_NAME
                + "\",\"lvl\":\"" + Level.ERROR.toString()
                + "\",\"msg\":\"{width=4, length=3, units=inches, height=5}\",\"stck\":["
                + "[\"java.lang.Exception: java.lang.NullPointerException\","
                + "\"at com.ge.predix.some.package.Clazz.method(Clazz.java:473)\","
                + "\"at com.ge.predix.some.other.package.OtherClazz.diffMethod(OtherClazz.java:55)\"],"
                + "[\"java.lang.NullPointerException\",\"at com.ge.predix.some.package.Class.method(Class.java:234)\","
                + "\"at com.ge.predix.some.other.package.OtherClass.diffMethod(OtherClass.java:45)\"]]}";
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testPredixLayoutMissingInfoLog() throws IOException {
        long timeStamp = Instant.now().toEpochMilli();
        String expectedTimeStamp = this.simpleDateFormat.format(new Date(timeStamp));
        HashMap<String, String> mdc = getMDC();
        Logger logger = new LoggerContext().getLogger(PredixEncoder.class);
        LoggingEvent logEvent = new LoggingEvent(CLASS_NAME, logger, null, null, null, null);
        logEvent.setMDCPropertyMap(mdc);
        logEvent.setThreadName("Thread1");
        logEvent.setCallerData(null);
        logEvent.setTimeStamp(timeStamp);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream os = new PrintStream(baos);
        this.predixLayout.init(os);
        this.predixLayout.doEncode(logEvent);
        String actual = baos.toString();
        String expected = "{\"time\":\"" + expectedTimeStamp + "\",\"tnt\":\"" + ZONE_VALUE + "\",\"corr\":\""
                + CORRELATION_VALUE + "\",\"appn\":\"" + APP_NAME_VALUE + "\",\"dpmt\":\"" + APP_ID_VALUE
                + "\",\"inst\":\"" + INSTANCE_ID_VALUE + "\",\"tid\":\"" + THREAD_NAME
                + "\",\"mod\":\"?\",\"msg\":null}";
        Assert.assertEquals(expected, actual);
    }

    private String getMsg() {
        HashMap<String, Object> msg = new HashMap<>();
        msg.put("height", 5);
        msg.put("width", 4);
        msg.put("length", 3);
        msg.put("units", "inches");
        return msg.toString();
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
