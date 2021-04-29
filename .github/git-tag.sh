#!/bin/bash

TAG=`git describe --tags --exclude "agoora-*" --abbrev=8`


echo ::set-output name=git-tag::"$TAG"
