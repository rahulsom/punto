#!/bin/bash

source ~/.sdkman/bin/sdkman-init.sh
VERSION=$(sdk ls java | xargs -n 1 echo | grep grl | head -1)
yes | sdk i java $VERSION