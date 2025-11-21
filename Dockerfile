FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

COPY target/agent-runtime-1.0.0.jar app.jar

EXPOSE 8080

ENV JAVA_OPTS="-Xms512m -Xmx2g"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
