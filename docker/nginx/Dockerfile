FROM ubuntu

COPY router.conf.erb /root/router.conf.erb
COPY . /root

RUN apt-get update
RUN apt-get -y install ruby wget unzip

WORKDIR /root
RUN /root/pagespeed.sh
RUN /root/substitute.sh

EXPOSE 80

CMD nginx -g 'daemon off;'
