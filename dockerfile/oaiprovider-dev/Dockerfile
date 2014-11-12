FROM debian:wheezy

MAINTAINER Martin Rumanek <martin@rumanek.cz>

ENV REFRESHED_AT 2014-09-11

## install packages
ENV PATH /usr/bin:/bin:/usr/sbin:/sbin
ENV DEBIAN_FRONTEND noninteractive
RUN apt-get -yqq  update && \
apt-get -y install wget openjdk-7-jre-headless postgresql vim unzip curl less 


# download binaries
RUN wget --no-verbose http://apache.mirrors.lucidnetworks.net/tomcat/tomcat-7/v7.0.56/bin/apache-tomcat-7.0.56.tar.gz -O /tmp/tomcat.tar.gz && \
    wget --no-verbose http://downloads.sourceforge.net/fedora-commons/oaiprovider-1.2.2.zip -O /tmp/oaiprovider.zip

# postgres
RUN sed 's/local   all             all                                     peer/local   all             all                                     md5/' \
  /etc/postgresql/9.1/main/pg_hba.conf > pg_hba.conf ; \
    mv pg_hba.conf /etc/postgresql/9.1/main/pg_hba.conf ; \
  /etc/init.d/postgresql start ; su -l postgres -c "createuser proai --no-superuser --no-createdb --no-createrole; \
 createdb proai --owner=proai; \
 psql --command \"alter user \\\"proai\\\" with password 'proai';\" "; \
 /etc/init.d/postgresql start

# install tomcat
RUN adduser --disabled-password --gecos "" oaiprovider
USER oaiprovider
RUN tar xzf /tmp/tomcat.tar.gz --directory=/home/oaiprovider/ && mv /home/oaiprovider/apache-tomcat-* /home/oaiprovider/tomcat
ENV CATALINA_HOME /home/oaiprovider/tomcat

# unpack oaiprovider
RUN unzip /tmp/oaiprovider.zip -d /tmp && \
    mv /tmp/oaiprovider-1.2.2/oaiprovider.war /home/oaiprovider/tomcat/webapps/

# script for wait until is some service started
USER root
ADD wait.sh /wait.sh

#  sleep until tomcat finish starting up
RUN /etc/init.d/postgresql start; su -l oaiprovider -c "/home/oaiprovider/tomcat/bin/startup.sh"; /wait.sh "http://localhost:8080/"; \
    /etc/init.d/postgresql stop

ADD proai.properties /home/oaiprovider/tomcat/webapps/oaiprovider/WEB-INF/classes/proai.properties

# context datasource, jdbc lib
RUN wget --no-verbose http://jdbc.postgresql.org/download/postgresql-9.1-903.jdbc4.jar -O /home/oaiprovider/tomcat/lib/postgresql-9.1-903.jdbc4.jar

ENV JAVA_OPTS -Djava.awt.headless=true -Dfile.encoding=UTF8 -XX:MaxPermSize=256m -Xms256m -Xmx1024m 

ADD init.sh /init.sh

# comment this out if you want to disable debug mode
ENV CATALINA_OPTS "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=8000"

RUN sed "s/data_directory = '\/var\/lib\/postgresql\/9.1\/main'/data_directory = '\/oaiprovider-data\/postgres-data\/main'/" /etc/postgresql/9.1/main/postgresql.conf > /tmp/postgresql.conf
RUN mv /tmp/postgresql.conf /etc/postgresql/9.1/main/postgresql.conf


