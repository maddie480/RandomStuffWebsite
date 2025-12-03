FROM jetty

USER root
RUN apt-get update && apt-get upgrade -y && apt-get clean \
  && usermod -u 1001 ubuntu && groupmod -g 1001 ubuntu \
  && usermod -u 1000 jetty && groupmod -g 1000 jetty \
  && (find / -user 999 -exec chown -c jetty {} \; || echo ok) \
  && (find / -group 999 -exec chgrp -c jetty {} \; || echo ok)
USER jetty

COPY target/random-stuff-website-1.0.0.war /var/lib/jetty/webapps/ROOT.war

RUN java -jar "$JETTY_HOME/start.jar" --add-modules=requestlog,console-capture,http-forwarded,ee10-deploy,ee10-jsp,ssl-reload \
  && echo "jetty.console-capture.retainDays=30" >> /var/lib/jetty/start.d/console-capture.ini \
  && echo "jetty.console-capture.append=true" >> /var/lib/jetty/start.d/console-capture.ini \
  && echo "jetty.requestlog.retainDays=30" >> /var/lib/jetty/start.d/requestlog.ini \
  && echo 'jetty.requestlog.formatString=%{client}a - %u %{dd/MMM/yyyy:HH:mm:ss ZZZ|GMT}t "%r" %s %IB/%OB %{ms}Tms "%{Referer}i" "%{User-Agent}i"' >> /var/lib/jetty/start.d/requestlog.ini \
  && echo "jetty.requestlog.append=true" >> /var/lib/jetty/start.d/requestlog.ini \
  && echo "jetty.deploy.scanInterval=0" >> /var/lib/jetty/start.d/ee10-deploy.ini \
  && echo "jetty.sslContext.reload.scanInterval=1" >> /var/lib/jetty/start.d/ssl-reload.ini \
  && mkdir /var/lib/jetty/etc \
  && echo '<Configure id="sslContextFactory" class="org.eclipse.jetty.util.ssl.SslContextFactory$Server">' \
    '\n\t<Set name="KeyStorePath">etc/keystore</Set>\n\t<Set name="KeyStorePassword">storep</Set>' \
    '\n</Configure>' \
    > /var/lib/jetty/etc/jetty-ssl-context.xml

VOLUME /shared
VOLUME /var/lib/jetty/logs
