FROM maven:3.9.9-amazoncorretto-21

WORKDIR /kitu

COPY . .

WORKDIR /kitu/server

RUN mvn package -DskipTests

ENTRYPOINT ["java", "-jar", "target/kitu-0.0.1-SNAPSHOT.jar"]
