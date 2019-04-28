FROM maven:3.6.0-jdk-8-alpine

LABEL maintainer="Richard Hendricksen <richard.hendricksen@codecontrol.nl>"

RUN apk update && \
    apk add -Uuv python less py-pip
RUN pip install awscli
RUN apk add -U tzdata && \
    cp /usr/share/zoneinfo/Europe/Amsterdam /etc/localtime
RUN apk --purge -v del py-pip
RUN rm /var/cache/apk/*

WORKDIR /build

COPY pom.xml .
RUN mvn org.apache.maven.plugins:maven-dependency-plugin:3.1.1:go-offline

COPY src/ /build/src
COPY bin/run.sh .

RUN mvn install --offline

ENTRYPOINT ["./run.sh"]
