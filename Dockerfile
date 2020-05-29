FROM openjdk:11

RUN apt-get update && apt-get install -y libxml2-utils
