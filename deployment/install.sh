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


BWHC_APP_DIR="BWHCAPPPLACEHOLDER"  # Value 'BWHCAPPPLACEHOLDER' in this script template
                                   # is replaced by actual value upon packaging

BWHC_ZIP="$BWHC_APP_DIR.zip"

PRODUCTION_CONF="production.conf"

SERVICE_SCRIPT="bwhc-backend-service"

CONFIG="config"

BWHC_CONNECTOR_CONFIG="bwhcConnectorConfig.xml"

LOGBACK="logback.xml"




cp $BWHC_ZIP "$TARGET_DIR/"

cp $SERVICE_SCRIPT "$TARGET_DIR/"



if [ ! -f "$TARGET_DIR/$CONFIG" ]; then

  echo "Copying $CONFIG ..."

  cp $CONFIG "$TARGET_DIR/"

fi


if [ ! -f "$TARGET_DIR/$PRODUCTION_CONF" ]; then

  echo "Copying $PRODUCTION_CONF ..."

  cp $PRODUCTION_CONF "$TARGET_DIR/"

fi


if [ ! -f "$TARGET_DIR/$BWHC_CONNECTOR_CONFIG" ]; then

  echo "Copying $BWHC_CONNECTOR_CONFIG ..."

  cp $BWHC_CONNECTOR_CONFIG "$TARGET_DIR/"

fi


if [ ! -f "$TARGET_DIR/$LOGBACK" ]; then
  
  echo "Copying $LOGBACK ..."

  cp $LOGBACK "$TARGET_DIR/"

fi



cd "$TARGET_DIR"


if [ -d "$BWHC_APP_DIR" ]; then

  echo " Removing previous bwHC backend app"

  rm -r $BWHC_APP_DIR

fi

unzip $BWHC_ZIP

