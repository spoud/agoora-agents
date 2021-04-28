#!/bin/bash

GIT_TAG=${GIT_TAG:-latest}

rm -Rf binaries
mkdir binaries

for agent in agoora-*-agent; do
  if [ -d "$agent" ]; then
    echo ""
    echo "Processing $agent"

    # Binaries
    cp $agent/target/*-runner binaries/$agent-$GIT_TAG || echo "no binary for $agent"

    # Jar
    cp -R $agent/target/quarkus-app $agent/target/$agent
    tar -czf binaries/$agent-jar-$GIT_TAG.tar.gz --directory=./$agent/target $agent

  fi
done

ls -lah binaries/


