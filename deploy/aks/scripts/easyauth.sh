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

if [ "$AZURE_USE_EASY_AUTH" = "true" ]; then
  echo "Enabling EasyAuth for the AKS Cluster"
  echo "If you want to disable EasyAuth, please set the AZURE_USE_EASY_AUTH environment variable to false"
else
  echo "EasyAuth is not enabled for the AKS Cluster"
  echo "If you want to enable EasyAuth, please set the AZURE_USE_EASY_AUTH environment variable to true"
  exit 0
fi

location=$AZURE_LOCATION

adAppName="$AZURE_ENV_NAME"
email="example@microsoft.com"

appHostName="$adAppName.$location.cloudapp.azure.com"

# Set these variables
homePage="https://$appHostName"
replyUrls="https://$appHostName/easyauth/signin-oidc"
tlsSecretName="$appHostName-tls"

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
ipName=$(az network public-ip list -g $nodeRG -o json | jq -c ".[] | select(.ipAddress | contains(\"$ingressIP\"))" | jq '.name' -r)
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
# ---------------------
# Register AAD Application
echo "Creating Azure AD Application"
appCreationResult=$(az ad app create --display-name $adAppName --web-home-page-url "$homePage" --web-redirect-uris "$replyUrls" --required-resource-accesses '@easyauth/manifest.json' -o json)
appId=$(echo $appCreationResult | jq -r '.appId')
echo "Created Azure AD application with appId: $appId"

# Reset credentials for the Azure AD application to generate a new password
echo "Resetting credentials for the Azure AD application"
credentialResetResult=$(az ad app credential reset --id $appId -o json)
clientSecret=$(echo $credentialResetResult | jq -r '.password')
echo "Generated new client secret: $clientSecret"

# Retrieve and output the Azure AD tenant ID
tenantInfo=$(az account show -o json)
azureTenantId=$(echo $tenantInfo | jq -r '.tenantId')
echo "Retrieved Azure AD tenant ID: $azureTenantId"

# ---------------------
# Install Cert Manager
# Create the namespace
kubectl create namespace cert-manager

# Add the Jetstack Helm repository
helm repo add jetstack https://charts.jetstack.io

# Update your local Helm chart repository cache
helm repo update

# Label the namespace
kubectl label namespace cert-manager cert-manager.io/disable-validation=true

# Install the cert manager
helm install cert-manager jetstack/cert-manager \
  --namespace cert-manager \
  --version v1.14.2 \
  --set installCRDs=true \
  --set ingressShim.defaultIssuerName=letsencrypt \
  --set ingressShim.defaultIssuerKind=ClusterIssuer

# Check the installed pods in cert-manager namespace
kubectl get pods -n cert-manager

# Deploy the issuer config to the cluster
kubectl apply -f ./easyauth/cluster-issuer.yaml

# ---------------------
# Deploy Easy Auth Proxy
helm install --set azureAd.tenantId="$azureTenantId" --set azureAd.clientId="$appId" --set secret.name="easyauth-proxy-$adAppName-secret" --set secret.azureclientsecret="$clientSecret" --set appHostName="$appHostName" --set tlsSecretName="$tlsSecretName" easyauth-proxy ./easyauth/easyauth-proxy

# ---------------------
# Apply proxy ingress rules

# Creating the easyauth-ingress.yaml content
cat << EOF > ./easyauth/easyauth-ingress.yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: easyauth-ingress-router
  annotations:
    nginx.ingress.kubernetes.io/auth-url: "https://\$host/easyauth/auth"
    nginx.ingress.kubernetes.io/auth-signin: "https://\$host/easyauth/login"
    nginx.ingress.kubernetes.io/auth-response-headers: "x-injected-userinfo,x-injected-name,x-injected-oid,x-injected-preferred-username,x-injected-sub,x-injected-tid,x-injected-email,x-injected-groups,x-injected-scp,x-injected-roles,x-injected-graph"
    cert-manager.io/cluster-issuer: letsencrypt
spec:
  ingressClassName: webapprouting.kubernetes.azure.com
  tls:
  - hosts:
    - \${appHostName}
    secretName: \${tlsSecretName}
  rules:
  - host: \${appHostName}
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
EOF

# Deploy the ingress configuration to the cluster
kubectl apply -f ./easyauth/easyauth-ingress.yaml

# Remove old ingress without auth
kubectl delete ingress ingress-api -n 'azure-open-ai'

# Update environment variables (This part needs to be adapted based on how you manage env vars in your setup)
# For a simple script, you could just export them, but they'd be session-specific.
export AZURE_AD_APP_NAME="$adAppName"
export AZURE_AD_APP_ID="$appId"

# Print Easy Auth Config Information
echo "EasyAuth for AKS has been configured successfully.

The application is now available at: ${homePage}

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
"
