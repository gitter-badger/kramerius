FROM moravianlibrary/kramerius

ENV REFRESHED_AT 2014-08-13

ADD tomcat-users.xml /home/kramerius/tomcat/conf/tomcat-users.xml

USER kramerius
RUN wget --no-verbose https://dl.dropbox.com/s/k527eaq2ubr7me2/client.war?dl=0 -O /home/kramerius/tomcat/webapps/client.war 

USER root
ADD workers.properties /etc/libapache2-mod-jk/workers.properties
ADD kramerius-site /etc/apache2/sites-enabled/000-default


ENV CATALINA_OPTS "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=8000"
