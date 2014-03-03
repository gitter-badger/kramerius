FROM debian

MAINTAINER Martin Rumanek <martin@rumanek.cz>

# install packages
RUN apt-get -y  update
RUN apt-get -y install wget openjdk-7-jre-headless postgresql apache2 libapache2-mod-jk vim unzip xmlstarlet curl

# download binaries
RUN wget --no-verbose http://mirror.hosting90.cz/apache/tomcat/tomcat-7/v7.0.52/bin/apache-tomcat-7.0.52.tar.gz -O /tmp/tomcat.tar.gz && \
    wget --no-verbose http://downloads.sourceforge.net/project/fedora-commons/fedora/3.6.1/fcrepo-installer-3.6.1.jar?r=\&ts=1393867971\&use_mirror=netcologne -O /tmp/fedora-install.jar && \
    wget --no-verbose https://kramerius.googlecode.com/files/Kramerius-4.8.3.zip  -O /tmp/kramerius.zip && \
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
RUN tar xzf /tmp/tomcat.tar.gz --directory=/home/kramerius/
ENV CATALINA_HOME /home/kramerius/apache-tomcat-7.0.52
ENV FEDORA_HOME /home/kramerius/fedora

#install fedora
ADD fedora-install.properties /home/kramerius/fedora-install.properties
RUN java -jar /tmp/fedora-install.jar /home/kramerius/fedora-install.properties

#install kramerius
RUN mkdir /tmp/kramerius; unzip /tmp/kramerius.zip -d /tmp/kramerius/ && \
    mv /tmp/kramerius/Kramerius-4.8.3/*.war  $CATALINA_HOME/webapps/ && \
    mv /tmp/kramerius/Kramerius-4.8.3/*.jar  $CATALINA_HOME/lib/


# sleep until tomcat finish starting up (TODO)
USER root
RUN /etc/init.d/postgresql start && su -l kramerius -c "/home/kramerius/apache-tomcat-7.0.52/bin/startup.sh" && sleep 360

# fedora config - alias and resource index
ADD changeFedoraSettings.sh /changeFedoraSettings.sh
RUN /changeFedoraSettings.sh && \
    mv fedora.fcfg $FEDORA_HOME/server/config/fedora.fcfg && rm /changeFedoraSettings.sh && \
    rm $FEDORA_HOME/data/fedora-xacml-policies/repository-policies/default/deny-apim-if-not-localhost.xml && \
    rm $FEDORA_HOME/data/fedora-xacml-policies/repository-policies/default/deny-unallowed-file-resolution.xml

# JAAS
ADD jaas.conf /jaas.conf
RUN cat /jaas.conf >> /home/kramerius/fedora/server/config/jaas.conf && rm /jaas.conf
ENV JAAS_CONFIG /home/kramerius/fedora/server/config/jaas.conf

# SOLR
RUN mkdir /tmp/kramerius-instalaltion && unzip /tmp/installation-4.8.zip -d /tmp/
USER kramerius
RUN cp /tmp/installation-4.8/solr.war /home/kramerius/apache-tomcat-7.0.52/webapps/solr.war && \
    cp -r /tmp/installation-4.8/solr /home/kramerius/

ENV SOLR_HOME /home/kramerius/solr
ENV JAVA_OPTS -Djava.awt.headless=true -XX:MaxPermSize=256m -Xms1024m -Xmx3072m  -Djava.security.auth.login.config=$JAAS_CONFIG -Dsolr.solr.home=$SOLR_HOME

# APACHE
USER root
RUN a2enmod rewrite && a2enmod jk
ADD workers.properties /etc/libapache2-mod-jk/workers.properties
ADD kramerius-site /etc/apache2/sites-enabled/000-default

ADD fedoraIngestModels.sh /fedoraIngestModels.sh
RUN /etc/init.d/postgresql start && su -l kramerius -c "/home/kramerius/apache-tomcat-7.0.52/bin/startup.sh" && sleep 90 && /fedoraIngestModels.sh

ADD initData.sh /initData.sh

RUN apt-get -y install libfcgi0ldbl libjpeg62 libmemcached10 libtiff4 libapache2-mod-fcgid
RUN wget --no-verbose https://dl.dropbox.com/s/7k24kkvqf1pafec/iipimage-jp2-amd64.deb -O /tmp/iipimage-jp2-amd64.deb
RUN dpkg -i /tmp/iipimage-jp2-amd64.deb 

EXPOSE 80
ENTRYPOINT /initData.sh && /etc/init.d/postgresql start && /etc/init.d/apache2 start && su -l kramerius -c "/home/kramerius/apache-tomcat-7.0.52/bin/startup.sh" && /bin/bash
