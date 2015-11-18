#!/bin/bash -ev

mkdir -p /var/log/nginx/
mkdir -p /var/cache
mkdir -p /var/cache/nginx
mkdir -p /var/cache/nginx/pagespeed
mkdir -p /etc/nginx
mkdir -p /etc/nginx/conf.d
mkdir -p /etc/nginx/redirects
touch /var/log/nginx/access.log
touch /var/log/nginx/error.log

sed 's/user                nginx;/user                root;/' nginxfiles/nginx.conf >/etc/nginx/nginx.conf
#cp nginxfiles/etc/init/nginx.conf /etc/init/nginx.conf
cp nginxfiles/mime.types /etc/nginx/mime.types
cp nginxfiles/redirects/redirects.conf /etc/nginx/redirects/redirects.conf
#cp nginxfiles/logrotate.conf /etc/logrotate.d/nginx

(cat values.erb router.conf.erb) | erb  >/etc/nginx/conf.d/router.conf
