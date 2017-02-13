package com.ge.predix.audit;

public interface AuditEventProcessor {

    boolean process(AuditEvent auditEvent);

}
