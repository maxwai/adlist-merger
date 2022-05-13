FROM openjdk:17-buster

ENV SAVE_PATH "/save_location"
RUN apt-get update && apt-get -y install git
WORKDIR /usr/src
COPY src/ ./
RUN javac Main.java
CMD ["java", "Main"]