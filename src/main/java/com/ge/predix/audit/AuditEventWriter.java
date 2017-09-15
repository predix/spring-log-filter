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

import java.io.OutputStream;

/**
 * @author 212570782
 */

import java.io.PrintWriter;
import java.util.Collection;
import java.util.stream.Collectors;

public class AuditEventWriter implements AuditEventProcessor {
    private final Collection<PrintWriter> writers;

    public AuditEventWriter(final Collection<OutputStream> outputStreams) {
        this.writers = outputStreams.stream().map(s -> new PrintWriter(s)).collect(Collectors.<PrintWriter>toList());
    }

    @Override
    public boolean process(final AuditEvent auditEvent) {
        this.writers.parallelStream().forEach(writer -> {
            writer.write(auditEvent.toString());
            writer.flush();
        });
        return true;
    }
}