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
##################################################################

# change to 'false' to have Maven run unit tests
skipJavaUnitTests=true

# change to 'false' to update to latest
skipSvnUpdate=true

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
projectDir="$(pwd)"
echo "Found OAuth2Server project at $projectDir"

# get latest OAuth code from subversion
echo "Updating OAuth2Server"
if [ "$skipSvnUpdate" == "true" ]; then
  echo "...update skipped"
else
  svn update || die "Unable to update OAuth2Server codebase to latest"
fi

# see if FgpUtil exists yet; check out or update, then build
if [ -d "../FgpUtil" ]; then
  cd ../FgpUtil
  echo "Updating FgpUtil"
  if [ "$skipSvnUpdate" == "true" ]; then
    echo "...update skipped"
  else
    svn update || die "Unable to update FgpUtil codebase to latest"
  fi
else
  cd ..
  echo "Checking out FgpUtil"
  svn co https://cbilsvn.pmacs.upenn.edu/svn/gus/FgpUtil/trunk FgpUtil || die "Unable to check out FgpUtil"
  cd FgpUtil
fi
echo "Building FgpUtil"
mvn clean install $altMavenRepoOption -DskipTests=$skipJavaUnitTests || die "Build of FgpUtil failed.  Cannot build OAuth2Server without FgpUtil"

# build server
cd "$projectDir"
echo "Building OAuth2Server"
cmd="mvn clean install $altMavenRepoOption -DskipTests=$skipJavaUnitTests $configFileOption"
echo "$cmd"
$cmd

# record duration
endTime=`date +%s`
runtimeSecs=$((endTime-startTime))
echo; echo "Done.  This script took $runtimeSecs seconds."
