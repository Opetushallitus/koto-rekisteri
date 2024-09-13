FROM node:22.7-alpine3.20 AS frontend-builder

WORKDIR /kitu

COPY frontend /kitu/frontend

WORKDIR /kitu/frontend

RUN npm ci && npm run build

FROM maven:3.9.9-amazoncorretto-21-al2023 AS backend-builder

WORKDIR /kitu

COPY server /kitu/server

WORKDIR /kitu/server

COPY --from=frontend-builder /kitu/frontend/out /kitu/server/target/classes/static

RUN mvn package -DskipTests

FROM maven:3.9.9-amazoncorretto-21-al2023

WORKDIR /kitu/server/target

COPY --from=backend-builder /kitu/server/target/kitu-0.0.1-SNAPSHOT.jar /kitu/server/target/kitu-0.0.1-SNAPSHOT.jar

ENTRYPOINT ["java", "-jar", "kitu-0.0.1-SNAPSHOT.jar"]
