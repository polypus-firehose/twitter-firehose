FROM ibmjava

ENV LANG=C.UTF-8 \
    LC_ALL=C.UTF-8

RUN mkdir /opt/polypus

COPY \
  dicts/emojis.sg \
  dicts/english.sg \
  dicts/spanish.sg /opt/polypus/dicts/
COPY \
  crawler.conf \
  crawler.jar /opt/polypus/

WORKDIR /opt/polypus
ENTRYPOINT ["java", "-jar", "crawler.jar"]
