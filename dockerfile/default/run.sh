#!/bin/sh
docker run -v /kramerius-data:/kramerius-data  -t -i --name="kramerius" -d -p 80:80  -u root kramerius /init.sh
