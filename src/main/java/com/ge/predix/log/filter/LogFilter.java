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

package com.ge.predix.log.filter;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StreamUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ge.predix.audit.AuditEvent;
import com.ge.predix.audit.AuditEventProcessor;
import com.ge.predix.vcap.VcapApplication;

public class LogFilter extends OncePerRequestFilter {

    private static final String APP_ID = "APP_ID";
    private static final String APP_NAME = "APP_NAME";
    private static final String INSTANCE_ID = "INSTANCE_ID";
    private static final String INSTANCE_INDEX = "INSTANCE_INDEX";
    private static final String CORRELATION_HEADER_NAME = "X-B3-TraceId";
    private static final String ZONE_HEADER_NAME = "Zone-Id";

    @Value("${VCAP_APPLICATION:}")
    private String vcapApplicationEnvJson;
    
    private String customAppName;

    @Autowired(required = false)
    private AuditEventProcessor auditProcessor;
    
    private ObjectMapper mapper = new ObjectMapper();

    private final Set<String> hostnames;

    private VcapApplication vcapApplication;

    public Set<String> getHostnames() {
        return this.hostnames;
    }

    public Set<String> getZoneHeaders() {
        return this.zoneHeaders;
    }

    public String getDefaultZone() {
        return this.defaultZone;
    }

    private final Set<String> zoneHeaders;
    private final String defaultZone;

    public LogFilter(final LinkedHashSet<String> hostnames, final LinkedHashSet<String> zoneHeaders,
            final String defaultZone) {
        if ((null == hostnames) || (hostnames.isEmpty())) {
            this.hostnames = new LinkedHashSet<>();
            this.hostnames.add("localhost");
        } else {
            this.hostnames = hostnames;
        }

        if ((null == zoneHeaders) || (zoneHeaders.isEmpty())) {
            this.zoneHeaders = new LinkedHashSet<>();
            this.zoneHeaders.add("X-Identity-Zone-Id");
            this.zoneHeaders.add("Predix-Zone-Id");
        } else {
            this.zoneHeaders = zoneHeaders;
        }

        if (null == defaultZone) {
            this.defaultZone = "";
        } else {
            this.defaultZone = defaultZone;
        }
    }

    public LogFilter() {
        this.hostnames = new LinkedHashSet<>();
        this.hostnames.add("localhost");
        this.zoneHeaders = new LinkedHashSet<>();
        this.zoneHeaders.add("Predix-Zone-Id");
        this.zoneHeaders.add("X-Identity-Zone-Id");
        this.defaultZone = "";
        this.vcapApplication = null;
    }

    @Override
    protected void doFilterInternal(final HttpServletRequest request,
            final HttpServletResponse response, final FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String correlationId = setCorrelationId(request, response);
            String zoneId = setZoneId(request);
            addVcapToMDC();
            addAppNameToMDC();
            if (null == this.auditProcessor) {
                filterChain.doFilter(request, response);
            } else {
                ContentCachingRequestWrapper cachedRequestWrapper = new ContentCachingRequestWrapper(request);
                ContentCachingResponseWrapper cachedResponseWrapper = new ContentCachingResponseWrapper(
                        response);

                filterChain.doFilter(cachedRequestWrapper, cachedResponseWrapper);

                // post request processing.
                this.auditProcessor.process(new AuditEvent(cachedRequestWrapper,
                        cachedResponseWrapper, zoneId, correlationId));
                copyBodyToResponse(cachedResponseWrapper);
            }
        } finally {
            clearMDC();
        }
    }

    private void clearMDC() {
        MDC.remove(APP_ID);
        MDC.remove(APP_NAME);
        MDC.remove(INSTANCE_ID);
        MDC.remove(INSTANCE_INDEX);
        MDC.remove(ZONE_HEADER_NAME);
        MDC.remove(CORRELATION_HEADER_NAME);
    }
    
    private void addAppNameToMDC() {
        if (customAppName != null) {
            MDC.put(APP_NAME, customAppName);
        } else if (this.vcapApplication != null) {
            MDC.put(APP_NAME, this.vcapApplication.getAppName());
        }
    }

    private void addVcapToMDC() {
        if (this.vcapApplication != null) {
            MDC.put(APP_ID, this.vcapApplication.getAppId());
            MDC.put(INSTANCE_ID, this.vcapApplication.getInstanceId());
            MDC.put(INSTANCE_INDEX, this.vcapApplication.getInstanceIndex());
        }
    }

    // This method has been made public in the current version of spring-web, in ContentCachingResponseWrapper. Can
    // be removed after upgrade.
    private void copyBodyToResponse(final ContentCachingResponseWrapper cachedResponseWrapper) throws IOException {
        if (cachedResponseWrapper.getContentAsByteArray().length > 0) {
            cachedResponseWrapper.getResponse().setContentLength(cachedResponseWrapper.getContentAsByteArray().length);
            StreamUtils.copy(cachedResponseWrapper.getContentAsByteArray(),
                    cachedResponseWrapper.getResponse().getOutputStream());
            cachedResponseWrapper.resetBuffer();
        }
    }

    private String setZoneId(final HttpServletRequest request) {
        String zoneId = getZoneId(request);
        if (StringUtils.isNotEmpty(zoneId)) {
            MDC.put(ZONE_HEADER_NAME, zoneId);
        }
        return zoneId;
    }

    private String setCorrelationId(final HttpServletRequest request, final HttpServletResponse response) {
        String correlationId = request.getHeader(CORRELATION_HEADER_NAME);
        if (StringUtils.isEmpty(correlationId)) {
            correlationId = UUID.randomUUID().toString();
        }
        MDC.put(CORRELATION_HEADER_NAME, correlationId);
        response.setHeader(CORRELATION_HEADER_NAME, correlationId);
        return correlationId;
    }

    String getZoneId(final HttpServletRequest request) {
        String zoneId = null;
        for (String zoneIdHeader : this.zoneHeaders) {
            zoneId = request.getHeader(zoneIdHeader);
            if (!StringUtils.isEmpty(zoneId)) {
                return zoneId;
            }
        }
        String hostname = request.getServerName();
        return getSubdomain(hostname);
    }

    String getSubdomain(final String hostname) {
        if (this.hostnames.contains(hostname)) {
            return this.defaultZone;
        }
        for (String internalHostname : this.hostnames) {
            if (hostname.endsWith("." + internalHostname)) {
                return hostname.substring(0, hostname.length() - internalHostname.length() - 1);
            }
        }
        return null;
    }
    
    public void setAuditProcessor(final AuditEventProcessor auditProcessor) {
        this.auditProcessor = auditProcessor;
    }
    

    public String getCustomAppName() {
        return customAppName;
    }

    public void setCustomAppName(final String customAppName) {
        this.customAppName = customAppName;
    }

    
    @Override
    public void afterPropertiesSet() {
        this.setVcapApplication(this.vcapApplicationEnvJson);
    }

    public VcapApplication getVcapApplication() {
        return this.vcapApplication;
    }

    void setVcapApplication(final String vcapString) {
        try {
            this.vcapApplication = this.mapper.readValue(vcapString, VcapApplication.class);
        } catch (Exception e) {
            this.logger.error("Error while reading from vcap.");
            this.logger.error(e.getMessage());
            this.vcapApplication = null;
        }
    }
}
