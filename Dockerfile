FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY . .
RUN mvn -B test package
RUN mkdir /app && cd /app && jar xf /build/target/brewlab.war

FROM tomcat:9.0-jdk21-temurin
COPY --from=build /app/ /usr/local/tomcat/webapps/ROOT/
COPY docker/entrypoint.sh /usr/local/bin/brewlab-start
RUN chmod +x /usr/local/bin/brewlab-start
EXPOSE 8080
ENTRYPOINT ["/usr/local/bin/brewlab-start"]
