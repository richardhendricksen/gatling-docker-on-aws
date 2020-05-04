FROM maven:3.6.1-jdk-8-alpine

ARG TIME_ZONE=Europe/Amsterdam

RUN apk add -Uuv python less py-pip openssl tzdata
RUN pip install awscli
RUN cp /usr/share/zoneinfo/${TIME_ZONE} /etc/localtime

RUN apk --purge -v del py-pip && \
    rm /var/cache/apk/*

WORKDIR /build

COPY pom.xml .
RUN mvn org.apache.maven.plugins:maven-dependency-plugin:3.1.1:go-offline

COPY src/ /build/src
COPY bin/run.sh .

RUN MAVEN_OPTS="-Xss20m" mvn install --offline

ENTRYPOINT ["./run.sh"]
