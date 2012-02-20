#!/bin/sh
# -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=1044 -Xms256m -Xmx1024m -XX:PermSize=64M -XX:MaxPermSize=256M
java -jar target/jwat-tools-0.2.0-SNAPSHOT-jar-with-dependencies.jar $@
