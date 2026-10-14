# Using template from: https://github.com/Azure/EasyAuthForK8s/blob/master/docs/deploy-to-existing-cluster.md
# Docs ref: https://learn.microsoft.com/en-us/azure/aks/ingress-tls?tabs=azure-cli

$output = azd env get-values

foreach ($line in $output) {
  if (!$line.Contains('=')) {
    continue
  }

  $name, $value = $line.Split("=")
  $value = $value -replace '^\"|\"$'
  [Environment]::SetEnvironmentVariable($name, $value)
}

if($env:AZURE_USE_EASY_AUTH -eq "true"){
  Write-Host "Enabling EasyAuth for the AKS Cluster"
  Write-Host "If you want to disable EasyAuth, please set the AZURE_USE_EASY_AUTH environment variable to false"
} else {
  Write-Host "EasyAuth is not enabled for the AKS Cluster"
  Write-Host "If you want to enable EasyAuth, please set the AZURE_USE_EASY_AUTH environment variable to true"
  exit 0;
}

$location = $env:AZURE_LOCATION

$adAppName = "$env:AZURE_ENV_NAME"
$email = "example@microsoft.com"

$appHostName="$adAppName.$location.cloudapp.azure.com"

# Set these variables
$homePage = "https://$appHostName"
$replyUrls = "https://$appHostName/easyauth/signin-oidc"
$tlsSecretName="$appHostName-tls"

$clusterName = $env:AZURE_AKS_CLUSTER_NAME
$clusterRG = $env:AZURE_RESOURCE_GROUP

Write-Host "Configure DNS for cluster Public IP using $appHostName"

# Get the AKS MC_ resource group name
$nodeRG = az aks show -n $clusterName -g $clusterRG -o json | ConvertFrom-Json | Select-Object -ExpandProperty nodeResourceGroup
Write-Host "AKS Cluster is in resource group: $nodeRG"

$ingressIP=$(kubectl get ingress ingress-api -n azure-open-ai -o jsonpath="{.status.loadBalancer.ingress[0].ip}")

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

# ---------------------
# Register AAD Aplication
# Create Azure AD application and retrieve the appId
Write-Host "Creating Azure AD Application"

$appCreationResult = az ad app create --display-name $adAppName --web-home-page-url "${homePage}" --web-redirect-uris "${replyUrls}" --required-resource-accesses '@easyauth/manifest.json' -o json | ConvertFrom-Json
$appId = $appCreationResult | Select-Object -ExpandProperty appId
Write-Host "Created Azure AD application with appId: $appId"

# Reset credentials for the Azure AD application to generate a new password
Write-Host "Resetting credentials for the Azure AD application"
$credentialResetResult = az ad app credential reset --id $appId -o json | ConvertFrom-Json
$clientSecret = $credentialResetResult | Select-Object -ExpandProperty password
Write-Output "Generated new client secret: $clientSecret"

# Retrieve and output the Azure AD tenant ID
$tenantInfo = az account show -o json | ConvertFrom-Json
$azureTenantId = $tenantInfo.tenantId
Write-Output "Retrieved Azure AD tenant ID: $azureTenantId"

# ---------------------
# Install Cert Manager
# Create the namespace
kubectl create namespace cert-manager

# Add the Jetstack Helm repository
helm repo add jetstack https://charts.jetstack.io

# Update your local Helm chart repository cache
helm repo update

kubectl label namespace cert-manager cert-manager.io/disable-validation=true

# Install the cert manager
helm install cert-manager jetstack/cert-manager `
--namespace cert-manager `
--version v1.14.2 `
--set installCRDs=true `
--set ingressShim.defaultIssuerName=letsencrypt `
--set ingressShim.defaultIssuerKind=ClusterIssuer

kubectl get pods -n cert-manager

# Deploy the issuer config to the cluster
kubectl apply -f ./easyauth/cluster-issuer.yaml

# ---------------------
# Deploy Easy Auth Proxy
helm install --set azureAd.tenantId=$azureTenantId --set azureAd.clientId=$appId --set secret.name=easyauth-proxy-$adAppName-secret --set secret.azureclientsecret=$clientSecret --set appHostName=$appHostName --set tlsSecretName=$tlsSecretName easyauth-proxy ./easyauth/easyauth-proxy

# ---------------------
# Apply proxy ingress rules
# Creating the easyauth-ingress.yaml content
$easyauthIngressYaml = @"
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: easyauth-ingress-router
  annotations:
    nginx.ingress.kubernetes.io/auth-url: "https://`$host/easyauth/auth"
    nginx.ingress.kubernetes.io/auth-signin: "https://`$host/easyauth/login"
    nginx.ingress.kubernetes.io/auth-response-headers: "x-injected-userinfo,x-injected-name,x-injected-oid,x-injected-preferred-username,x-injected-sub,x-injected-tid,x-injected-email,x-injected-groups,x-injected-scp,x-injected-roles,x-injected-graph"
    cert-manager.io/cluster-issuer: letsencrypt
    #nginx.ingress.kubernetes.io/rewrite-target: "/`$1"
spec:
  ingressClassName: webapprouting.kubernetes.azure.com
  tls:
  - hosts:
    - ${appHostName}
    secretName: ${tlsSecretName}
  rules:
  - host: ${appHostName}
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
"@

# Save the YAML configuration to a file
$easyauthIngressYaml | Out-File -FilePath ./easyauth/easyauth-ingress.yaml

# Deploy the ingress configuration to the cluster
kubectl apply -f ./easyauth/easyauth-ingress.yaml

# Remove old ingress without auth
kubectl delete ingress ingress-api -n 'azure-open-ai'

azd env set "AZURE_AD_APP_NAME" $adAppName
azd env set "AZURE_AD_APP_ID" $appId

$easyAuthConfig = @"
EasyAuth for AKS has been configured successfully.

The application is now available at:${homePage}

Configuration AD details:
- App Host Name: $appHostName
- Azure AD Application: $adAppName
- Azure AD Application ID / Client ID: $appId
- Azure AD Tenant ID: $azureTenantId
- Client Secret: $clientSecret

Configuration AKS details:
- AKS Cluster: $clusterName
- Ingress IP: $ingressIP
- Public IP Name: $ipName
- Public AKS Resource Group: $nodeRG
"@

Write-Host $easyAuthConfig 
