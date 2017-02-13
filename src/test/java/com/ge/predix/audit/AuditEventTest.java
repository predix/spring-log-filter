package com.ge.predix.audit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.time.Instant;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;
import org.testng.annotations.Test;

public class AuditEventTest {

    private static final String URI = "/guardians/123";
    private static final String CORRELATION_HEADER = "Correlation-Id";
    private static final String ZONE_ID = "1234";
    private static final String CORRELATION_VALUE = "5678";
    private static final String METHOD = "POST";
    private static final String REQUEST_BODY = "request content";
    private static final String RESPONSE_BODY = "response content";

    @Test
    public void testAuditEvent() throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(URI);
        request.addHeader(CORRELATION_HEADER, CORRELATION_VALUE);
        request.setMethod(METHOD);
        request.setContent(REQUEST_BODY.getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();

        ContentCachingRequestWrapper cachedRequest = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper cachedResponse = new ContentCachingResponseWrapper(response);

        // read from the request and write to the response to simulate what the controller will be doing
        cachedRequest.getReader().readLine();
        cachedResponse.getWriter().write(RESPONSE_BODY);

        AuditEvent event = new AuditEvent(cachedRequest, cachedResponse, ZONE_ID, CORRELATION_VALUE);
        // test that the time stamp on the Audit event is less then 250 ms from now
        assertTrue((Instant.now().toEpochMilli() - event.getTime().toEpochMilli()) < 250);
        assertEquals(event.getRequestUri(), URI);
        assertEquals(event.getZoneId(), ZONE_ID);
        assertEquals(event.getCorrelationId(), CORRELATION_VALUE);
        assertEquals(event.getMethod(), METHOD);
        assertEquals(event.getRequestBody(), REQUEST_BODY);
        assertEquals(event.getResponseBody(), RESPONSE_BODY);
    }

}
