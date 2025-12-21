FROM maven:3.9.12-amazoncorretto-25-al2023 AS backend-builder

WORKDIR /kitu

COPY server /kitu/server

WORKDIR /kitu/server

RUN mvn package -DskipTests

FROM amazoncorretto:25.0.1-al2023

WORKDIR /kitu/server/target

COPY --from=backend-builder /kitu/server/target/kitu-0.0.1-SNAPSHOT.jar /kitu/server/target/kitu-0.0.1-SNAPSHOT.jar

ENTRYPOINT ["java", "-jar", "kitu-0.0.1-SNAPSHOT.jar"]
