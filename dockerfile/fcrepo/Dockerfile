FROM tomcat:7

MAINTAINER Martin Rumanek <martin@rumanek.cz>

ENV REFRESHED_AT 2014-11-29
ENV FCREPO_DOWNLOAD_URL http://downloads.sourceforge.net/fedora-commons/fcrepo-installer-3.6.2.jar
ENV JDBC_DRIVER_DOWNLOAD_URL http://jdbc.postgresql.org/download/postgresql-9.1-903.jdbc4.jar

## install packages
RUN apt-get -yqq  update \
  && apt-get -y install xmlstarlet

# download binaries
#RUN wget --no-verbose http://downloads.sourceforge.net/fedora-commons/fcrepo-installer-3.6.2.jar -O /tmp/fedora-install.jar && \
#    wget --no-verbose https://kramerius.googlecode.com/files/installation-4.8.zip -O /tmp/installation-4.8.zip 


# postgres
#RUN sed -i 's/local   all             all                                     peer/local   all             all                                     md5/' \
#  /etc/postgresql/9.1/main/pg_hba.conf && \
#  /etc/init.d/postgresql start ; su -l postgres -c "createuser fedoraAdmin --no-superuser --no-createdb --no-createrole; \
# createdb fedora3 --owner=fedoraAdmin; createdb riTriples --owner=fedoraAdmin; \
# psql --command \"alter user \\\"fedoraAdmin\\\" with password 'fedoraAdmin';\" "; \
# /etc/init.d/postgresql stop

WORKDIR $CATALINA_HOME
RUN curl -sL "$JDBC_DRIVER_DOWNLOAD_URL" -o lib/postgresql-9.1-903.jdbc4.jar

ENV FEDORA_HOME /usr/local/fedora
#
## install fedora
#RUN unzip /tmp/installation-4.8.zip -d /tmp/
ADD fedora-install.properties /home/kramerius/fedora-install.properties
RUN  curl -sL  "$FCREPO_DOWNLOAD_URL" -o fedora-install.jar && \
     java -jar fedora-install.jar /home/kramerius/fedora-install.properties


RUN xmlstarlet ed -L -N x="http://www.fedora.info/definitions/1/0/config/" \
  -s "//x:module[@role='org.fcrepo.server.resourceIndex.ResourceIndex']" -t elem -n "paramKrameriusTMP" -v "" \
  -s "//x:module[@role='org.fcrepo.server.resourceIndex.ResourceIndex']" -t elem -n "paramOAITMP" -v "" \
  -i "//paramKrameriusTMP" -t attr -n "name" -v "alias:kramerius" \
  -i "//paramKrameriusTMP" -t attr -n "value" -v "http://www.nsdl.org/ontologies/relationships#" \
  -s "//paramKrameriusTMP" -t elem -n "comment" -v "Alias for Kramerius" \
  -r "//paramKrameriusTMP" -v "param" \
  -i "//paramOAITMP" -t attr -n "name" -v "alias:oai" \
  -i "//paramOAITMP" -t attr -n "value" -v "http://www.openarchives.org/OAI/2.0/" \
  -s "//paramOAITMP" -t elem -n "comment" -v "Alias for OAI" \
  -r "//paramOAITMP" -v "param" \
  -u "//x:module[@role='org.fcrepo.server.resourceIndex.ResourceIndex']/x:param[@name='datastore']/@value" -v "localPostgresMPTTriplestore" \
  $FEDORA_HOME/server/config/fedora.fcfg \
  && rm $FEDORA_HOME/data/fedora-xacml-policies/repository-policies/default/deny-apim-if-not-localhost.xml \
     $FEDORA_HOME/data/fedora-xacml-policies/repository-policies/default/deny-unallowed-file-resolution.xml

#ADD fedoraIngestModels.sh /fedoraIngestModels.sh
#RUN /etc/init.d/postgresql start; su -l kramerius -c "/home/kramerius/tomcat/bin/startup.sh"; \
#    /wait.sh "http://localhost:8080/fedora"; /fedoraIngestModels.sh; /etc/init.d/postgresql stop
