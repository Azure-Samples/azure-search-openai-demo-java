#!/bin/bash

# Using template from: https://github.com/Azure/EasyAuthForK8s/blob/master/docs/deploy-to-existing-cluster.md
# Docs ref: https://learn.microsoft.com/en-us/azure/aks/ingress-tls?tabs=azure-cli


# Check if jq for json manipulation is installed, if not attempt to install it
if ! command -v jq &> /dev/null; then
    echo "'jq' could not be found. Attempting to install..."
    if command -v apt &> /dev/null; then
        sudo apt update && sudo apt install -y jq
    elif command -v yum &> /dev/null; then
        sudo yum update && sudo yum install -y jq
    else
        echo "Package manager not recognized. Please install 'jq' manually."
        exit 1
    fi
fi

# Getting environment variables
output=$(azd env get-values)

# Loop through each line in the output
while IFS= read -r line; do
  if [[ ! $line == *"="* ]]; then
    continue
  fi

  name=$(echo "$line" | cut -d '=' -f 1)
  value=$(echo "$line" | cut -d '=' -f 2 | sed 's/^"//;s/"$//')
  export "$name"="$value"
done <<< "$output"

if [ "$AZURE_USE_AUTHENTICATION" = "true" ]; then
  echo "Enabling App authentication for the AKS Cluster"
  echo "If you want to disable it, please set the AZURE_USE_AUTHENTICATION environment variable to false"
else
  echo "App authentication is not enabled for the AKS Cluster"
  echo "If you want to enable EasyAuth, please set the AZURE_USE_AUTHENTICATION environment variable to true"
  exit 0
fi



location=$AZURE_LOCATION

adAppName="$AZURE_ENV_NAME"

appHostName="$adAppName.$location.cloudapp.azure.com"


clusterName=$AZURE_AKS_CLUSTER_NAME
clusterRG=$AZURE_RESOURCE_GROUP

echo "Configure DNS for cluster Public IP using $appHostName"

# Get the AKS MC_ resource group name
nodeRG=$(az aks show -n $clusterName -g $clusterRG -o json | jq -r '.nodeResourceGroup')
echo "AKS Cluster is in resource group: $nodeRG"

ingressIP=$(kubectl get ingress ingress-api -n azure-open-ai -o jsonpath="{.status.loadBalancer.ingress[0].ip}")

if [ -z "$ingressIP" ]; then
  echo "Please retry once Ingress Address is assigned to the AKS Cluster"
  exit 1
fi

echo "Found Ingress IP: $ingressIP"

# List public IP resources in the specified resource group
ipName=$(az network.public-ip list -g $nodeRG -o json | jq -c ".[] | select(.ipAddress | contains(\"$ingressIP\"))" | jq '.name' -r)
echo "Public-ip IP Name within RG is: $ipName"

# Add a DNS name ($adAppName) to the public IP address
echo "Adding DNS name to public IP address"
az network public-ip update -g $nodeRG -n $ipName --dns-name $adAppName

# Get the FQDN assigned to the public IP address
ingressHost=$(az network public-ip show -g $nodeRG -n $ipName -o json | jq -r '.dnsSettings.fqdn')
echo "FQDN assigned to the public IP address: $ingressHost"
if [ "$ingressHost" != "$appHostName" ]; then
  echo "FQDN assigned to the public IP address does not match the expected value: $appHostName"
  exit 1
fi

# Set the environment variable for the ingress FQDN
export WEB_URI="$ingressHost"

##### Enable https for Ingress controller #####
# https://learn.microsoft.com/en-us/azure/aks/app-routing-dns-ssl#create-and-export-a-self-signed-ssl-certificate

kvName=$AZURE_KEY_VAULT_NAME

# Check if aks-ingress-tls certificate exists in Azure Key Vault
certificate=$(az keyvault certificate show --vault-name $kvName --name aks-ingress-tls --query id -o tsv 2>/dev/null)
if [ -n "$certificate" ]; then
  echo ""
  echo "aks-ingress-tls certificate already exists in Azure Key Vault."
else
  echo "aks-ingress-tls certificate does not exist in Azure Key Vault. Searching for aks-ingress-tls.pfx file into ./ folder to import.."
  # Check if aks-ingress-tls.pfx exists and import it in Azure Key Vault
  if [ -f "aks-ingress-tls.pfx" ]; then
    echo "aks-ingress-tls.pfx found."
  else
    echo "Please create a aks-ingress-tls.pfx. For more info see https://learn.microsoft.com/en-us/azure/aks/app-routing-dns-ssl#create-and-export-a-self-signed-ssl-certificate"
    exit 1
  fi

  # Import the PFX file into Azure Key Vault
  echo "Importing aks-ingress-tls.pfx into Azure Key Vault: $kvName"
  az keyvault certificate import --vault-name $kvName --name aks-ingress-tls --file aks-ingress-tls.pfx
fi

# Enable azure key vault as Secrets Store CSI driver for application routing add-on enabled
keyVaultId=$(az keyvault show --name $kvName --query id -o tsv)

secretsProvider=$(az aks show -g $clusterRG -n $clusterName --query "addonProfiles.azureKeyvaultSecretsProvider" -o json)
secretsProviderEnabled=$(echo $secretsProvider | jq -r '.enabled // false')

if [ "$secretsProviderEnabled" = "true" ]; then
  echo "Azure Key Vault Secrets Provider add-on is ENABLED"
else
  echo "Enabling Azure Key Vault Secrets Provider add-on for aks ingress. KeyVault Id[$keyVaultId]"
  az aks approuting update -g $clusterRG -n $clusterName --enable-kv --attach-kv $keyVaultId

  IngressServicePrincipalID=$(az ad sp list --display-name "webapprouting-$clusterName" --query "[].id" --output tsv)

  echo "Assigning Key Vault access policies to the Ingress Service Principal [$IngressServicePrincipalID] for Key Vault [$kvName]"
  az keyvault set-policy --name $kvName --object-id $IngressServicePrincipalID --secret-permissions get list --certificate-permissions get list
fi

# Create the Ingress resource with TLS configuration
certUri="https://$kvName.vault.azure.net/certificates/aks-ingress-tls"

cat > ingress-tls.yml <<EOF
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
EOF

echo "Ingress with TLS generated at ingress-tls.yml"

# Apply the Ingress resource with TLS configuration
kubectl apply -f ingress-tls.yml -n java-rag-ns

ingressURL="https://$ingressHost"

echo "Ingress with TLS applied successfully"

# Set the AZD env variable for the ingress FQDN. WEB_URI env variable is used to setup redirect url for Oauth2 setup in Entra app registration.
azd env set WEB_URI "https://$ingressHost"
