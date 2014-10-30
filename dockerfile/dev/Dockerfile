FROM moravianlibrary/kramerius

ENV REFRESHED_AT 2014-08-13

ADD tomcat-users.xml /home/kramerius/tomcat/conf/tomcat-users.xml

USER root
RUN wget --no-verbose https://dl.dropbox.com/s/m1zdvcgpp82l1js/kramerius.tar?dl=0 -O tmp/kramerius.tar && \
    tar xf /tmp/kramerius.tar -C /tmp/


USER kramerius
RUN wget --no-verbose https://dl.dropbox.com/s/k527eaq2ubr7me2/client.war?dl=0 -O  $CATALINA_HOME/webapps/client.war


# install kramerius
RUN rm -rf /home/kramerius/tomcat/webapps/search && \
    cp /tmp/kramerius/*.war  $CATALINA_HOME/webapps/ && \
    cp /tmp/kramerius/*.jar  $CATALINA_HOME/lib/

USER root
ADD workers.properties /etc/libapache2-mod-jk/workers.properties
ADD kramerius-site /etc/apache2/sites-enabled/000-default


ENV CATALINA_OPTS "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=8000"
