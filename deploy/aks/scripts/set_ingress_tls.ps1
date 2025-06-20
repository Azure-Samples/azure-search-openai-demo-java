$AZURE_USE_AUTHENTICATION = (azd env get-value AZURE_USE_AUTHENTICATION)
if ($AZURE_USE_AUTHENTICATION -ne "true") {
  Exit 0
}

$output = azd env get-values

foreach ($line in $output) {
  if (!$line.Contains('=')) {
    continue
  }

  $name, $value = $line.Split("=")
  $value = $value -replace '^\"|\"$'
  [Environment]::SetEnvironmentVariable($name, $value)
}

if($env:AZURE_USE_AUTHENTICATION -eq "true"){
  Write-Host "Enabling App authentication for the AKS Cluster"
  Write-Host "If you want to disable it, please set the AZURE_USE_AUTHENTICATION environment variable to false"
} else {
  Write-Host " App authentication is not enabled for the AKS Cluster"
  Write-Host "If you want to enable it, please set the AZURE_USE_AUTHENTICATION environment variable to true"
  exit 0;
}

$location = $env:AZURE_LOCATION

$adAppName = "$env:AZURE_ENV_NAME"

$appHostName="$adAppName.$location.cloudapp.azure.com"

$clusterName = $env:AZURE_AKS_CLUSTER_NAME
$clusterRG = $env:AZURE_RESOURCE_GROUP

##### SET DNS name for Ingress Public IP #####

Write-Host "Configure DNS for cluster Public IP using $appHostName"

# Get the AKS MC_ resource group name
$nodeRG = az aks show -n $clusterName -g $clusterRG -o json | ConvertFrom-Json | Select-Object -ExpandProperty nodeResourceGroup
Write-Host "AKS Cluster is in resource group: $nodeRG"

$ingressIP=$(kubectl get ingress ingress-api -n java-rag-ns -o jsonpath="{.status.loadBalancer.ingress[0].ip}")

if($ingressIP -eq "" -or  $null -eq $ingressIP){
  Write-Host "Please retry once Ingress Address is assigned to the AKS Cluster"
  exit 1;
}


Write-Host "Found Ingress IP: $ingressIP"

# List public IP resources in the specified resource group and convert JSON output to PowerShell object
$publicIPs = az network public-ip list -g $nodeRG -o json | ConvertFrom-Json
# Filter the public IPs to find one that matches the ingress IP and select the name
$ipName = $publicIPs | Where-Object { $_.ipAddress -eq $ingressIP } | Select-Object -ExpandProperty name

Write-Host "Pulic-ip IP Name within RG is: $ipName"

# Add a DNS name ($adAppName) to the public IP address
Write-Host "Adding DNS name to public IP address"
az network public-ip update -g $nodeRG -n $ipName --dns-name $adAppName

# Get the FQDN assigned to the public IP address
$ingressHost=$(az network public-ip show -g $nodeRG -n $ipName -o json | ConvertFrom-Json | Select-Object -ExpandProperty dnsSettings | Select-Object -ExpandProperty fqdn)
# This should be the same as the $APP_HOSTNAME
Write-Host "FQDN assigned to the public IP address: $ingressHost"
if ($ingressHost -ne $appHostName) {
Write-Host "FQDN assigned to the public IP address does not match the expected value: $appHostName"
exit 1
}


##### Enable https for Ingress controller #####
#https://learn.microsoft.com/en-us/azure/aks/app-routing-dns-ssl#create-and-export-a-self-signed-ssl-certificate

$kvName = $env:AZURE_KEY_VAULT_NAME

#Check if aks-ingress-tls certificate exists in Azure Key Vault
$certificate = az keyvault certificate show --vault-name $kvName --name aks-ingress-tls --query id -o tsv 
if ($certificate) {
  Write-Host ""
  Write-Host "aks-ingress-tls certificate already exists in Azure Key Vault."
} else {
      Write-Host "aks-ingress-tls certificate does not exist in Azure Key Vault. Searching for aks-ingress-tls.pfx file into ./ folder to import.."
      #check if aks-ingress-tls.pfx exists and import it in Azure Key Vault
      if (Test-Path -Path "aks-ingress-tls.pfx") {
        Write-Host "aks-ingress-tls.pfx found."
      } else {
        Write-Host "Please create a aks-ingress-tls.pfx. For more info see https://learn.microsoft.com/en-us/azure/aks/app-routing-dns-ssl#create-and-export-a-self-signed-ssl-certificate "
        exit 1
      }

      # Import the PFX file into Azure Key Vault
      Write-Host "Importing aks-ingress-tls.pfx into Azure Key Vault: $kvName"
      az keyvault certificate import --vault-name $kvName --name aks-ingress-tls --file aks-ingress-tls.pfx 

}



# Enable azure key vault as Secrets Store CSI driver for application routing add-on enabled
$keyVaultId = az keyvault show --name $kvName --query id -o tsv
                                                                              
$secretsProvider = az aks show -g $clusterRG -n $clusterName --query "addonProfiles.azureKeyvaultSecretsProvider" -o json | ConvertFrom-Json

if ($secretsProvider -and $secretsProvider.PSObject.Properties['enabled'] -and ($secretsProvider.enabled -eq $true -or $secretsProvider.enabled -eq "true")) {
  Write-Host "Azure Key Vault Secrets Provider add-on is ENABLED" -ForegroundColor Green
} else {
  Write-Host "Enabling Azure Key Vault Secrets Provider add-on for aks ingress. KeyVault Id[$keyVaultId] " -ForegroundColor Green
  az aks approuting update -g $clusterRG -n $clusterName --enable-kv --attach-kv $keyVaultId

  $IngressServicePrincipalID = az ad sp list --display-name "webapprouting-$clusterName" --query "[].id" --output tsv

  Write-Host "Assigning Key Vault access policies to the Ingress Service Principal [$IngressServicePrincipalID] for Key Vault [$kvName]"
  az keyvault set-policy --name $kvName --object-id  $IngressServicePrincipalID --secret-permissions get list --certificate-permissions get list
}



# Create the Ingress resource with TLS configuration
$certUri = "https://$kvName.vault.azure.net/certificates/aks-ingress-tls"

$ingressWithTls = @"
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: ingress-api
  namespace: java-rag-ns
  annotations: 
    nginx.ingress.kubernetes.io/proxy-body-size : "20m"
    kubernetes.azure.com/tls-cert-keyvault-uri: $certUri
spec:
  ingressClassName: webapprouting.kubernetes.azure.com
  rules:
    - host: $ingressHost
      http:
        paths:
          - path: /api
            pathType: Prefix
            backend:
              service:
                name: backend-service
                port:
                  number: 80
          - path: /
            pathType: Prefix
            backend:
              service:
                name: frontend-service
                port:
                  number: 80
  tls:
    - hosts:
        - $ingressHost
      secretName: keyvault-ingress-api
"@


$outputFilePath = "ingress-tls.yml"


# Write the Ingress content to the file
if (Test-Path $outputFilePath) {
    Remove-Item -Force $outputFilePath
}
$ingressWithTls | Out-File -FilePath $outputFilePath -Encoding UTF8

Write-Output "Ingress with TLS generated at $outputFilePath"

# Apply the Ingress resource with TLS configuration
kubectl apply -f $outputFilePath -n java-rag-ns

$ingressURL = "https://$ingressHost"

Write-Host "Ingress with TLS applied successfully"

# Set the AZD env variable for the ingress FQDN. WEB_URI env variable is used to setup redirect url for Oauth2 setup in Entra app registration.
azd env set WEB_URI "https://$ingressHost"