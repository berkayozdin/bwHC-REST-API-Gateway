#!/bin/bash



BWHC_DIR=./bwhc-rest-api-gateway-1.0-SNAPSHOT

BWHC_ZIP=$BWHC_DIR.zip



if [ -d "$BWHC_DIR" ]; then

  echo "-------------------------------------------------------------------------"
  echo " Removing previous bwHC service installation"
  echo "-------------------------------------------------------------------------"

  rm -r $BWHC_DIR

fi

unzip $BWHC_ZIP

