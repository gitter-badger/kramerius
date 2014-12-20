#!/bin/sh
docker run -v /kramerius-data:/kramerius-data  -t -i --name="fedora" -p 80:80 -p 8080:8080 -u root fedora /bin/bash
