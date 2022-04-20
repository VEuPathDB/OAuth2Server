#!/bin/bash
##################################################################
##
##  Builds EuPathDB-configured OAuth2 server using custom config
##  file and custom local Maven repo.  Note that use of this
##  script generally requires an internet connection.
##
##  Usage: build.sh [configFile [localMvnRepo]]
##
##  configFile (optional): configuration file for OAuth2 server;
##    if none specified, uses sample config file at
##    EuPathDb/src/main/webapp/WEB-INF/OAuthSampleConfig.json
##
##  localMvnRepo: local maven repository to use instead of the
##    default.  Can only be specified if configuration file is
##    also specified (2-arg execution).  Uses ~/.m2/repository
##    if not specified.
##
##  To download VEuPathDB-authored dependendencies from their
##  Github Packages repo, the following environment variables
##  must be set to a user with the required permission:
##
##  $GITHUB_USERNAME=<github_user>
##  $GITHUB_TOKEN=<github_access_token>
##
##################################################################

# define die for easy exits
die() { echo "$@" 1>&2 ; exit 1; }

# use in case readlink is unavailable on your system
myreadlink() { ( cd "$(dirname "$1")"; echo "$PWD/$(basename "$1")"; ) }

# record start time
startTime=`date +%s`

# check arguments and set up options based on them
if [ "$#" == "1" ]; then
  configFile=$1
  echo "Config file path: $configFile"
  configFileOption="\"-DoauthConfigFile=$configFile\""
elif [ "$#" == "2" ]; then
  configFile=$1
  echo "Config file path: $configFile"
  configFileOption="\"-DoauthConfigFile=$configFile\""
  altMavenRepo=$(myreadlink "$2")
  echo "Custom local Maven repo absolute path: $altMavenRepo"
  altMavenRepoOption="\"-Dmaven.repo.local=$altMavenRepo\""
fi

# find OAuth2Server project dir and go there
scriptDir=$(cd "$(dirname "$0")" && pwd)
cd "$scriptDir/../.."
echo "Found OAuth2Server project at $(pwd)"

# download standard settings.xml
curl -O https://raw.githubusercontent.com/VEuPathDB/base-pom/main/settings.xml

# build server
echo "Building OAuth2Server"
cmd="mvn clean install --settings ./settings.xml $altMavenRepoOption $configFileOption"
echo "$cmd"
$cmd

# remove downloaded settings.xml if present
rm -f settings.xml

# record duration
endTime=`date +%s`
runtimeSecs=$((endTime-startTime))
echo; echo "Done.  This script took $runtimeSecs seconds."
