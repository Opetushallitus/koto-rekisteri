FROM maven:3.9.15-amazoncorretto-25-al2023 AS backend-builder

WORKDIR /kitu

COPY server /kitu/server

WORKDIR /kitu/server

RUN mvn package -DskipTests

FROM amazoncorretto:25.0.3-al2023

RUN groupadd -r app && useradd -r -g app -u 10001 -d /kitu app \
 && mkdir -p /kitu/server/target \
 && chown -R app:app /kitu

WORKDIR /kitu/server/target

COPY --from=backend-builder --chown=app:app /kitu/server/target/kitu-0.0.1-SNAPSHOT.jar /kitu/server/target/kitu-0.0.1-SNAPSHOT.jar

USER app

ENTRYPOINT ["java", "-jar", "kitu-0.0.1-SNAPSHOT.jar"]
