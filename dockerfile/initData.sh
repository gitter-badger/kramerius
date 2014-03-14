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

#if [ ! -d "/kramerius-data/foxml-import/530719f5-ee95-4449-8ce7-12b0f4cadb22" ]; then
#  unzip /tmp/530719f5-ee95-4449-8ce7-12b0f4cadb22.zip -d /kramerius-data/foxml-import/
#fi


