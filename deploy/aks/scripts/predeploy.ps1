# PowerShell script to replace environment variables in deployment files

Write-Output "Loading azd .env file from current environment"

# Retrieve environment variables from the `.env` file using `azd env get-values`
$envData = azd env get-values | Out-String
$lines = $envData -split "`r?`n"

# Create a hashtable to store environment variables
$envVars = @{}
$date = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
# Start building the ConfigMap YAML content
$configMapContent = @"
# Updated ${date}
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
# Specify the output file path for the ConfigMap
$outputFilePath = "../../app/backend/manifests/azd-env-configmap.yml"

# Add-Content -Path $outputFilePath -Value $configMapContent -Encoding UTF8
# Write the ConfigMap content to the file
if (Test-Path $outputFilePath) {
    Remove-Item -Force $outputFilePath
}
$configMapContent | Out-File -FilePath $outputFilePath -Encoding UTF8

Write-Output "ConfigMap generated at $outputFilePath"