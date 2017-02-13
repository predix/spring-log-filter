# spring-log-filter
* Spring filter that adds zone and correlation id to log
* Optionally, this filter can also be used to generate audit events which includes the request and response payload.

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
