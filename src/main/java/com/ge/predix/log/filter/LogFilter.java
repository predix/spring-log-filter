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

import static org.slf4j.MDC.put;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

public class LogFilter extends OncePerRequestFilter {

    private static final String LOG_CORRELATION_ID = "Correlation-Id";
    private static final String LOG_ZONE_ID = "Zone-Id";

    private final Set<String> hostnames;
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
    }

    @Override
    protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response,
            final FilterChain filterChain) throws ServletException, IOException {
        String correlationId = request.getHeader(LOG_CORRELATION_ID);
        if (StringUtils.isEmpty(correlationId)) {
            correlationId = UUID.randomUUID().toString();
        }
        
        put(LOG_CORRELATION_ID, correlationId);
        response.setHeader(LOG_CORRELATION_ID, correlationId);

        String zoneId = getZoneId(request);
        if (StringUtils.isNotEmpty(zoneId)) {
            put(LOG_ZONE_ID, zoneId);
        }

        filterChain.doFilter(request, response);
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
}
