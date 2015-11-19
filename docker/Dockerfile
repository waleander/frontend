FROM hseeberger/scala-sbt

# Install deps
RUN apt-key update
RUN apt-get update
RUN apt-get -y install ruby ruby-dev make gcc
RUN gem install bundler

# Install gems
COPY Gemfile /tmp/Gemfile
COPY Gemfile.lock /tmp/Gemfile.lock
RUN cd /tmp; bundle install --system

RUN mkdir /etc/gu
COPY install_vars /etc/gu/install_vars

RUN mkdir /frontend
WORKDIR /frontend

EXPOSE 9000

ENTRYPOINT /frontend/sbt
