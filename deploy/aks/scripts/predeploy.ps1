# PowerShell script to replace environment variables in deployment files

Write-Output "Loading azd .env file from current environment"

# Retrieve environment variables from the `.env` file using `azd env get-values`
$envData = azd env get-values | Out-String
$lines = $envData -split "`r?`n"

# Create a hashtable to store environment variables
$envVars = @{}

# Start building the ConfigMap YAML content
$configMapContent = @"
apiVersion: v1
kind: ConfigMap
metadata:
  name: azd-env-configmap
data:

"@


foreach ($line in $lines) {
    if ($line -match '^(.*?)=(.*)$') {
        # Extract key and value, trimming quotes from value
        $key = $matches[1]
        $value = $matches[2].Trim('"')
        $envVars[$key] = $value
        $configMapContent += "  ${key}: `"${value}`"`n"
    }
}

# Specify the path to the deployment files you want to process
# Define the root directory to search for .tmpl.yml files
# Use '.' for the current directory or specify a path
$rootDir = "../../"
# Find all .tmpl.yml files in the specified directory and its subdirectories
$deploymentFiles = Get-ChildItem -Path $rootDir -Filter *.tmpl -Recurse

$number = $deploymentFiles.Length
$replaced = 0
Write-Output "Updating environment variables in $number files..."

foreach ($file in $deploymentFiles) {

    if (Test-Path $file) {
        # Read the content of the file
        $content = Get-Content $file -Raw

        # Iterate through each environment variable and replace it in the file's content
        foreach ($key in $envVars.Keys) {
            $placeholder = "{ { .Env.$key } }"
            $value = $envVars[$key]
            #if($key.Contains("IMAGE_NAME")) { 
            #        $splitValue = $value -split ':'
            #        $value = $splitValue[0]
            #}
            $content = $content.Replace($placeholder, $value)
        }

            
        # Define the new filename (replace .tmpl.yml with .yml)
        $newFileName = $file.FullName -replace '\.tmpl$', '-deployment.yml'

        # Write the updated content to a new file
        Set-Content -Path $newFileName -Value $content

        $replaced++
    }
    else {
        Write-Warning "File not found: $file"
    }
}

Write-Output "Environment variable replacement complete for $replaced/$number of files."

# Specify the output file path for the ConfigMap
$outputFilePath = "../../app/backend/manifests/azd-env-configmap.yml"

# Add-Content -Path $outputFilePath -Value $configMapContent -Encoding UTF8
# Write the ConfigMap content to the file
$configMapContent | Out-File -FilePath $outputFilePath -Encoding UTF8

Write-Output "ConfigMap generated at $outputFilePath"