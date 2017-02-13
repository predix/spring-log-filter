package com.ge.predix.log.filter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.filter.OncePerRequestFilter;

// this filter is used in the testLogFilterAudit to test audit functionality. It reads from the http request and writes
// to the response, as a controller would.
public class MockControllerFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response,
            final FilterChain filterChain) throws ServletException, IOException {

        request.getReader().readLine();
        response.getWriter().write(LogFilterTest.TEST_RESPONSE_CONTENT);

        filterChain.doFilter(request, response);
    }

}
