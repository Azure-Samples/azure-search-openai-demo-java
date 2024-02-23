#!/bin/bash

# Displaying the initial message
echo "Loading azd .env file from current environment"

# Retrieve environment variables from the `.env` file using `azd env get-values`
envData=$(azd env get-values)

# Create a temporary file to store environment variables
tempEnvFile="temp_env_file"
echo "$envData" > "$tempEnvFile"

# Get the current date in the required format
date=$(date +"%Y-%m-%d %H:%M:%S")

# Start building the ConfigMap YAML content
configMapContent="# Updated $date
apiVersion: v1
kind: ConfigMap
metadata:
  name: azd-env-configmap
data:
"

# Read each line from the temp file and process it
while IFS= read -r line; do
    if [[ $line =~ ^([^=]+)=(.*)$ ]]; then
        key="${BASH_REMATCH[1]}"
        value="${BASH_REMATCH[2]}"
        # Trim quotes from value
        trimmedValue="${value%\"}"
        trimmedValue="${trimmedValue#\"}"
        # Append to the ConfigMap content
        configMapContent+="  ${key}: \"${trimmedValue}\""$'\n'
    fi
done < "$tempEnvFile"

# Specify the output file path for the ConfigMap
outputFilePath="../../app/backend/manifests/azd-env-configmap.yml"

# Check if the output file exists and remove it before creating a new one
if [ -f "$outputFilePath" ]; then
    rm -f "$outputFilePath"
fi

# Write the ConfigMap content to the file
echo -e "$configMapContent" > "$outputFilePath"

# Cleaning up temporary files
rm -f "$tempEnvFile"

echo "ConfigMap generated at $outputFilePath"