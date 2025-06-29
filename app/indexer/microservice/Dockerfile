FROM mcr.microsoft.com/openjdk/jdk:17-mariner AS build

WORKDIR /workspace/app
EXPOSE 3100

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY cli cli
COPY core core
COPY microservice microservice

RUN chmod +x ./mvnw
# Convert CRLF to LF
RUN sed -i 's/\r$//' ./mvnw
RUN ./mvnw package -DskipTests
RUN mkdir -p target/dependency && (cd target/dependency; jar -xf ../../microservice/target/*.jar)

FROM mcr.microsoft.com/openjdk/jdk:17-mariner

ARG DEPENDENCY=/workspace/app/target/dependency
COPY --from=build ${DEPENDENCY}/BOOT-INF/lib /app/lib
COPY --from=build ${DEPENDENCY}/META-INF /app/META-INF
COPY --from=build ${DEPENDENCY}/BOOT-INF/classes /app

RUN curl -s -LJ -o /app/applicationinsights-agent-3.4.19.jar https://github.com/microsoft/ApplicationInsights-Java/releases/download/3.4.19/applicationinsights-agent-3.4.19.jar
COPY microservice/applicationinsights.json /app

ENTRYPOINT ["java","-javaagent:/app/applicationinsights-agent-3.4.19.jar","-noverify", "-XX:MaxRAMPercentage=70", "-XX:+UseParallelGC", "-XX:ActiveProcessorCount=2", "-cp","app:app/lib/*","com.microsoft.openai.samples.indexer.service.IndexerApplication"]
