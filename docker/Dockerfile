FROM hseeberger/scala-sbt

# Install bundler
RUN apt-key update
RUN apt-get update
RUN apt-get -y install ruby ruby-dev make gcc
RUN gem install bundler

RUN mkdir /frontend
WORKDIR /frontend

EXPOSE 9000
