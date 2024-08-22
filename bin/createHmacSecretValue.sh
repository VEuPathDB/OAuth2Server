#!/bin/bash

# get to the parent directory of the bin dir
cd $( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )/..

source bin/shared.sh

buildUberJar

echo
createHmacValue
echo
