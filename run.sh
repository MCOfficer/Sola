#!/bin/bash

exit=0
while [ $exit -eq 0 ]
do
  git pull
  gradle shadowJar
  mv build/libs/Sola-1.0-SNAPSHOT-all.jar Sola.jar -f
  java -jar Sola.jar
  if [ $? -ne 133 ]
    then
      exit=1
  fi
done
