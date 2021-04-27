#!/bin/bash

TAG=`git describe --tags --exclude "sdm-*" --abbrev=8`


echo ::set-output name=git-tag::"$TAG"
