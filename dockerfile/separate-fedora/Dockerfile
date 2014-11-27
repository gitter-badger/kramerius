FROM debian:wheezy

MAINTAINER Martin Rumanek <martin@rumanek.cz>

ENV REFRESHED_AT 2014-11-29

## install packages
ENV PATH /usr/bin:/bin:/usr/sbin:/sbin
ENV DEBIAN_FRONTEND noninteractive
RUN apt-get -yqq  update && \
apt-get -y install wget openjdk-7-jre-headless postgresql apache2 libapache2-mod-jk vim unzip xmlstarlet curl less libfcgi0ldbl libjpeg62 libmemcached10 libtiff4 libapache2-mod-fcgid htop openssh-server


# download binaries
RUN wget --no-verbose http://apache.mirrors.lucidnetworks.net/tomcat/tomcat-7/v7.0.57/bin/apache-tomcat-7.0.57.tar.gz -O /tmp/tomcat.tar.gz && \
    wget --no-verbose http://downloads.sourceforge.net/fedora-commons/fcrepo-installer-3.6.2.jar -O /tmp/fedora-install.jar && \
    wget --no-verbose https://kramerius.googlecode.com/files/installation-4.8.zip -O /tmp/installation-4.8.zip 


# postgres
RUN sed -i 's/local   all             all                                     peer/local   all             all                                     md5/' \
  /etc/postgresql/9.1/main/pg_hba.conf && \
  /etc/init.d/postgresql start ; su -l postgres -c "createuser fedoraAdmin --no-superuser --no-createdb --no-createrole; \
 createdb fedora3 --owner=fedoraAdmin; createdb riTriples --owner=fedoraAdmin; \
 psql --command \"alter user \\\"fedoraAdmin\\\" with password 'fedoraAdmin';\" "; \
 /etc/init.d/postgresql stop

# install tomcat
RUN adduser --disabled-password --gecos "" kramerius
USER kramerius
RUN tar xzf /tmp/tomcat.tar.gz --directory=/home/kramerius/ && mv /home/kramerius/apache-tomcat-* /home/kramerius/tomcat
ENV CATALINA_HOME /home/kramerius/tomcat
ENV FEDORA_HOME /home/kramerius/fedora

# install fedora
RUN unzip /tmp/installation-4.8.zip -d /tmp/
ADD fedora-install.properties /home/kramerius/fedora-install.properties
RUN java -jar /tmp/fedora-install.jar /home/kramerius/fedora-install.properties

# script for wait until is some service started
USER root
ADD wait.sh /wait.sh

#  sleep until tomcat finish starting up
RUN /etc/init.d/postgresql start; su -l kramerius -c "/home/kramerius/tomcat/bin/startup.sh"; /wait.sh "http://localhost:8080/"; \
    /etc/init.d/postgresql stop


# fedora config - alias and resource index
USER kramerius
ADD changeFedoraSettings.sh /home/kramerius/changeFedoraSettings.sh

USER root

RUN /home/kramerius/changeFedoraSettings.sh && \
    mv /home/kramerius/fedora.fcfg $FEDORA_HOME/server/config/fedora.fcfg && rm /home/kramerius/changeFedoraSettings.sh && \
    rm $FEDORA_HOME/data/fedora-xacml-policies/repository-policies/default/deny-apim-if-not-localhost.xml && \
    rm $FEDORA_HOME/data/fedora-xacml-policies/repository-policies/default/deny-unallowed-file-resolution.xml

# context datasource, jdbc lib
RUN wget --no-verbose http://jdbc.postgresql.org/download/postgresql-9.1-903.jdbc4.jar -O /home/kramerius/tomcat/lib/postgresql-9.1-903.jdbc4.jar

ENV JAVA_OPTS -Djava.awt.headless=true -Dfile.encoding=UTF8 -XX:MaxPermSize=256m -Xms1024m -Xmx3072m

# APACHE
USER root
RUN a2enmod rewrite && a2enmod jk
ADD workers.properties /etc/libapache2-mod-jk/workers.properties
ADD kramerius-site /etc/apache2/sites-enabled/000-default

ADD fedoraIngestModels.sh /fedoraIngestModels.sh
RUN /etc/init.d/postgresql start; su -l kramerius -c "/home/kramerius/tomcat/bin/startup.sh"; \
    /wait.sh "http://localhost:8080/fedora"; /fedoraIngestModels.sh; /etc/init.d/postgresql stop


ADD init.sh /init.sh
ADD parser.sh /parser.sh

RUN sed "s/data_directory = '\/var\/lib\/postgresql\/9.1\/main'/data_directory = '\/kramerius-data\/postgres-data\/main'/" /etc/postgresql/9.1/main/postgresql.conf > /tmp/postgresql.conf
RUN mv /tmp/postgresql.conf /etc/postgresql/9.1/main/postgresql.conf
RUN mv /home/kramerius/fedora/data /home/kramerius/fedora/dataDefault && ln -s  /kramerius-data/fedora-data /home/kramerius/fedora/data

