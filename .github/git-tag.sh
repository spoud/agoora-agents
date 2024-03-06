#!/bin/bash

TAG=`git describe --tags --exclude "agoora-*" --abbrev=8`

echo "git-tag=${TAG}" >> $GITHUB_OUTPUT
