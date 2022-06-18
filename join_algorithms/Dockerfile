FROM maven:openjdk AS builder
WORKDIR /opt
COPY src ./src
COPY pom.xml .
RUN mvn -f pom.xml clean compile assembly:single

FROM openjdk:11
COPY --from=builder /opt/target/*.jar /opt/assets/job.jar
WORKDIR /opt/assets
CMD java -jar job.jar