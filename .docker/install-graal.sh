#!/bin/bash

source ~/.sdkman/bin/sdkman-init.sh
VERSION=$(sdk ls java | grep grl | head -1 | xargs -n 1 echo | tail -1)
yes | sdk i java $VERSION

VERSION=$(ls -1 ~/.sdkman/candidates/java | grep -v current)
export JAVA_HOME=~/.sdkman/candidates/java/$VERSION
export PATH=$PATH:$JAVA_HOME/bin

gu install native-image
