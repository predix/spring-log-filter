package com.ge.predix.log.filter;

import java.util.LinkedHashSet;

import org.springframework.mock.web.MockHttpServletRequest;
import org.testng.Assert;
import org.testng.annotations.Test;

public class LogFilterTest {

    @Test
    public void testLogFilterWithAcsHeader() throws Exception {
        LogFilter logFilter = new LogFilter();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Predix-Zone-Id", "log-test-zone");
        request.setServerName("localhost");

        Assert.assertEquals(logFilter.getZoneId(request), "log-test-zone");
    }
    
    @Test
    public void testLogFilterWithSubdomain() throws Exception {
        LogFilter logFilter = new LogFilter();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServerName("log-test-zone.localhost");

        Assert.assertEquals(logFilter.getZoneId(request), "log-test-zone");
    }
    
    @Test
    public void testLogFilterWithSubdomainAndHeader() throws Exception {
        LogFilter logFilter = new LogFilter();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Predix-Zone-Id", "log-test-zone");
        request.setServerName("log-test-zone-two.localhost");

        Assert.assertEquals(logFilter.getZoneId(request), "log-test-zone");
    }
    
    @Test
    public void testLogFilterWithTwoHeaders() throws Exception {
        LogFilter logFilter = new LogFilter();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Predix-Zone-Id", "log-test-zone");
        request.addHeader("X-Identity-Zone-Id", "log-test-zone-two");
        request.setServerName("localhost");

        Assert.assertEquals(logFilter.getZoneId(request), "log-test-zone");
    }
    
    @Test
    public void testLogFilterWithEmptyHeaderWithSubdomain() throws Exception {
        LogFilter logFilter = new LogFilter();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Predix-Zone-Id", "");
        request.setServerName("log-test-zone.localhost");

        Assert.assertEquals(logFilter.getZoneId(request), "log-test-zone");
    }
    
    @Test
    public void testLogFilterWithEmptyHeaderAndNoSubdomain() throws Exception {
        LogFilter logFilter = new LogFilter();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Predix-Zone-Id", "");
        request.setServerName("localhost");

        Assert.assertEquals(logFilter.getZoneId(request), "");
    }

    @Test
    public void testConstructorArguments() {
        LinkedHashSet<String> hostnames = new LinkedHashSet<>();
        hostnames.add("hostOne");
        hostnames.add("hostTwo");
        
        LinkedHashSet<String> zoneHeaders = new LinkedHashSet<>();
        zoneHeaders.add("headerOne");
        zoneHeaders.add("headerTwo");
        LogFilter logFilter = new LogFilter(hostnames, zoneHeaders, "acs");
        Assert.assertEquals(logFilter.getDefaultZone(), "acs");
        Assert.assertEquals(logFilter.getHostnames().toString(), hostnames.toString());
        Assert.assertEquals(logFilter.getZoneHeaders().toString(), zoneHeaders.toString());
    }

    @Test
    public void testConstructorArgumentsNoHeaders() {
        LinkedHashSet<String> hostnames = new LinkedHashSet<>();
        hostnames.add("hostOne");
        hostnames.add("hostTwo");

        LogFilter logFilter = new LogFilter(hostnames, null, "acs");
        Assert.assertEquals(logFilter.getDefaultZone(), "acs");
        Assert.assertEquals(logFilter.getHostnames().toString(), hostnames.toString());
        Assert.assertEquals(logFilter.getZoneHeaders().toString(), "[X-Identity-Zone-Id, Predix-Zone-Id]");
    }

    @Test
    public void testConstructorArgumentsNoHostNames() {
        LinkedHashSet<String> zoneHeaders = new LinkedHashSet<>();
        zoneHeaders.add("headerOne");
        zoneHeaders.add("headerTwo");

        LogFilter logFilter = new LogFilter(null, zoneHeaders, "acs");
        Assert.assertEquals(logFilter.getDefaultZone(), "acs");
        Assert.assertEquals(logFilter.getHostnames().toString(), "[localhost]");
        Assert.assertEquals(logFilter.getZoneHeaders().toString(), zoneHeaders.toString());
    }

    @Test
    public void testConstructorArgumentsNoDefaultZone() {
        LogFilter logFilter = new LogFilter(null, null, null);
        Assert.assertEquals(logFilter.getDefaultZone(), "");
        Assert.assertEquals(logFilter.getHostnames().toString(), "[localhost]");
        Assert.assertEquals(logFilter.getZoneHeaders().toString(), "[X-Identity-Zone-Id, Predix-Zone-Id]");
    }
}
