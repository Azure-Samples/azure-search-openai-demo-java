# Using template from: https://github.com/Azure/EasyAuthForK8s/blob/master/docs/deploy-to-existing-cluster.md

Write-Host ""
Write-Host "Loading azd .env file from current environment"
Write-Host ""

$output = azd env get-values

foreach ($line in $output) {
  if (!$line.Contains('=')) {
    continue
  }

  $name, $value = $line.Split("=")
  $value = $value -replace '^\"|\"$'
  [Environment]::SetEnvironmentVariable($name, $value)
}

Write-Host "Environment variables set."
$appId = $Env:AZURE_AD_APP_ID
if($appId) {
  Write-Host "Deleting Azure AD App ${Env:AZURE_AD_APP_NAME}"
  az ad app delete --id $env:AZURE_AD_APP_ID
}
