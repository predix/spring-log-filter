Utility filter for tracing, log enrichment and auditing.

# What does this filter do ?
## Populate tracing headers
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
Add the log pattern to the log4j.properties file. Then reference the log pattern as the layout's conversion pattern using the EnhancedPatternLayout layout for desired appenders.
```
LOG_PATTERN={ "time":"%d{yyyy-MM-dd HH:mm:ss.SSS}{UTC}", "corr":"%X{X-B3-TraceId}", "appn":"%X{APP_NAME}", "dpmt":"%X{APP_ID}", "inst":"%X{INSTANCE_ID}", "tnt":"%X{Zone-Id}", "msg":"${project.artifactId}%X{context} - ${PID} [%t] .... %5p --- %c{1}: %m" }%n

log4j.appender.CONSOLE.layout=org.apache.log4j.EnhancedPatternLayout
log4j.appender.CONSOLE.layout.ConversionPattern=${LOG_PATTERN}
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
