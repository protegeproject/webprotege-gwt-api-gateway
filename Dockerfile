FROM openjdk:16-alpine
MAINTAINER protege.stanford.edu

EXPOSE 7777

COPY target/webprotege-gwt-api-gateway-0.1.1-SNAPSHOT.jar webprotege-gwt-api-gateway-0.1.1-SNAPSHOT.jar
ENTRYPOINT ["java","-jar","/webprotege-gwt-api-gateway-0.1.1-SNAPSHOT.jar"]