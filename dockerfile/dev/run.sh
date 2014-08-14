#!/bin/sh
docker run -v /kramerius-data:/kramerius-data  -t -i --name="kramerius-dev" -d -p 80:80 -p 8080:8080 -p 8000:8000 -u root kramerius-dev /init.sh

