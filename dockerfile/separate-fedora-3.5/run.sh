#!/bin/sh
docker run -v /kramerius-data:/kramerius-data  -t -i -p 80:80 martinrumanek/fedora-3.5 /init.sh
