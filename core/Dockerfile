FROM openjdk:8-jre-alpine
MAINTAINER Jeroen van Maanen <jeroen@leialearns.org>

ENTRYPOINT ["/usr/bin/java", "-jar", "/usr/share/ledger/ledger.jar"]

# Add Maven dependencies (not shaded into the artifact; Docker-cached)
##ADD target/lib           /usr/share/ledger/lib
# Add the service itself
ARG JAR_FILE
ADD target/${JAR_FILE} /usr/share/ledger/ledger.jar
