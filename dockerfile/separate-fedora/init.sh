#!/bin/bash


if [ ! -d "/kramerius-data/postgres-data" ]; then
  mkdir /kramerius-data/postgres-data
  mv /var/lib/postgresql/9.1/main /kramerius-data/postgres-data/main
fi

if [ ! -d "/kramerius-data/fedora-data" ]; then
  mv /home/kramerius/fedora/dataDefault /kramerius-data/fedora-data
fi

if [ ! -d "/kramerius-data/.fedora" ]; then
  mkdir /kramerius-data/.fedora
  NEW_RANDOM_FEDORA_PASSWORD=$(cat /dev/urandom | tr -dc 'a-zA-Z0-9' | fold -w 10 | head -n 1)
  echo "fedoraPass=$NEW_RANDOM_FEDORA_PASSWORD" > /kramerius-data/.fedora/configuration.properties
fi

if [ -d "/kramerius-data/.fedora" ]; then
  FEDORA_PASSWORD=$(/parser.sh fedoraPass /kramerius-data/.fedora/configuration.properties)
  xmlstarlet ed -u "/users/user/@password" -v $FEDORA_PASSWORD /home/kramerius/fedora/server/config/fedora-users.xml > fedora-users.xml
  mv fedora-users.xml /home/kramerius/fedora/server/config/fedora-users.xml
fi


/etc/init.d/postgresql start
/etc/init.d/apache2 start
su -l kramerius -c "/home/kramerius/tomcat/bin/startup.sh"

while :; do /bin/bash; sleep 1; done

