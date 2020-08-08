FROM maven:3.6.3-jdk-11-openj9 AS mvn

WORKDIR /usr/src/customer-service
COPY dependencies.pom ./pom.xml
RUN mvn package dependency:go-offline # cache dependencies
COPY pom.xml src ./
RUN mvn package

FROM openjdk:11

EXPOSE 8080
COPY --from=mvn /usr/src/customer-service/target/runner/meecrowave-core-runner.jar /opt/meecrowave-runner.jar
COPY --from=mvn /usr/src/customer-service/target/customer-service.war /opt/customer-service.war

ENTRYPOINT ["java", "-Djava.net.preferIPv4Stack=true", "-jar", "/opt/meecrowave-runner.jar", "--webapp", "/opt/customer-service.war"]