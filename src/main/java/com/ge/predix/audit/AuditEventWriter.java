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