#!/bin/bash
set -eu

# This script will push a previously build ods-provisioning-app:local image with the corresponding tag to dockerhub.
# The tag will be identified based on the passed GIT_REF.
# We only create docker images for master (latest) and releases i.e. refs/heads/1.x, refs/heads/1.1.x, refs/tags/v1.0, refs/tags/v1.1.0

shopt -s extglob

GIT_REF=$1
USER=$2
PASSWORD=$3

echo "GIT_REF=$GIT_REF"

DOCKERTAG='none'

# Examples for GIT_REF to DOCKERTAG mappings
# master             -> latest
# tag    'v.1.0'     -> 1.0
# tag    'v.1.1.0'   -> 1.1.0
# branch '1.x'       -> 1.x
# branch '1.1.x'     -> 1.1.x
# branch 'feature/x' -> none
case $GIT_REF in
  refs/heads/master )
    DOCKERTAG='latest' ;;
  refs/heads/?(+([0-9]).)+([0-9]).x )
  	DOCKERTAG="${GIT_REF/refs\/heads\//}" ;;
  refs/tags/v?(+([0-9]).)+([0-9]).*([0-9]) )
    DOCKERTAG="${GIT_REF/refs\/tags\/v/}" ;;
  refs/tags/?.*(\bsnapshot\b)$.* )
    DOCKERTAG="snapshot" ;;
  * )
    DOCKERTAG='none' ;;
esac
echo "DOCKERTAG=$DOCKERTAG"

if [[ $DOCKERTAG != 'none' ]]; then
  echo "Pushing docker image opendevstackorg/ods-provisioning-app:$DOCKERTAG"

  echo "$PASSWORD" | docker login -u "$USER" --password-stdin
  docker tag ods-provisioning-app:local opendevstackorg/ods-provisioning-app:$DOCKERTAG
  docker push opendevstackorg/ods-provisioning-app:$DOCKERTAG
  docker logout
  rm -f /home/runner/.docker/config.json
else
  echo "NOT pushing a docker image for GIT_REF=$GIT_REF"
fi
