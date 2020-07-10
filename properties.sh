#!/bin/sh

set -ex

if [ '$TRAVIS_PULL_REQUEST' != "false"]; then
  echo "Generating dummy property file for the pull request build"
  touch treetracker.keys.properites
else
  openssl aes-256-cbc -K $encrypted_env_key -iv $encrypted_env_iv -in .env.enc -out fastlane/.env -d
  openssl aes-256-cbc -K $encrypted_props_key -iv $encrypted_props_iv -in treetracker.keys.properties.enc -out treetracker.keys.properties -d
fi
