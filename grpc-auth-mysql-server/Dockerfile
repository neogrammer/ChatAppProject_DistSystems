FROM gradle:8.7-jdk17 AS build
WORKDIR /src
COPY . .
RUN gradle clean shadowJar --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /src/build/libs/auth-service-all.jar /app/app.jar
EXPOSE 50051
ENV PORT=50051
ENTRYPOINT ["java","-jar","/app/app.jar"]