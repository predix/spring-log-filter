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

package com.ge.predix.log.filter;

import static org.mockito.Matchers.any;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.ge.predix.audit.AuditEvent;
import com.ge.predix.audit.AuditEventWriter;

public class LogFilterTest {

    static final String TEST_RESPONSE_CONTENT = "test-response-content";
    private static final String TEST_REQUEST_CONTENT = "test-request-content";

    @Test
    public void testLogFilterWithAcsHeader() {
        LogFilter logFilter = new LogFilter();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Predix-Zone-Id", "log-test-zone");
        request.setServerName("localhost");

        Assert.assertEquals(logFilter.getZoneId(request), "log-test-zone");
    }

    @Test
    public void testLogFilterWithSubdomain() {
        LogFilter logFilter = new LogFilter();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServerName("log-test-zone.localhost");

        Assert.assertEquals(logFilter.getZoneId(request), "log-test-zone");
    }

    @Test
    public void testLogFilterWithSubdomainAndHeader() {
        LogFilter logFilter = new LogFilter();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Predix-Zone-Id", "log-test-zone");
        request.setServerName("log-test-zone-two.localhost");

        Assert.assertEquals(logFilter.getZoneId(request), "log-test-zone");
    }

    @Test
    public void testLogFilterWithTwoHeaders() {
        LogFilter logFilter = new LogFilter();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Predix-Zone-Id", "log-test-zone");
        request.addHeader("X-Identity-Zone-Id", "log-test-zone-two");
        request.setServerName("localhost");

        Assert.assertEquals(logFilter.getZoneId(request), "log-test-zone");
    }

    @Test
    public void testLogFilterWithEmptyHeaderWithSubdomain() {
        LogFilter logFilter = new LogFilter();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Predix-Zone-Id", "");
        request.setServerName("log-test-zone.localhost");

        Assert.assertEquals(logFilter.getZoneId(request), "log-test-zone");
    }

    @Test
    public void testLogFilterWithEmptyHeaderAndNoSubdomain() {
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
    public void testVcapApplicationEnvIsSet() throws IOException {
        String vcapString = new String(Files.readAllBytes(Paths.get("src/test/resources/vcap_example.json")));
        LinkedHashSet<String> hostnames = new LinkedHashSet<>();
        hostnames.add("hostOne");
        hostnames.add("hostTwo");
        LinkedHashSet<String> zoneHeaders = new LinkedHashSet<>();
        zoneHeaders.add("headerOne");
        zoneHeaders.add("headerTwo");
        LogFilter logFilter = new LogFilter(hostnames, zoneHeaders, "acs");
        logFilter.setVcapApplication(vcapString);
        Assert.assertEquals("2", logFilter.getVcapApplication().getInstanceIndex());
        Assert.assertEquals("5b2332ca1b6ff9c2d481b5353cd117e8", logFilter.getVcapApplication().getInstanceId());
        Assert.assertEquals("test-application", logFilter.getVcapApplication().getAppName());
        Assert.assertEquals("5e0a1f5f-7da0-4b1f-982f-e7d06da0886a", logFilter.getVcapApplication().getAppId());
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
    public void testLogFilterWithCustomAppName() throws Exception {
        LogFilter logFilter = new LogFilter();
        String appName = "custom app name";
        logFilter.setCustomAppName(appName);
        Map<String, String> expectMap = new HashMap<>();
        expectMap.put("APP_NAME", "custom app name");
        
        Servlet mockServlet = Mockito.mock(Servlet.class);

        logFilter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), new MockFilterChain(mockServlet, new MockMDCFilter(expectMap)));
    }

    @Test
    public void testLogFilterAudit() throws ServletException, IOException {
        AuditEventWriter testEventWriter = Mockito.mock(AuditEventWriter.class);
        Mockito.doAnswer(invocation -> {
            AuditEvent event = (AuditEvent) invocation.getArguments()[0];
            Assert.assertEquals(event.getRequestBody(), TEST_REQUEST_CONTENT);
            Assert.assertEquals(event.getResponseBody(), TEST_RESPONSE_CONTENT);
            return true;
        }).when(testEventWriter).process(any(AuditEvent.class));

        LogFilter testLogFilter = new LogFilter();
        testLogFilter.setAuditProcessor(testEventWriter);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContent(TEST_REQUEST_CONTENT.getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();

        Servlet mockServlet = Mockito.mock(Servlet.class);
        testLogFilter.doFilterInternal(request, response, new MockFilterChain(mockServlet, new MockControllerFilter()));
    }

}
