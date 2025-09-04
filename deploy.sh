#!/bin/bash

HOST=root@192.168.21.57
#ssh $HOST "docker rm heating && docker image rm heating:latest"

#set -e
#docker compose build
#docker save heating:latest > heating.img.tar

#cat heating.img.tar | ssh $HOST "docker load"

DOCKER_HOST="ssh://$HOST" docker compose up
