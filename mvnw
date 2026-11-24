#!/bin/sh
PRG="$0"
while [ -h "$PRG" ] ; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "$PRG"`"/$link"
  fi
done
DIR=`dirname "$PRG"`
if [ -x "$DIR/.tools/maven/bin/mvn" ]; then
  exec "$DIR/.tools/maven/bin/mvn" "$@"
else
  exec mvn "$@"
fi
