Utility  for request tracing, log enrichment and auditing.

# Maven Dependency to use this project

  ```xml
          <dependency>
              <groupId>com.ge.predix</groupId>
              <artifactId>spring-log-filter</artifactId>
              <version>${spring-log-filter.version}</version>
              <exclusions>
                  <exclusion>
                      <groupId>org.slf4j</groupId>
                      <artifactId>slf4j-log4j12</artifactId>
                  </exclusion>
              </exclusions>
          </dependency>
  ```

# HTTP Request tracing

## Populate zipkin HTTP headers for [tracing](opentracing.io)
This filter initializes an HTTP header(X-B3-TraceID) for tracing, if not already present. The header is also added in the returned response.  Note that if you are already using another library for propogating headers, this will have no effect.

* You will need to configure the following bean to specify how the filter determines tenant in a request
  ```xml
      <bean id="logFilter" class="com.ge.predix.log.filter.LogFilter">
         <constructor-arg>
              <set value-type="java.lang.String">
                  <value>${BASE_DOMAIN:localhost}</value>
              </set>
          </constructor-arg>
          <constructor-arg>
              <set value-type="java.lang.String">
                  <value>Predix-Zone-Id</value>
              </set>
          </constructor-arg>
          <constructor-arg value="DEFAULT_ZONE_NAME" />
      </bean>
  ```

# Enable logging in Predix common format
## Sample log message:
![](docs/sample-json-log.png)

## MDC enrichment
* LogFilter enriches SLF4J [MDC](https://logback.qos.ch/manual/mdc.html) with tracing and cloudfoundry VCAP info
   * The log filter adds the following VCAP information to the MDC. 
      ```
          APP_ID
          APP_NAME
          INSTANCE_ID
      ```
      * APP_NAME value can be customized using LogFilter.setCustomAppName
    * It also adds the following from HTTP headers:
      ```
          X-B3-TraceId
          Zone-Id
      ```
This information can be used by logging formatters to include in log messages. (see below)

## logback configuration
[PredixEncoder.java](src/main/java/com/ge/predix/logback/PredixEncoder.java) formats the log in JSON and includes the cloudfoundry VCAP info listed in the section above.

* Configure PredixEncoder 
  ```xml
  <appender name="myAppender" class="ch.qos.logback.core.ConsoleAppender">
    <encoder class="com.ge.predix.logback.PredixEncoder" />
  </appender>
  ```
  * Encoder source -  [PredixEncoder.java](src/main/java/com/ge/predix/logback/PredixEncoder.java)
  
## log4j configuration
[PredixLayoutPattern.java](src/main/java/com/ge/predix/log4j1/PredixLayoutPattern.java) formats the log in JSON and includes the cloudfoundry VCAP info listed in the section above.
* Configure log4j.properties to use PredixLayout
  ```
  log4j.appender.CONSOLE.layout=com.ge.predix.log4j1.PredixLayout
  ```
  
# Integration with Predix Audit

Optionally, this filter can also be used to generate audit events which includes the request and response payload.

  * Wire an [AuditEventProcessor](src/main/java/com/ge/predix/audit/AuditEventProcessor.java) bean to 
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
