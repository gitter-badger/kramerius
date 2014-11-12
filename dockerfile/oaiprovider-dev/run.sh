#!/bin/sh
docker run -v /oaiprovider-data:/oaiprovider-data  -t -i --name="oaiprovider-dev" -d -p 80:80 -p 8080:8080 \
-e "FEDORA_URL=http://fedora.mzk.cz/fedora/" \
-e "FEDORA_USER=*" \
-e "FEDORA_PASS=*" \
-e "FEDORA_JDBC_URL=jdbc:postgresql://localhost/ritriples" \
-e "FEDORA_JDBC_USER=*" \
-e "FEDORA_JDBC_PASS=*" \
-u root oaiprovider /init.sh

