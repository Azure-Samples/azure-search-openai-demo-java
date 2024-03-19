#!/bin/bash

# Retrieve environment values
output=$(azd env get-values)

# Loop through each line of output
while IFS= read -r line; do
  # Continue if line doesn't contain '='
  if [[ ! "$line" == *"="* ]]; then
    continue
  fi

  # Split line into name and value
  name=$(echo "$line" | cut -d '=' -f 1)
  value=$(echo "$line" | cut -d '=' -f 2- | sed -e 's/^"//' -e 's/"$//')

  # Set environment variable
  export "$name=$value"
done <<< "$output"

# Check if EasyAuth is enabled
if [ "$AZURE_USE_EASY_AUTH" == "true" ]; then
  echo "Enabled EasyAuth for the AKS Cluster"
else
  exit 0
fi

# Deleting Entra App
echo "Deleting Entra App ${adAppName}:${AZURE_AD_APP_ID}"

az ad app delete --id "${AZURE_AD_APP_ID}"
