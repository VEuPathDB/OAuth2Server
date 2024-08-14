#!/bin/bash

# record the current directory for output
RUN_DIR=$(pwd)

# get to the parent directory of the bin dir
PROJECT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )/..
cd $PROJECT_DIR

# only build uber-jar if necessary
if [ ! -e Server/target/uber-jar.jar ]; then

  # tell the user what we're doing
  echo "Project not built; this will take a few moments..."
  echo

  # build the entire project to cache snapshot deps in local mvn repo
  mvn clean install &> /dev/null

  # package the server component into an uber jar
  cd Server
  mvn clean package assembly:single &> /dev/null
  cd ..

fi

# generate a random value of sufficient length
java -classpath Server/target/uber-jar.jar org.gusdb.oauth2.tools.KeyGenerator HmacSHA512
