FROM eclipse-temurin:21-jre

COPY env-tests/cert/ign-fr-chain.der /app/ign-fr-chain.der
RUN keytool -import -alias ign-fr-chain -keystore $JAVA_HOME/lib/security/cacerts -file /app/ign-fr-chain.der -storepass changeit -noprompt
COPY connaissance-client-app/target/connaissance-client-app-2.2.0-SNAPSHOT.jar /app/connaissance-client.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/connaissance-client.jar"]
