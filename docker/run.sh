#!/bin/sh

brew cask install dockertoolbox

docker-machine create -d virtualbox default

dockerenv=$(docker-machine env default)
eval($dockerenv)

docker run -ti -v "$HOME/.ivy2:/root/.ivy2"  -v "$HOME/work/frontend:/root" -P hseeberger/scala-sbt sbt