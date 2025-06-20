$output = azd env get-values

foreach ($line in $output) {
  if (!$line.Contains('=')) {
    continue
  }

  $name, $value = $line.Split("=")
  $value = $value -replace '^\"|\"$'
  [Environment]::SetEnvironmentVariable($name, $value)
}

if($Env:AZURE_USE_EASY_AUTH -eq "true"){
  Write-Host "Enabled EasyAuth for the AKS Cluster"
} else {
  exit 0;
}

Write-Host "Deleting Entra App ${Env:adAppName}:${Env:AZURE_AD_APP_ID}"

az ad app delete --id "{$Env:AZURE_AD_APP_ID}"