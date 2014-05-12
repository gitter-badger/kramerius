#!/bin/sh
docker.io run -v /kramerius-data:/kramerius-data  -t -i -p 80:80 -p 443:22 kramerius /bin/bash
