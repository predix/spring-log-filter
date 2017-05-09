Utility filter for tracing, log enrichment and auditing.

# What does this filter do ?
## Populate HTTP headers for [tracing](opentracing.io)
This filter initializes an HTTP header(X-B3-TraceID) for tracing, if not already present. The header is also added in the outgoing response.
## Enrich SLF4J [MDC](https://logback.qos.ch/manual/mdc.html) with tracing and cloudfoundry VCAP info
* The log filter adds the following VCAP information to the MDC:
```
    APP_ID
    APP_NAME
    INSTANCE_INDEX
    INSTANCE_ID
```
* It also adds the following cloud-foundry VCAP info to the MDC for logging:
```
    X-B3-TraceId
    Zone-Id
```
* Optionally, this filter can also be used to generate audit events which includes the request and response payload.

### Example Log4j pattern to use tracing/vcap info in logs
Reference the PredixLayout for desired appenders.
```
log4j.appender.CONSOLE.layout=com.ge.predix.log4j1.PredixLayout
```

## Auditing
Wire an [AuditEventProcessor](src/main/java/com/ge/predix/audit/AuditEventProcessor.java) bean to 
[LogFilter](src/main/java/com/ge/predix/log/filter/LogFilter.java), to receive AuditEvent for each request.


# Build
```
mvn clean package
```

# Run Integration Tests
```
mvn clean verify
```

# LICENSE
This project is licensed under Apache v2.
