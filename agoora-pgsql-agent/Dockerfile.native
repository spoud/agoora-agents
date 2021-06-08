FROM quay.io/quarkus/centos-quarkus-maven:20.2.0-java11 AS build
RUN mkdir -p /tmp/ssl \
  && cp /opt/graalvm/lib/security/cacerts /tmp/ssl/

FROM registry.access.redhat.com/ubi8/ubi-minimal
WORKDIR /work/
COPY --from=build /tmp/ssl/ /work/
COPY ./target/*-runner /work/application

# set up permissions for user `1001`
RUN chmod 775 /work /work/application \
  && chown -R 1001 /work \
  && chmod -R "g+rwX" /work \
  && chown -R 1001:root /work

EXPOSE 8281
USER 1001

ENV JAVA_OPTS=-Xmx128m

CMD ["./application", "-Dquarkus.http.host=0.0.0.0", "-Djavax.net.ssl.trustStore=/work/cacerts", "$JAVA_OPTS"]
