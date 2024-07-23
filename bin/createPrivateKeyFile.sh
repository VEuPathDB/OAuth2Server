#!/bin/bash

if [ "$#" != "1" ]; then
    # print usage
    echo
    echo "createPrivateKeyFile.sh <passPhrase>"
    echo
    exit 1
fi

passPhrase=$1
outputFile=ecKeys.$( date +%s%3N ).pkcs12

# record the current directory for output
RUN_DIR=$(pwd)

# get to the parent directory of the bin dir
PROJECT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )/..
cd $PROJECT_DIR

# only build uber-jar if necessary
if [ ! -e Server/target/uber-jar.jar ]; then

  # build the entire project to cache snapshot deps in local mvn repo
  mvn clean install

  # package the server component into an uber jar
  cd Server
  mvn clean package assembly:single
  cd ..

fi

# generate a random seed of sufficient length
seed=$( java -classpath Server/target/uber-jar.jar org.gusdb.oauth2.tools.KeyGenerator HmacSHA512 )

# generate the key and write to file
java -classpath Server/target/uber-jar.jar org.gusdb.oauth2.tools.KeyPairWriter $outputFile "$passPhrase" $seed && echo "Wrote key file to $outputFile"
