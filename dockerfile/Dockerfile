FROM debian

MAINTAINER Martin Rumanek <martin@rumanek.cz>

# install packages
RUN apt-get -y  update
RUN apt-get -y install wget openjdk-7-jre-headless postgresql apache2 libapache2-mod-jk vim unzip xmlstarlet

# download java binaries
RUN wget --no-verbose http://mirror.hosting90.cz/apache/tomcat/tomcat-7/v7.0.52/bin/apache-tomcat-7.0.52.tar.gz -O /tmp/tomcat.tar.gz
RUN wget --no-verbose http://sourceforge.net/projects/fedora-commons/files/fedora/3.7.0/fcrepo-installer-3.7.0.jar/download -O /tmp/fedora-install.jar

RUN sed 's/local   all             all                                     peer/local   all             all                                     md5/' \
  /etc/postgresql/9.1/main/pg_hba.conf > pg_hba.conf

RUN mv pg_hba.conf /etc/postgresql/9.1/main/pg_hba.conf

# start postgres and create user and db
RUN /etc/init.d/postgresql start && su -l postgres -c "createuser fedoraAdmin --no-superuser --no-createdb --no-createrole; \
 createdb fedora3 --owner=fedoraAdmin; createdb kramerius4 --owner=fedoraAdmin; createdb riTriples --owner=fedoraAdmin; \
 psql --command \"alter user \\\"fedoraAdmin\\\" with password 'fedoraAdmin';\" "

RUN touch test
# install tomcat
RUN adduser --disabled-password --gecos "" kramerius
USER kramerius
RUN tar xzf /tmp/tomcat.tar.gz --directory=/home/kramerius/
ENV CATALINA_HOME /home/kramerius/apache-tomcat-7.0.52
ENV FEDORA_HOME /home/kramerius/fedora

#install fedora
ADD fedora-install.properties /home/kramerius/fedora-install.properties
RUN java -jar /tmp/fedora-install.jar /home/kramerius/fedora-install.properties

#install kramerius
RUN wget --no-verbose https://kramerius.googlecode.com/files/Kramerius-4.8.3.zip  -O /tmp/kramerius.zip
RUN mkdir /tmp/kramerius; unzip /tmp/kramerius.zip -d /tmp/kramerius/
RUN mv /tmp/kramerius/Kramerius-4.8.3/*.war  $CATALINA_HOME/webapps/
RUN mv /tmp/kramerius/Kramerius-4.8.3/*.jar  $CATALINA_HOME/lib/


# sleep until tomcat finish starting up (TODO)
USER root
RUN /etc/init.d/postgresql start && su -l kramerius -c "/home/kramerius/apache-tomcat-7.0.52/bin/startup.sh" && sleep 360

# fedora config - alias and resource index
ADD changeFedoraSettings.sh /changeFedoraSettings.sh
RUN /changeFedoraSettings.sh
RUN mv fedora.fcfg $FEDORA_HOME/server/config/fedora.fcfg && rm /changeFedoraSettings.sh

# JAAS
ADD jaas.conf /jaas.conf
RUN cat /jaas.conf >> /home/kramerius/fedora/server/config/jaas.conf && rm /jaas.conf
ENV JAAS_CONFIG /home/kramerius/fedora/server/config/jaas.conf

# SOLR
RUN wget --no-verbose https://kramerius.googlecode.com/files/installation-4.8.zip -O /tmp/installation-4.8.zip
RUN mkdir /tmp/kramerius-instalaltion;  unzip /tmp/installation-4.8.zip -d /tmp/
USER kramerius
RUN cp /tmp/installation-4.8/solr.war /home/kramerius/apache-tomcat-7.0.52/webapps/solr.war
RUN cp -r /tmp/installation-4.8/solr /home/kramerius/

ENV SOLR_HOME /home/kramerius/solr
ENV JAVA_OPTS -Djava.awt.headless=true -XX:MaxPermSize=256m -Xms1024m -Xmx3072m  -Djava.security.auth.login.config=$JAAS_CONFIG -Dsolr.solr.home=$SOLR_HOME

# APACHE
USER root
RUN a2enmod rewrite && a2enmod jk
ADD workers.properties /etc/libapache2-mod-jk/workers.properties
ADD kramerius-site /etc/apache2/sites-enabled/000-default


EXPOSE 80
ENTRYPOINT /etc/init.d/postgresql start && /etc/init.d/apache2 start && su -l kramerius -c "/home/kramerius/apache-tomcat-7.0.52/bin/startup.sh" && /bin/bash
