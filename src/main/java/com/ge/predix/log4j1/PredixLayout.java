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

package com.ge.predix.log4j1;

import org.apache.log4j.Layout;
import org.apache.log4j.spi.LoggingEvent;

public class PredixLayout extends Layout {

    private final PredixLayoutPattern predixLayoutPattern = new PredixLayoutPattern();

    public void setMessageLineSeparatorRegex(final String messageLineSeparatorRegex) {
        predixLayoutPattern.setMessageLineSeparatorRegex(messageLineSeparatorRegex);
    }

    public void setCorrelationKey(final String correlationKey) {
        predixLayoutPattern.setCorrelationKey(correlationKey);
    }

    @Override
    public String format(final LoggingEvent event) {
        return predixLayoutPattern.convert(event);
    }

    @Override
    public void activateOptions() {
        /**/
    }

    @Override
    public boolean ignoresThrowable() {
        return false;
    }
}
