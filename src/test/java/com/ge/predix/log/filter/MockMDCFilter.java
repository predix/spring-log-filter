package com.ge.predix.log.filter;

import java.io.IOException;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import junit.framework.Assert;

//this filter is used in the testLogFilterAudit to test MDC values are set correctly
public class MockMDCFilter extends OncePerRequestFilter {

    private Map<String, String> expectMap;

    public MockMDCFilter(Map<String, String> expectMap) {
        super();
        this.expectMap = expectMap;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest paramHttpServletRequest,
            HttpServletResponse paramHttpServletResponse, FilterChain paramFilterChain)
            throws ServletException, IOException {
        
        for(String key : expectMap.keySet()) {
            Assert.assertEquals(expectMap.get(key), MDC.get(key));
        }
        
        paramFilterChain.doFilter(paramHttpServletRequest, paramHttpServletResponse);
    }

}
