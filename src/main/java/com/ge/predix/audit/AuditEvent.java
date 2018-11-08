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

package com.ge.predix.audit;

import java.time.Instant;

import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author 212570782
 */

public class AuditEvent {

    private final String correlationId;
    private final String requestBody;
    private final String responseBody;
    private final String method;
    private final String sourceIp;
    private final String zoneId;
    private final Instant time;
    private final String requestUri;
    private final int status;
    private final String grantType;
    private final String toString;

    public AuditEvent(final ContentCachingRequestWrapper requestWrapper,
            final ContentCachingResponseWrapper responseWrapper, final String zoneId, final String correlationId,
            final String grantType)
            throws JsonProcessingException {
        this.correlationId = correlationId;
        this.status = responseWrapper.getStatus();
        this.method = requestWrapper.getMethod();
        this.sourceIp = requestWrapper.getRemoteHost();
        this.zoneId = zoneId;
        this.time = Instant.now();
        this.grantType = grantType;
        this.requestUri = requestWrapper.getRequestURI();
        this.requestBody = new String(requestWrapper.getContentAsByteArray());
        this.responseBody = new String(responseWrapper.getContentAsByteArray());
        this.toString = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(this);

    }

    public AuditEvent(final ContentCachingRequestWrapper requestWrapper,
            final ContentCachingResponseWrapper responseWrapper, final String zoneId, final String correlationId)
            throws JsonProcessingException {
        this(requestWrapper, responseWrapper, zoneId, correlationId, null);
    }

    @Override
    public String toString() {
        return this.toString;
    }

    public String getCorrelationId() {
        return this.correlationId;
    }

    public String getRequestBody() {
        return this.requestBody;
    }

    public String getResponseBody() {
        return this.responseBody;
    }

    public String getMethod() {
        return this.method;
    }

    public String getSourceIp() {
        return this.sourceIp;
    }

    public String getZoneId() {
        return this.zoneId;
    }

    public int getStatus() {
        return this.status;
    }

    public Instant getTime() {
        return this.time;
    }

    public String getRequestUri() {
        return this.requestUri;
    }

    public String getGrantType() {
        return this.grantType;
    }

}
