#!/bin/sh
docker run -v /kramerius-data:/kramerius-data  -t -i -p 80:80 martinrumanek/kramerius-dev /init.sh
