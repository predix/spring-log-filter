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

```json
{
    "time": "2017-07-10T20:59:48.923+0000",
    "tnt": "uaa",
    "corr": "02b47904ae01e8ec",
    "appn": "uaa-local",
    "dpmt": "b408c0ad-854f-44c4-8166-820259f6b4c0",
    "inst": "c089b940-0626-4d0b-5817-3f2d1229d7d2",
    "tid": "http-nio-8080-exec-3",
    "mod": "ClientController.java",
    "lvl": "ERROR",
    // either "msg" or "msgLines"
    "msg": "Failure during:\nPOST /v1/client",
    "msgLines": [
        "Failure during:",
        "POST /v1/client"
    ],
    "stck": [
        "error=\"access_denied\", error_description=\"Error requesting access token.\"",
        "at org.springframework.security.oauth2.client.token.OAuth2AccessTokenSupport.retrieveToken(OAuth2AccessTokenSupport.java:145)",
        "at org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsAccessTokenProvider.obtainAccessToken(ClientCredentialsAccessTokenProvider.java:44)",
        "at org.springframework.security.oauth2.client.token.AccessTokenProviderChain.obtainNewAccessTokenInternal(AccessTokenProviderChain.java:148)",
        "at org.springframework.security.oauth2.client.token.AccessTokenProviderChain.obtainAccessToken(AccessTokenProviderChain.java:121)",
        "... 150 more"
    ]
}
```

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

## Logback configuration

The [`PredixEncoder`](src/main/java/com/ge/predix/logback/PredixEncoder.java) formats the log in JSON and includes the cloudfoundry VCAP info listed in the section above.

* Configure `logback.xml` to use `PredixEncoder`:
  ```xml
  <appender name="myAppender" class="ch.qos.logback.core.ConsoleAppender">
      <encoder class="com.ge.predix.logback.PredixEncoder">
          <messageLineSeparatorRegex>\n</messageLineSeparatorRegex> <!-- optional -->
      </encoder>
  </appender>
  ```

## Log4J 1.2 configuration

The [`PredixLayout`](src/main/java/com/ge/predix/log4j1/PredixLayout.java) formats the log in JSON and includes the cloudfoundry VCAP info listed in the section above.

* Configure `log4j.properties` to use `PredixLayout`:
  ```
  log4j.appender.CONSOLE.layout=com.ge.predix.log4j1.PredixLayout
  log4j.appender.CONSOLE.messageLineSeparatorRegex=\n //optional
  ```

## Log4J 2 configuration

The [`PredixLayout`](src/main/java/com/ge/predix/log4j2/PredixLayout.java) formats the log in JSON and includes the cloudfoundry VCAP info listed in the section above.

* Configure `log4j2.xml` to use `PredixLayout`:
  ```xml
  <Console name="CONSOLE" target="SYSTEM_OUT">
      <PredixLayout messageLineSeparatorRegex="\n" />
  </Console>
  ```

# Multi-line message support

In the default mode, the encoder outputs the log message using the `msg` field. This behavior remains unchanged.

If the optional `messageLineSeparatorRegex` property is explicitly set, it activates line detection on the encoder.
In this mode, the message is pre-processed to split it into multiple lines using the provided regex. The encoder outputs
the resulting lines into a `msgLines` field instead of a `msg` field.

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
