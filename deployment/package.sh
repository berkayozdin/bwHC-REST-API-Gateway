#!/bin/bash


BWHC_APP_PLACEHOLDER="BWHCAPPPLACEHOLDER"

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
BWHC_APP_PREFIX="$(echo $BWHC_APP_NAME | cut -c 1-21)"
BWHC_APP_VERSION="$(echo $BWHC_APP_NAME | cut -c 23-${#BWHC_APP_NAME})"

echo $BWHC_APP_PREFIX
echo $BWHC_APP_VERSION


PCKG_DIR="bwhc-backend-$BWHC_APP_VERSION"

PCKG_ZIP=$PCKG_DIR.zip


if [ -d "$PCKG_DIR" ]; then
  rm -r $PCKG_DIR
fi

if [ -f "$PCKG_ZIP" ]; then
  rm $PCKG_ZIP
fi


mkdir $PCKG_DIR


#cp $TARGET $PCKG_DIR/
#cp install.sh $PCKG_DIR/
#cp config $PCKG_DIR/
#cp bwhcConnectorConfig.xml $PCKG_DIR/
#cp logback.xml  $PCKG_DIR/
#cp production.conf $PCKG_DIR/
#cp bwhc-backend-service $PCKG_DIR/

FILES=(
  "$TARGET"
  "install.sh"
  "config"
  "bwhcConnectorConfig.xml"
  "logback.xml"
  "production.conf"
  "bwhc-backend-service"
  "user_migration_v1.1.sh"
)

for FILE in "${FILES[@]}"; do
  echo "Copying $FILE to $PCKG_DIR..."
  cp $FILE "$PCKG_DIR/"
done


REPLACE_APP_PLACEHOLDER_CMD="s/$BWHC_APP_PLACEHOLDER/$BWHC_APP_NAME/g"

sed -i $REPLACE_APP_PLACEHOLDER_CMD $PCKG_DIR/bwhc-backend-service

sed -i $REPLACE_APP_PLACEHOLDER_CMD $PCKG_DIR/install.sh
sed -i "s/PREFIX_PLACEHOLDER/$BWHC_APP_PREFIX/g" $PCKG_DIR/install.sh



zip -r $PCKG_ZIP $PCKG_DIR/

rm -r $PCKG_DIR
