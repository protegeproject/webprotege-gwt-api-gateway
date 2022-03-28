FROM openjdk:16-alpine
MAINTAINER protege.stanford.edu

EXPOSE 7777

COPY target/${JAR_FILE} webprotege-gwt-api-gateway.jar
ENTRYPOINT ["java","-jar","/webprotege-gwt-api-gateway.jar"]