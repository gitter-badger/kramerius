#!/bin/bash

if [ ! -d "/oaiprovider-data/postgres-data" ]; then
  mkdir /oaiprovider-data/postgres-data
  mv /var/lib/postgresql/9.1/main /oaiprovider-data/postgres-data/main
fi

if [ ! -d "/oaiprovider-data/proai-data" ]; then
  mkdir /oaiprovider-data/proai-data
  chown -R oaiprovider:oaiprovider /oaiprovider-data/proai-data
fi

/etc/init.d/postgresql start
su -l oaiprovider -c "/home/oaiprovider/tomcat/bin/startup.sh"

sed -i -e "s|\${FEDORA_URL}|$FEDORA_URL|" \
       -e "s|\${FEDORA_USER}|$FEDORA_USER|" \
       -e "s|\${FEDORA_PASS}|$FEDORA_PASS|" \
       -e "s|\${FEDORA_JDBC_URL}|$FEDORA_JDBC_URL|" \
       -e "s|\${FEDORA_JDBC_USER}|$FEDORA_JDBC_USER|" \
       -e "s|\${FEDORA_JDBC_PASS}|$FEDORA_JDBC_PASS|" \
       /home/oaiprovider/tomcat/webapps/oaiprovider/WEB-INF/classes/proai.properties 

while :; do /bin/bash; sleep 1; done

