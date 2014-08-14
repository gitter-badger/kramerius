FROM moravianlibrary/kramerius

ENV REFRESHED_AT 2014-08-13

ADD tomcat-users.xml /home/kramerius/tomcat/conf/tomcat-users.xml

ENV CATALINA_OPTS "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=8000"
