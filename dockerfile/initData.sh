#!/bin/bash

if [ ! -d "/kramerius-data/foxml-import" ]; then
  mkdir /kramerius-data/foxml-import
fi

if [ ! -d "/kramerius-data/postgres-data" ]; then
  mkdir /kramerius-data/postgres-data
  mv /var/lib/postgresql/9.1/main /kramerius-data/postgres-data/main
fi

if [ ! -d "/kramerius-data/fedora-data" ]; then
  mv /home/kramerius/fedora/dataDefault /kramerius-data/fedora-data
fi

if [ ! -d "/kramerius-data/.kramerius4" ]; then
  NEW_RANDOM_FEDORA_PASSWORD=$(cat /dev/urandom | tr -dc 'a-zA-Z0-9' | fold -w 10 | head -n 1)
  mv /home/kramerius/.kramerius4Default /kramerius-data/.kramerius4
  sed "s/fedoraPass=fedoraAdmin/fedoraPass=$NEW_RANDOM_FEDORA_PASSWORD/" /kramerius-data/.kramerius4/configuration.properties > /kramerius-data/.kramerius4/configuration.properties_with_new_password
  mv /kramerius-data/.kramerius4/configuration.properties_with_new_password /kramerius-data/.kramerius4/configuration.properties
fi


if [ -d "/kramerius-data/.kramerius4" ]; then
  FEDORA_PASSWORD=$(/parser.sh fedoraPass /kramerius-data/.kramerius4/configuration.properties)
  xmlstarlet ed -u "/users/user/@password" -v $FEDORA_PASSWORD /home/kramerius/fedora/server/config/fedora-users.xml > fedora-users.xml
  mv fedora-users.xml /home/kramerius/fedora/server/config/fedora-users.xml
fi

