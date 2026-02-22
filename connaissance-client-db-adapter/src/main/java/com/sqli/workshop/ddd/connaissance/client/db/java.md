java 
  -javaagent:./tests/otl/opentelemetry-javaagent.jar 
  -Dotel.exporter.otlp.endpoint=http://10.237.34.8:4318 
  -Dotel.resource.attributes=service.name=cclient 
  -jar connaissance-client-app/target/connaissance-client-app-2.0.0-SNAPSHOT.jar