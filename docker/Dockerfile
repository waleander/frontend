FROM hseeberger/scala-sbt

# Install bundler
RUN apt-get update
RUN apt-get -y install ruby ruby-dev make
RUN gem install bundler

RUN mkdir /frontend
WORKDIR /frontend

EXPOSE 9000
