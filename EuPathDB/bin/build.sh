#!/bin/bash

skipJavaUnitTests=true

# define die for easy exits
die() { echo "$@" 1>&2 ; exit 1; }

# use in case readlink is unavailable on your system
myreadlink() { ( cd $(dirname $1); echo $PWD/$(basename $1); ) }

# record start time
startTime=`date +%s`

# check arguments and set up options based on them
if [ "$#" == "1" ]; then
  configFile=$(myreadlink $1)
  echo "Config file absolute path: $configFile"
  configFileOption="\"-DoauthConfigFile=$configFile\""
elif [ "$#" == "2" ]; then
  configFile=$(myreadlink $1)
  configFileOption="\"-DoauthConfigFile=$configFile\""
  altMavenRepo=$(myreadlink $2)
  altMavenRepoOption="\"-Dmaven.repo.local=$altMavenRepo\""
fi

# find OAuth2Server project dir and go there
scriptDir=$(cd $(dirname "$0") && pwd)
cd $scriptDir/../..
projectDir=$(pwd)
echo "Found OAuth2Server project at $projectDir"

# get latest OAuth code from subversion
echo "Updating OAuth2Server"
svn update || die "Unable to update OAuth2Server codebase to latest"

# see if FgpUtil exists yet; check out or update, then build
if [ -d "../FgpUtil" ]; then
  cd ../FgpUtil
  echo "Updating FgpUtil"
  svn update || die "Unable to update FgpUtil codebase to latest"
else
  cd ..
  echo "Checking out FgpUtil"
  svn co https://www.cbil.upenn.edu/svn/gus/FgpUtil/trunk FgpUtil || die "Unable to check out FgpUtil"
  cd FgpUtil
fi
echo "Building FgpUtil"
mvn clean install $altMavenRepoOption -DskipTests=$skipJavaUnitTests || die "Build of FgpUtil failed.  Cannot build OAuth2Server without FgpUtil"

# build server
cd $projectDir
echo "Building OAuth2Server"
cmd="mvn clean install $altMavenRepoOption -DskipTests=$skipJavaUnitTests $configFileOption"
echo "$cmd"
$cmd

# record duration
endTime=`date +%s`
runtimeSecs=$((endTime-startTime))
echo; echo "Done.  This script took $runtimeSecs seconds."
