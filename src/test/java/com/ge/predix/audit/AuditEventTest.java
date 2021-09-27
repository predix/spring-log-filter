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

package com.ge.predix.audit;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.time.Instant;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;
import org.testng.annotations.Test;

public class AuditEventTest {

    private static final String URI = "/guardians/123";
    private static final String ZONE_ID = "1234";
    private static final String METHOD = "POST";
    private static final String REQUEST_BODY = "request content";
    private static final String RESPONSE_BODY = "response content";

    @Test
    public void testAuditEvent() throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(URI);
        request.setMethod(METHOD);
        request.setContent(REQUEST_BODY.getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();

        ContentCachingRequestWrapper cachedRequest = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper cachedResponse = new ContentCachingResponseWrapper(response);

        // read from the request and write to the response to simulate what the controller will be doing
        cachedRequest.getReader().readLine();
        cachedResponse.getWriter().write(RESPONSE_BODY);

        AuditEvent event = new AuditEvent(cachedRequest, cachedResponse, ZONE_ID);
        // test that the time stamp on the Audit event is less then 250 ms from now
        assertTrue((Instant.now().toEpochMilli() - event.getTime().toEpochMilli()) < 250);
        assertEquals(event.getRequestUri(), URI);
        assertEquals(event.getZoneId(), ZONE_ID);
        assertEquals(event.getMethod(), METHOD);
        assertEquals(event.getRequestBody(), REQUEST_BODY);
        assertEquals(event.getResponseBody(), RESPONSE_BODY);
    }
}
