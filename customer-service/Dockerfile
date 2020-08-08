FROM openjdk:11

COPY target/runner/meecrowave-core-runner.jar /opt/meecrowave-runner.jar

EXPOSE 8080

COPY target/customer-service.war /opt/customer-service.war

ENTRYPOINT ["java", "-Djava.net.preferIPv4Stack=true", "-jar", "/opt/meecrowave-runner.jar", "--webapp", "/opt/customer-service.war"]
