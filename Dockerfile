FROM node:22.9-bookworm AS frontend-builder

WORKDIR /kitu

COPY scripts /kitu/scripts
COPY frontend /kitu/frontend

RUN ./scripts/build_frontend.sh

FROM maven:3.9.9-amazoncorretto-21-al2023 AS backend-builder

WORKDIR /kitu

COPY server /kitu/server

WORKDIR /kitu/server

COPY --from=frontend-builder /kitu/server/target/classes/static /kitu/server/target/classes/static

RUN mvn package -DskipTests

FROM amazoncorretto:22.0.2-al2023

WORKDIR /kitu/server/target

COPY --from=backend-builder /kitu/server/target/kitu-0.0.1-SNAPSHOT.jar /kitu/server/target/kitu-0.0.1-SNAPSHOT.jar

ENTRYPOINT ["java", "-jar", "kitu-0.0.1-SNAPSHOT.jar"]
