FROM eclipse-temurin:17-jre-jammy
MAINTAINER protege.stanford.edu

EXPOSE 7777
ARG JAR_FILE
COPY target/${JAR_FILE} webprotege-gwt-api-gateway.jar
ENTRYPOINT ["java","-jar","/webprotege-gwt-api-gateway.jar"]