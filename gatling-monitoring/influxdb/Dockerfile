FROM influxdb:1.7-alpine
COPY influxdb.conf /etc/influxdb/influxdb.conf
COPY ./scripts /docker-entrypoint-initdb.d
