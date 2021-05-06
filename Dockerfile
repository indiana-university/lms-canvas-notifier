FROM registry.docker.iu.edu/lms/microservices_base:1.0.0
MAINTAINER LMS Development Team <iu-uits-lms-dev-l@list.iu.edu>

CMD exec java -jar /usr/src/app/canvasnotifier.jar
EXPOSE 5005

COPY --chown=lms:root target/canvasnotifier.jar /usr/src/app/