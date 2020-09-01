FROM maven:3.6.3-jdk-11-openj9 AS mvn

WORKDIR /usr/src/onlineshop
COPY dependencies.pom ./pom.xml
RUN mvn package dependency:go-offline # cache dependencies
COPY pom.xml ./
COPY src ./src
RUN mvn clean package

FROM openjdk:11

EXPOSE 8080
COPY --from=mvn /usr/src/onlineshop/target/runner/meecrowave-core-runner.jar /opt/meecrowave-runner.jar
COPY --from=mvn /usr/src/onlineshop/target/billing-service.war /opt/billing-service.war

ENTRYPOINT ["java", "--illegal-access=permit", "-Djava.net.preferIPv4Stack=true", "-jar", "/opt/meecrowave-runner.jar", "--webapp", "/opt/billing-service.war"]
