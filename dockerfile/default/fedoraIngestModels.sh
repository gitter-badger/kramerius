#!/bin/bash

function ingest2fedora {
  curl -vv -u "fedoraAdmin:fedoraAdmin" -H "Content-type:text/xml" -d "format=info:fedora/fedora-system:FOXML-1.1" -X POST --upload-file $1 http://localhost:8080/fedora/objects/new
}

MODELS=/tmp/Installation-5.1/fedora/*.xml
for MODEL in $MODELS
do
  ingest2fedora $MODEL 
done

