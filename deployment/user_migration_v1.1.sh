#!/bin/bash


if [ "$#" -eq 0 ]; then

  echo "ERROR: enter a directory of JSON user entities to be migrated"
  exit 1

else

  USER_DIR="$1"

  cd $USER_DIR

  ROLE="ApprovedResearcher"

  echo "Removing obsolete role \"$ROLE\""

  REPLACE_COMMAND="s/\"$ROLE\", //g"

  sed -i "$REPLACE_COMMAND" User*.json

fi

