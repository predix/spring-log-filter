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
