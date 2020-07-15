FROM adoptopenjdk/openjdk8:jre8u242-b08-alpine

ARG TIME_ZONE=Europe/Amsterdam

RUN apk add -Uuv python less py-pip openssl tzdata
RUN pip install awscli
RUN cp /usr/share/zoneinfo/${TIME_ZONE} /etc/localtime

RUN apk --purge -v del py-pip && \
    rm /var/cache/apk/*

WORKDIR /gatling

COPY target/ ./target
COPY bin/run.sh .

ENTRYPOINT ["./run.sh"]
