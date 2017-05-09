package com.ge.predix.log4j1;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.TimeZone;

import org.apache.log4j.helpers.PatternConverter;
import org.apache.log4j.spi.LoggingEvent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class PredixLayoutPattern extends PatternConverter {

    private final LinkedHashMap<String, Object> logFormatMap = new LinkedHashMap<String, Object>();

    private ObjectMapper mapperObj = new ObjectMapper();

    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    public PredixLayoutPattern() {
        this.simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
      }

    @Override
    protected String convert(final LoggingEvent event) {
        this.logFormatMap.put("time", this.simpleDateFormat.format(new Date(event.getTimeStamp())));
        this.logFormatMap.put("tnt", event.getMDC("Zone-Id"));
        this.logFormatMap.put("corr", event.getMDC("X-B3-TraceId"));
        this.logFormatMap.put("appn", event.getMDC("APP_NAME"));
        this.logFormatMap.put("dpmt", event.getMDC("APP_ID"));
        this.logFormatMap.put("inst", event.getMDC("INSTANCE_ID"));
        this.logFormatMap.put("tid", event.getThreadName());
        this.logFormatMap.put("mod", event.getLocationInformation().getFileName());
        this.logFormatMap.put("lvl", event.getLevel().toString());
        this.logFormatMap.put("msg", event.getMessage());
        this.logFormatMap.put("stck", event.getThrowableStrRep());
        try {
            return this.mapperObj.writeValueAsString(this.logFormatMap) + "\n";
        } catch (JsonProcessingException e) {
            return "Failed to convert log to json for event: " + event.getMessage();
        }
    }
}
