#!/bin/bash


APP_DIR=bwhc-backend
APP_ZIP=$APP_DIR.zip


if [ -d "$APP_DIR" ]; then
  rm -r $APP_DIR
fi

if [ -d "$APP_ZIP" ]; then
  rm $APP_ZIP
fi


mkdir $APP_DIR


cp ../target/universal/bwhc-rest-api-gateway-1.0-SNAPSHOT.zip $APP_DIR/

cp install.sh $APP_DIR/

cp config $APP_DIR/

cp bwhc-backend-service $APP_DIR/

cp bwhcConnectorConfig.xml $APP_DIR/

cp logback.xml  $APP_DIR/

cp production.conf $APP_DIR/


zip --encrypt -r $APP_ZIP $APP_DIR/

rm -r $APP_DIR
