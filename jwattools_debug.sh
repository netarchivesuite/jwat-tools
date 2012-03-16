#!/bin/sh
java -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=1044 -Xms256m -Xmx1024m -XX:PermSize=64M -XX:MaxPermSize=256M -jar target/jwat-tools-0.3.0-SNAPSHOT-jar-with-dependencies.jar $@
