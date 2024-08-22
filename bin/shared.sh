#!/bin/bash

fetchSettings() {
  if [ "$GITHUB_USERNAME" == "" ]; then
    echo 'Environment variable $GITHUB_USERNAME must be set'
    exit 1
  fi
  if [ "$GITHUB_TOKEN" == "" ]; then
    echo 'Environment variable $GITHUB_TOKEN must be set'
    exit 1
  fi
  echo "Github credentials for $GITHUB_USERNAME found in environment"

  echo "Fetching settings.xml" \
     && curl --no-progress-meter -O https://raw.githubusercontent.com/VEuPathDB/maven-release-tools/main/settings.xml
}

buildUberJar() {
  # only build uber-jar if necessary
  if [ ! -e Server/target/uber-jar.jar ]; then

    # tell the user what we're doing
    echo "Project not built; this will take a few moments (script will be faster next time)..."

    fetchSettings

    # build the entire project to cache snapshot deps in local mvn repo
    echo "Building code..."
    mvn --settings ./settings.xml clean install &> /dev/null

    # package the server component into an uber jar
    cd Server
    echo "Building uber jar..."
    mvn --settings ../settings.xml clean package assembly:single &> /dev/null
    cd ..

    rm settings.xml
  fi
}

createHmacValue() {
  # generate a random value of sufficient length
  java -classpath Server/target/uber-jar.jar org.gusdb.oauth2.tools.KeyGenerator HmacSHA512
}
