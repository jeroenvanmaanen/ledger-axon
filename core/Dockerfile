FROM eclipse-temurin:8u472-b08-jre-alpine-3.22
MAINTAINER Jeroen van Maanen <jeroen@leialearns.org>

ENTRYPOINT ["/opt/java/openjdk/bin/java", "-jar", "/usr/share/ledger/ledger.jar"]

# Add Maven dependencies (not shaded into the artifact; Docker-cached)
##ADD target/lib           /usr/share/ledger/lib
# Add the service itself
ARG JAR_FILE
ADD target/${JAR_FILE} /usr/share/ledger/ledger.jar
