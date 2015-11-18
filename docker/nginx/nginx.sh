#!/bin/sh -ev
readonly MACHINE_NAME=default

if [[ -z $(docker-machine ls | grep $MACHINE_NAME) ]]; then
  docker-machine create -d virtualbox $MACHINE_NAME
fi

dockerenv=$(docker-machine env $MACHINE_NAME)
eval $dockerenv

cp ../../../platform/provisioning/puppet/modules/frontend/templates/etc/nginx/router.conf.erb .
cp -r ../../../platform/provisioning/puppet/modules/nginx/files/ nginxfiles/
NPS_VERSION=1.9.32.3
NGINX_VERSION=1.6.2
[[ -f release-${NPS_VERSION}-beta.zip ]] || wget https://github.com/pagespeed/ngx_pagespeed/archive/release-${NPS_VERSION}-beta.zip
[[ -f nginx-${NGINX_VERSION}.tar.gz ]] || wget http://nginx.org/download/nginx-${NGINX_VERSION}.tar.gz
[[ -f ${NPS_VERSION}.tar.gz ]] || wget https://dl.google.com/dl/page-speed/psol/${NPS_VERSION}.tar.gz

docker build -t mynginx .
docker run -p 80:80 -ti mynginx bash
