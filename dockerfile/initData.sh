#!/bin/bash

if [ ! -d "/kramerius-data/foxml-import" ]; then
  mkdir /kramerius-data/foxml-import
fi

if [ ! -d "/kramerius-data/foxml-import/530719f5-ee95-4449-8ce7-12b0f4cadb22" ]; then
  unzip /tmp/530719f5-ee95-4449-8ce7-12b0f4cadb22.zip -d /kramerius-data/foxml-import/
fi


