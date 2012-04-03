@echo off
java -Xms256m -Xmx1048m -XX:PermSize=64M -XX:MaxPermSize=256M -jar target\jwat-tools-0.4.0-SNAPSHOT-jar-with-dependencies.jar %*
