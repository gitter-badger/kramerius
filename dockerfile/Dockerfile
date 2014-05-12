FROM debian:wheezy

MAINTAINER Martin Rumanek <martin@rumanek.cz>

# install packages
ENV PATH /usr/bin:/bin:/usr/sbin:/sbin
ENV DEBIAN_FRONTEND noninteractive
RUN apt-get -y  update && \
apt-get -y install wget openjdk-7-jre-headless postgresql apache2 libapache2-mod-jk vim unzip xmlstarlet curl less libfcgi0ldbl libjpeg62 libmemcached10 libtiff4 libapache2-mod-fcgid htop openssh-server

# download binaries
RUN wget --no-verbose http://apache.mirrors.lucidnetworks.net/tomcat/tomcat-7/v7.0.53/bin/apache-tomcat-7.0.53.tar.gz -O /tmp/tomcat.tar.gz && \
    wget --no-verbose http://downloads.sourceforge.net/fedora-commons/fcrepo-installer-3.6.2.jar -O /tmp/fedora-install.jar && \
    wget --no-verbose http://sourceforge.net/projects/kramerius/files/Kramerius-5.0.beta1.zip/download -O /tmp/kramerius.zip && \
    wget --no-verbose https://kramerius.googlecode.com/files/installation-4.8.zip -O /tmp/installation-4.8.zip

# postgres
RUN sed 's/local   all             all                                     peer/local   all             all                                     md5/' \
  /etc/postgresql/9.1/main/pg_hba.conf > pg_hba.conf && \
    mv pg_hba.conf /etc/postgresql/9.1/main/pg_hba.conf && \
  /etc/init.d/postgresql start && su -l postgres -c "createuser fedoraAdmin --no-superuser --no-createdb --no-createrole; \
 createdb fedora3 --owner=fedoraAdmin; createdb kramerius4 --owner=fedoraAdmin; createdb riTriples --owner=fedoraAdmin; \
 psql --command \"alter user \\\"fedoraAdmin\\\" with password 'fedoraAdmin';\" "

# install tomcat
RUN adduser --disabled-password --gecos "" kramerius
USER kramerius
RUN tar xzf /tmp/tomcat.tar.gz --directory=/home/kramerius/ && mv /home/kramerius/apache-tomcat-* /home/kramerius/tomcat
ENV CATALINA_HOME /home/kramerius/tomcat
ENV FEDORA_HOME /home/kramerius/fedora

#install fedora
ADD fedora-install.properties /home/kramerius/fedora-install.properties
RUN java -jar /tmp/fedora-install.jar /home/kramerius/fedora-install.properties

#install kramerius
RUN mkdir /tmp/kramerius; unzip /tmp/kramerius.zip -d /tmp/kramerius/ && \
    mv /tmp/kramerius/Kramerius-5.0.beta1/*.war  $CATALINA_HOME/webapps/ && \
    mv /tmp/kramerius/Kramerius-5.0.beta1/*.jar  $CATALINA_HOME/lib/


# sleep until tomcat finish starting up (TODO)
USER root
RUN /etc/init.d/postgresql start && su -l kramerius -c "/home/kramerius/tomcat/bin/startup.sh" && sleep 360


# fedora config - alias and resource index
USER kramerius
ADD changeFedoraSettings.sh /home/kramerius/changeFedoraSettings.sh
RUN /home/kramerius/changeFedoraSettings.sh && \
    mv /home/kramerius/fedora.fcfg $FEDORA_HOME/server/config/fedora.fcfg && rm /home/kramerius/changeFedoraSettings.sh && \
    rm $FEDORA_HOME/data/fedora-xacml-policies/repository-policies/default/deny-apim-if-not-localhost.xml && \
    rm $FEDORA_HOME/data/fedora-xacml-policies/repository-policies/default/deny-unallowed-file-resolution.xml

# context datasource, jdbc lib
RUN wget --no-verbose http://jdbc.postgresql.org/download/postgresql-9.1-903.jdbc4.jar -O /home/kramerius/tomcat/lib/postgresql-9.1-903.jdbc4.jar
ADD search.xml /home/kramerius/tomcat/conf/Catalina/localhost/search.xml
ADD rightseditor.xml /home/kramerius/tomcat/conf/Catalina/localhost/rightseditor.xml

# JAAS
ADD jaas.conf /home/kramerius/fedora/server/config/jaas-kramerius.conf 
RUN cat /home/kramerius/fedora/server/config/jaas-kramerius.conf >> /home/kramerius/fedora/server/config/jaas.conf
ENV JAAS_CONFIG /home/kramerius/fedora/server/config/jaas.conf

# SOLR
USER root
RUN unzip /tmp/installation-4.8.zip -d /tmp/
USER kramerius
RUN cp /tmp/installation-4.8/solr.war /home/kramerius/tomcat/webapps/solr.war && \
    cp -r /tmp/installation-4.8/solr /home/kramerius/

ENV SOLR_HOME /home/kramerius/solr
ENV JAVA_OPTS -Djava.awt.headless=true -XX:MaxPermSize=256m -Xms1024m -Xmx3072m  -Djava.security.auth.login.config=$JAAS_CONFIG -Dsolr.solr.home=$SOLR_HOME

# APACHE
USER root
RUN a2enmod rewrite && a2enmod jk
ADD workers.properties /etc/libapache2-mod-jk/workers.properties
ADD kramerius-site /etc/apache2/sites-enabled/000-default

ADD fedoraIngestModels.sh /fedoraIngestModels.sh
RUN /etc/init.d/postgresql start && su -l kramerius -c "/home/kramerius/tomcat/bin/startup.sh" && sleep 90 && /fedoraIngestModels.sh

RUN wget --no-verbose https://dl.dropbox.com/s/7k24kkvqf1pafec/iipimage-jp2-amd64.deb -O /tmp/iipimage-jp2-amd64.deb
RUN dpkg -i /tmp/iipimage-jp2-amd64.deb 

ADD migration.properties /home/kramerius/.kramerius4/migration.properties
ADD configuration.properties /home/kramerius/.kramerius4/configuration.properties
ADD initData.sh /initData.sh
ADD parser.sh /parser.sh

RUN sed "s/data_directory = '\/var\/lib\/postgresql\/9.1\/main'/data_directory = '\/kramerius-data\/postgres-data\/main'/" /etc/postgresql/9.1/main/postgresql.conf > /tmp/postgresql.conf
RUN mv /tmp/postgresql.conf /etc/postgresql/9.1/main/postgresql.conf
RUN mv /home/kramerius/fedora/data /home/kramerius/fedora/dataDefault && ln -s  /kramerius-data/fedora-data /home/kramerius/fedora/data 
RUN mv /home/kramerius/.kramerius4 /home/kramerius/.kramerius4Default && ln -s /kramerius-data/.kramerius4 /home/kramerius/.kramerius4
RUN mv /home/kramerius/solr/data /home/kramerius/solr/dataDefault && ln -s /kramerius-data/solr-data/ /home/kramerius/solr/data

ENTRYPOINT /initData.sh && /etc/init.d/postgresql start && /etc/init.d/apache2 start && su -l kramerius -c "/home/kramerius/tomcat/bin/startup.sh" && /bin/bash
