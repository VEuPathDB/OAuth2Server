#!/bin/bash

if [ "$#" != "1" ]; then
    # print usage
    echo
    echo "createPrivateKeyFile.sh <passPhrase>"
    echo
    exit 1
fi

passPhrase=$1
outputFilename=ecKeys.$( date +%s%3N ).pkcs12
outputFile=$(pwd)/$outputFilename

# get to the parent directory of the bin dir
cd $( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )/..

source bin/shared.sh

buildUberJar

# generate a random seed of sufficient length
seed=$(createHmacValue)

# generate the key and write to file
echo
java -classpath Server/target/uber-jar.jar org.gusdb.oauth2.tools.KeyPairWriter $outputFile "$passPhrase" $seed \
  && echo && echo "Wrote key file to $outputFilename" && echo
