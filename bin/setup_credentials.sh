#!/bin/bash
set -ev

# This script is designed to be run from automated build tools (Travis CI, Samson)
echo "realm=Artifactory Realm
host=zdrepo.jfrog.io
user=${ARTIFACTORY_USERNAME}
password=${ARTIFACTORY_KEY}" > "${PWD}/.credentials"

# Ignore changes to sbtopts
grep -F '.sbtopts' ${PWD}/.git/info/exclude > /dev/null || echo ".sbtopts" >> ${PWD}/.git/info/exclude
# Modify .sbtopts to include new credentials file
grep -F 'sbt.boot.credentials' ${PWD}/.sbtopts > /dev/null || echo "-Dsbt.boot.credentials=${PWD}/.credentials" >> ${PWD}/.sbtopts

echo "Created ${PWD}/.credentials"
