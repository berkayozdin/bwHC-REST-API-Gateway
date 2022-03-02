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

FILES=(
  "config"
  "production.conf"
  "bwhc-backend-service"
  "bwhcConnectorConfig.xml"
  "logback.xml"
)


cp $BWHC_ZIP "$TARGET_DIR/"


for FILE in "${FILES[@]}"; do

  if [ ! -f "$TARGET_DIR/$FILE" ]; then

    echo "Copying $FILE ..."

    cp $FILE "$TARGET_DIR/"
  fi

done



cd "$TARGET_DIR"


if [ -d "$BWHC_APP_DIR" ]; then

  echo " Removing previous bwHC backend app"

  rm -r $BWHC_APP_DIR

fi

unzip $BWHC_ZIP

