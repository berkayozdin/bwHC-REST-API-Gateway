#!/bin/bash


if [ "$#" -eq 0 ]; then

  echo "ERROR: enter the directory of JSON user entities to be migrated"
  exit 1

else

  USER_DIR="$1"

  cd $USER_DIR

  ROLE="ApprovedResearcher"

  echo "Removing obsolete role \"$ROLE\""

  # Replace all occurrences of ApprovedResearcher in JSON array: 
  # either in intermediate position [..., "ApprovedResearcher", ... ]
  # or in terminal position [..., "ApprovedResearcher" ]  

  REPLACE_COMMAND_1="s/\"$ROLE\", //g"
  REPLACE_COMMAND_2="s/, \"$ROLE\" //g"

  sed -i "$REPLACE_COMMAND_1" User*.json
  sed -i "$REPLACE_COMMAND_2" User*.json

fi

