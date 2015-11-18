#!/bin/sh
apt-get -y install build-essential libc6 libgd2-noxpm-dev libpam0g libpcre3 libpcre3-dev libssl1.0.0 libssl-dev zlib1g unzip

NPS_VERSION=1.9.32.3
# wget in nginx.sh now
unzip release-${NPS_VERSION}-beta.zip
cd ngx_pagespeed-release-${NPS_VERSION}-beta/
#wget - cp it instead
cp ../${NPS_VERSION}.tar.gz .
tar -xzvf ${NPS_VERSION}.tar.gz
cd ..

NGINX_VERSION=1.6.2
# wget in nginx.sh now
tar -xvzf nginx-${NGINX_VERSION}.tar.gz
cd nginx-${NGINX_VERSION}/
./configure \
  --sbin-path=/usr/sbin/nginx \
  --conf-path=/etc/nginx/nginx.conf \
  --with-http_ssl_module \
  --add-module=../ngx_pagespeed-release-${NPS_VERSION}-beta
make
sudo apt-get remove nginx*
sudo make install