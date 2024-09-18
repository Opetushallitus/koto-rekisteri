FROM node:22.7-bookworm AS frontend-builder

WORKDIR /kitu

RUN apt-get update
RUN apt-get install tree

COPY scripts /kitu/scripts
COPY frontend /kitu/frontend

RUN ./scripts/build_frontend.sh

RUN ls -la
RUN tree /kitu

FROM maven:3.9.9-amazoncorretto-21-al2023 AS backend-builder

WORKDIR /kitu

COPY server /kitu/server

WORKDIR /kitu/server

COPY --from=frontend-builder /kitu/server/target/classes/static /kitu/server/target/classes/static

RUN mvn package -DskipTests

FROM maven:3.9.9-amazoncorretto-21-al2023

WORKDIR /kitu/server/target

COPY --from=backend-builder /kitu/server/target/kitu-0.0.1-SNAPSHOT.jar /kitu/server/target/kitu-0.0.1-SNAPSHOT.jar

ENTRYPOINT ["java", "-jar", "kitu-0.0.1-SNAPSHOT.jar"]
