#!/bin/sh
ProgDir=`dirname "$0"`
. "${ProgDir}/env.sh"

if [ -z "${JAVA_OPTS}" ]; then
  JAVA_OPTS="-Xms256m -Xmx1024m -XX:PermSize=64M -XX:MaxPermSize=256M"
fi

"${JAVA}" ${JAVA_OPTS} -cp "$CP" org.jwat.tools.JWATTools $@
