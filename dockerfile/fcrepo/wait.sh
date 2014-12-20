#!/bin/bash

while : ; do
  echo "...waiting...";
  curl --silent --show-error --connect-timeout 1 -I $1;
  if [ $? == 0 ];
  then
    break;
  fi 
  sleep 5
done
echo Ready!
