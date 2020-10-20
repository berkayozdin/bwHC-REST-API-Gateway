#!/bin/bash



TARGET_DIR=""


if [ "$#" -eq "0" ]; then

  echo "ERROR: Enter a target directory for bwHC backend installation"
 
  exit 1

else 

  TARGET_DIR="$1"

fi


if [ ! -d "$TARGET_DIR" ]; then

  mkdir -p "$TARGET_DIR"

fi



BWHC_APP_DIR="bwhc-rest-api-gateway-1.0-SNAPSHOT"

BWHC_ZIP="$BWHC_APP_DIR.zip"

PRODUCTION_CONF="production.conf"

SERVICE_SCRIPT="bwhc-backend-service"

CONFIG="config"

BWHC_CONNECTOR_CONFIG="bwhcConnectorConfig.xml"

LOGBACK="logback.xml"



cp $PRODUCTION_CONF "$TARGET_DIR/"

cp $BWHC_ZIP "$TARGET_DIR/"

cp $SERVICE_SCRIPT "$TARGET_DIR/"



if [ ! -f "$TARGET_DIR/$CONFIG" ]; then

  cp $CONFIG "$TARGET_DIR/"

fi


if [ ! -f "$TARGET_DIR/$BWHC_CONNECTOR_CONFIG" ]; then

  cp $BWHC_CONNECTOR_CONFIG "$TARGET_DIR/"

fi


if [ ! -f "$TARGET_DIR/$LOGBACK" ]; then

  cp $LOGBACK "$TARGET_DIR/"

fi



cd "$TARGET_DIR"


if [ -d "$BWHC_APP_DIR" ]; then

  echo "-------------------------------------------------------------------------"
  echo " Removing previous bwHC service installation"
  echo "-------------------------------------------------------------------------"

  rm -r $BWHC_APP_DIR

fi

unzip $BWHC_ZIP

