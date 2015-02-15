FROM debian:wheezy

MAINTAINER Martin Rumanek <martin@rumanek.cz>

ENV REFRESHED_AT 2014-08-06

## install packages
ENV PATH /usr/bin:/bin:/usr/sbin:/sbin
ENV DEBIAN_FRONTEND noninteractive
RUN apt-get -yqq  update && \
apt-get -y install wget openjdk-7-jre-headless postgresql apache2 libapache2-mod-jk vim unzip xmlstarlet curl less libfcgi0ldbl libjpeg62 libmemcached10 libtiff4 libapache2-mod-fcgid htop openssh-server


# download binaries
RUN wget --no-verbose http://mirror.hosting90.cz/apache/tomcat/tomcat-8/v8.0.18/bin/apache-tomcat-8.0.18.tar.gz -O /tmp/tomcat.tar.gz && \
    wget --no-verbose http://sourceforge.net/projects/fedora-commons/files/fedora/3.5/fcrepo-installer-3.5.jar/download -O /tmp/fedora-install.jar && \
    wget --no-verbose https://kramerius.googlecode.com/files/installation-4.8.zip -O /tmp/installation-4.8.zip && \
    mkdir /tmp/kramerius 

RUN echo "test"
# postgres
RUN sed 's/local   all             all                                     peer/local   all             all                                     md5/' \
  /etc/postgresql/9.1/main/pg_hba.conf > pg_hba.conf ; \
    mv pg_hba.conf /etc/postgresql/9.1/main/pg_hba.conf ; \
  /etc/init.d/postgresql start ; su -l postgres -c "createuser fedoraAdmin --no-superuser --no-createdb --no-createrole; \
 createdb fedora3 --owner=fedoraAdmin; createdb riTriples --owner=fedoraAdmin; \
 psql --command \"alter user \\\"fedoraAdmin\\\" with password 'fedoraAdmin';\" "; \
 /etc/init.d/postgresql start

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

# fix bad web.xml in fedora.war
RUN unzip /home/kramerius/tomcat/webapps/fedora.war -d /home/kramerius/tomcat/webapps/fedora
RUN sed -i.bak -e '216,219d' /home/kramerius/tomcat/webapps/fedora/WEB-INF/web.xml
USER root
RUN apt-get -y install  zip
USER kramerius
RUN zip -0 -r /home/kramerius/tomcat/webapps/fedora.war /home/kramerius/tomcat/webapps/fedora
USER root

# script for wait until is some service started
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

