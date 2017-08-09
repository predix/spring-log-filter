package com.ge.predix.log4j1;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;

import org.apache.log4j.Level;
import org.apache.log4j.spi.LocationInfo;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;
import org.junit.Before;
import org.junit.Test;

import junit.framework.Assert;

public class PredixLayoutTest {

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

    private final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    private final PredixLayout predixLayout = new PredixLayout();

    @Before
    public void beforeSuite() {
        this.simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    @Test
    public void testPredixLayoutRegularLog() throws IOException {
        LocationInfo info = new LocationInfo(FILE_NAME, CLASS_NAME, METHOD_NAME, LINE_NUMBER);
        long timeStamp = Instant.now().toEpochMilli();
        String expectedTimeStamp = this.simpleDateFormat.format(new Date(timeStamp));
        HashMap<String, String> mdc = getMDC();
        HashMap<String, Object> msg = getMsg();
        LoggingEvent logEvent = new LoggingEvent(FILE_NAME, null, timeStamp, Level.INFO, msg, THREAD_NAME, null, "ndc",
                info, mdc);
        String actual = predixLayout.format(logEvent);
        String expected = "{\"time\":\"" + expectedTimeStamp +"\",\"tnt\":\"" + ZONE_VALUE +"\",\"corr\":\"" 
        + CORRELATION_VALUE + "\",\"appn\":\"" + APP_NAME_VALUE + "\",\"dpmt\":\"" + APP_ID_VALUE + "\",\"inst\":\""
                + INSTANCE_ID_VALUE + "\",\"tid\":\"" + THREAD_NAME + "\",\"mod\":\"" + FILE_NAME + "\",\"lvl\":\""
                + Level.INFO.toString() + "\",\"msg\":{\"width\":4,\"length\":3,\"units\":\"inches\",\"height\":5}}\n";
        System.out.println(actual);
        Assert.assertEquals(expected, actual);
    }
    

    @Test
    public void testPredixLayoutSpecialCharsLog() throws IOException {
        LocationInfo info = new LocationInfo(FILE_NAME, CLASS_NAME, METHOD_NAME, LINE_NUMBER);
        long timeStamp = Instant.now().toEpochMilli();
        String expectedTimeStamp = this.simpleDateFormat.format(new Date(timeStamp));
        HashMap<String, String> mdc = getMDC();
        HashMap<String, Object> msg = new HashMap<>();
        msg.put("quote", '"');
        msg.put("backslash", (char)92);
        LoggingEvent logEvent = new LoggingEvent(FILE_NAME, null, timeStamp, Level.INFO, msg, THREAD_NAME, null, "ndc",
                info, mdc);
        String actual = predixLayout.format(logEvent);
        String expected = "{\"time\":\"" + expectedTimeStamp +"\",\"tnt\":\"" + ZONE_VALUE +"\",\"corr\":\"" 
        + CORRELATION_VALUE + "\",\"appn\":\"" + APP_NAME_VALUE + "\",\"dpmt\":\"" + APP_ID_VALUE + "\",\"inst\":\""
                + INSTANCE_ID_VALUE + "\",\"tid\":\"" + THREAD_NAME + "\",\"mod\":\"" + FILE_NAME + "\",\"lvl\":\""
                + Level.INFO.toString() + "\",\"msg\":{\"quote\":\"\\\"\",\"backslash\":\"\\\\\"}}\n";
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testPredixLayoutExceptionLog() throws IOException {
        LocationInfo info = new LocationInfo(FILE_NAME, CLASS_NAME, METHOD_NAME, LINE_NUMBER);
        long timeStamp = Instant.now().toEpochMilli();
        String expectedTimeStamp = this.simpleDateFormat.format(new Date(timeStamp));
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
        //check that a logEvent without a stack trace is not polluted from previous logEvent with a stack trace.
        logEvent = new LoggingEvent(FILE_NAME, null, timeStamp, Level.INFO,
                msg, THREAD_NAME, null, "ndc", info, mdc);
        actual = predixLayout.format(logEvent);
        expected = "{\"time\":\"" + expectedTimeStamp +"\",\"tnt\":\"" + ZONE_VALUE +"\",\"corr\":\"" 
        + CORRELATION_VALUE + "\",\"appn\":\"" + APP_NAME_VALUE + "\",\"dpmt\":\"" + APP_ID_VALUE + "\",\"inst\":\""
                + INSTANCE_ID_VALUE + "\",\"tid\":\"" + THREAD_NAME + "\",\"mod\":\"" + FILE_NAME + "\",\"lvl\":\""
                + Level.INFO.toString() + "\",\"msg\":{\"width\":4,\"length\":3,\"units\":\"inches\",\"height\":5}}\n";
        Assert.assertEquals(expected, actual);
    }
    
    @Test
    public void testPredixLayoutExceptionChainLog() throws IOException {
        
        LocationInfo info = new LocationInfo(FILE_NAME, CLASS_NAME, METHOD_NAME, LINE_NUMBER);
        long timeStamp = Instant.now().toEpochMilli();
        String expectedTimeStamp = this.simpleDateFormat.format(new Date(timeStamp));
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
        LoggingEvent logEvent = new LoggingEvent(FILE_NAME, null, timeStamp, Level.ERROR, msg.toString(), THREAD_NAME, throwable,
                "ndc", info, mdc);
        String actual = predixLayout.format(logEvent);

        String expected = "{\"time\":\"" + expectedTimeStamp + "\",\"tnt\":\"" + ZONE_VALUE + "\",\"corr\":\""
                + CORRELATION_VALUE + "\",\"appn\":\"" + APP_NAME_VALUE + "\",\"dpmt\":\"" + APP_ID_VALUE
                + "\",\"inst\":\"" + INSTANCE_ID_VALUE + "\",\"tid\":\"" + THREAD_NAME + "\",\"mod\":\"" + FILE_NAME
                + "\",\"lvl\":\"" + Level.ERROR.toString()
                + "\",\"msg\":\"{width=4, length=3, units=inches, height=5}\",\"stck\":["
                + "[\"java.lang.Exception: java.lang.NullPointerException: example NullPointerException\","
                + "\"at com.ge.predix.some.package.Clazz.method(Clazz.java:473)\","
                + "\"at com.ge.predix.some.other.package.OtherClazz.diffMethod(OtherClazz.java:55)\"],"
                + "[\"java.lang.NullPointerException: example NullPointerException\",\"at com.ge.predix.some.package.Class.method(Class.java:234)\","
                + "\"at com.ge.predix.some.other.package.OtherClass.diffMethod(OtherClass.java:45)\"]]}\n";
        Assert.assertEquals(expected, actual);
    }


    @Test
    public void testPredixLayoutMissingInfoLog() throws IOException {
        long timeStamp = Instant.now().toEpochMilli();
        String expectedTimeStamp = this.simpleDateFormat.format(new Date(timeStamp));
        HashMap<String, String> mdc = getMDC();
        LoggingEvent logEvent = new LoggingEvent(FILE_NAME, null, timeStamp, null, null, THREAD_NAME, null, "ndc",
                null, mdc);
        String actual = predixLayout.format(logEvent);
        System.out.println(actual);
        String expected = "{\"time\":\"" + expectedTimeStamp +"\",\"tnt\":\"" + ZONE_VALUE +"\",\"corr\":\"" 
        + CORRELATION_VALUE + "\",\"appn\":\"" + APP_NAME_VALUE + "\",\"dpmt\":\"" + APP_ID_VALUE + "\",\"inst\":\""
                + INSTANCE_ID_VALUE + "\",\"tid\":\"" + THREAD_NAME + "\",\"mod\":\"?\",\"msg\":null}\n";
        Assert.assertEquals(expected, actual);
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
