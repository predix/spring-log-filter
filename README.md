# spring-log-filter
* This filter initializes an HTTP header(X-B3-TraceID) for tracing, if not already present. The header is also added in the outgoing response.
* Optionally, this filter can also be used to generate audit events which includes the request and response payload.

##Standardize Log Messages
The log filter adds the following VCAP variables to the MDC:
    APP_ID
    APP_NAME
    INSTANCE_INDEX
    INSTANCE_ID

The log filter also adds the following variables to the MDC:
    X-B3-TraceId
    Zone-Id

###Log4j
Add the log pattern to the log4j.properties file. Then reference the log pattern as a conversion pattern for desired appenders.

LOG_PATTERN={ "time":"%d{yyyy-MM-dd HH:mm:ss.SSS,UTC}", "corr":"%X{X-B3-TraceId}", "appn":"%X{APP_NAME}", "dpmt":"%X{APP_ID}", "inst":"%X{INSTANCE_ID}", "tnt":"%X{Zone-Id}", "msg":"${project.artifactId}%X{context} - ${PID} [%t] .... %5p --- %c{1}: %m" }%n

log4j.appender.CONSOLE.layout.ConversionPattern=${LOG_PATTERN}

## Auditing
Wire an [AuditEventProcessor](src/main/java/com/ge/predix/audit/AuditEventProcessor.java) bean to 
[LogFilter](src/main/java/com/ge/predix/log/filter/LogFilter.java), to receive AuditEvent for each request.

# LICENSE
This project is licensed under Apache v2.

#Build
```
mvn clean package
```

#Run Integration Tests
```
mvn clean verify
```
