# Request logger

Sample of how to preprocess incoming and outgoing (feign) requests.

## Instructions

- build using the following command
```
mvn clean install
```
- include this library in another Spring project

pom.xml
``` xml
<dependency>                    
  <groupId>com.example</groupId>
  <artifactId>request-logger</artifactId>
  <version>0.0.1-SNAPSHOT</version>
</dependency>
```

- set the application to use it

```
@Import(RequestLoggerConfigurationReference.class)
public class SomeSpringApplication {
    ...
}
```
