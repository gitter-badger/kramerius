#!/bin/sh
docker run -v /kramerius-data:/kramerius-data  -t -i --name="fedora" -d -p 80:80 -p 8080:8080 -u root fedora-3.5 /init.sh
