#!/bin/bash


BWHC_APP_PLACEHOLDER="BWHCAPPPLACEHOLDER"

PCKG_DIR=bwhc-backend

PCKG_ZIP=$PCKG_DIR.zip


TARGET=""

if [ "$#" -eq "0" ]; then
  echo "ERROR: Enter the Play application package to be deployed"
  exit 1
else
  TARGET="$1"
fi

if [ ! -f "$TARGET" ]; then
  echo "ERROR: File $TARGET does not exist!"
  exit 1
fi


BWHC_APP_FILE="${TARGET##*/}"
BWHC_APP_NAME="${BWHC_APP_FILE%.*}"



if [ -d "$PCKG_DIR" ]; then
  rm -r $PCKG_DIR
fi

if [ -d "$PCKG_ZIP" ]; then
  rm $PCKG_ZIP
fi

mkdir $PCKG_DIR


cp $TARGET $PCKG_DIR/

cp install.sh $PCKG_DIR/

cp config $PCKG_DIR/

cp bwhcConnectorConfig.xml $PCKG_DIR/

cp logback.xml  $PCKG_DIR/

cp production.conf $PCKG_DIR/

cp bwhc-backend-service $PCKG_DIR/


REPLACE_COMMAND="s/BWHCAPPPLACEHOLDER/$BWHC_APP_NAME/g"

sed -i $REPLACE_COMMAND $PCKG_DIR/bwhc-backend-service
sed -i $REPLACE_COMMAND $PCKG_DIR/install.sh



//zip --encrypt -r $PCKG_ZIP $PCKG_DIR/
zip -r $PCKG_ZIP $PCKG_DIR/

rm -r $PCKG_DIR
