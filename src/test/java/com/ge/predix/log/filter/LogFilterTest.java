/*******************************************************************************
 * Copyright 2016 General Electric Company.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 * http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.ge.predix.log.filter;

import java.util.LinkedHashSet;
import java.util.UUID;

import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
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
    
    @Test
    public void testLogFilterWithCorrelationIdHeader() throws Exception {
        LogFilter logFilter = new LogFilter();
        MockHttpServletRequest request = new MockHttpServletRequest();
        String requestCorrelationId = UUID.randomUUID().toString();
        request.addHeader("Correlation-Id", requestCorrelationId);
        MockHttpServletResponse response = new MockHttpServletResponse();
        logFilter.doFilter(request, response, new MockFilterChain());
        String responseCorrelationId = response.getHeader("Correlation-Id");
        Assert.assertEquals(requestCorrelationId, responseCorrelationId);
    }

    @Test
    public void testLogFilterWithoutCorrelationIdHeader() throws Exception {
        LogFilter logFilter = new LogFilter();
        MockHttpServletResponse response = new MockHttpServletResponse();
        logFilter.doFilter(new MockHttpServletRequest(), response, new MockFilterChain());
        String responseCorrelationId = response.getHeader("Correlation-Id");
        Assert.assertNotNull(responseCorrelationId);
    }

}
