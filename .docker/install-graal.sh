#!/bin/bash

source ~/.sdkman/bin/sdkman-init.sh
VERSION=$(sdk ls java | grep grl | head -1 | xargs -n 1 echo | tail -1)
yes | sdk i java $VERSION