#!/bin/sh
docker run -v /kramerius-data:/kramerius-data  -t -i --name="kramerius-dev" -d  -u root kramerius /init.sh
