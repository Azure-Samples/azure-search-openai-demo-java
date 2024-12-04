targetScope = 'subscription'

@minLength(1)
@maxLength(64)
@description('Name of the the environment which is used to generate a short unique hash used in all resources.')
param environmentName string

@minLength(1)
@description('Primary location for all resources')
param location string



param resourceGroupName string = ''

param applicationInsightsName string = ''
param logAnalyticsName string = ''


param kubernetesVersion string = '1.29.7'

param searchServiceName string = ''
param searchServiceResourceGroupName string = ''
param searchServiceLocation string = ''
// The free tier does not support managed identity (required) or semantic search (optional)
@allowed(['basic', 'standard', 'standard2', 'standard3', 'storage_optimized_l1', 'storage_optimized_l2'])
param searchServiceSkuName string // Set in main.parameters.json
param searchIndexName string // Set in main.parameters.json
param searchQueryLanguage string // Set in main.parameters.json
param searchQuerySpeller string // Set in main.parameters.json

param storageAccountName string = ''
param storageResourceGroupName string = ''
param storageResourceGroupLocation string = location
param storageContainerName string = 'content'
param storageSkuName string // Set in main.parameters.json

@allowed(['azure', 'openai'])
param openAiHost string // Set in main.parameters.json

param openAiServiceName string = ''
param openAiResourceGroupName string = ''
@description('Location for the OpenAI resource group')
@allowed(['canadaeast', 'eastus', 'eastus2', 'francecentral', 'switzerlandnorth', 'uksouth', 'japaneast', 'northcentralus', 'australiaeast', 'swedencentral'])
@metadata({
  azd: {
    type: 'location'
  }
})
param openAiResourceGroupLocation string
param customOpenAiResourceGroupLocation string = ''

param openAiSkuName string = 'S0'

param openAiApiKey string = ''
param openAiApiOrganization string = ''

param formRecognizerServiceName string = ''
param formRecognizerResourceGroupName string = ''
param formRecognizerResourceGroupLocation string = location

param formRecognizerSkuName string = 'S0'

param chatGptDeploymentName string // Set in main.parameters.json
param chatGptDeploymentCapacity int = 80
param chatGptModelName string = 'gpt-4o-mini'
param chatGptModelVersion string = '2024-07-18'
param chatGptDeploymentSkuName string= 'Standard'

param embeddingDeploymentName string // Set in main.parameters.json
param embeddingDeploymentCapacity int = 120
param embeddingModelName string = 'text-embedding-3-small'
param embeddingModelVersion string = '1'

param servicebusNamespace string = ''
param serviceBusSkuName string = 'Standard'
param queueName string = 'documents-queue'


@description('The resource name of the AKS cluster')
param clusterName string = ''
param containerRegistryName string = ''

param keyVaultName string = ''


@description('Id of the user or app to assign application roles')
param principalId string = ''
@description('Type of the principal. Valid values: User,ServicePrincipal')
param principalType string = 'User'

@description('Use Application Insights for monitoring and performance tracing')
param useApplicationInsights bool = false

param allowedOrigin string = ''

var abbrs = loadJsonContent('../../shared/abbreviations.json')
var resourceToken = toLower(uniqueString(subscription().id, environmentName, location))
var tags = { 'azd-env-name': environmentName, 'assignedTo': environmentName }

// Organize resources in a resource group
resource resourceGroup 'Microsoft.Resources/resourceGroups@2021-04-01' = {
  name: !empty(resourceGroupName) ? resourceGroupName : '${abbrs.resourcesResourceGroups}${environmentName}'
  location: location
  tags: tags
}

resource openAiResourceGroup 'Microsoft.Resources/resourceGroups@2021-04-01' existing = if (!empty(openAiResourceGroupName)) {
  name: !empty(openAiResourceGroupName) ? openAiResourceGroupName : resourceGroup.name
}

resource formRecognizerResourceGroup 'Microsoft.Resources/resourceGroups@2021-04-01' existing = if (!empty(formRecognizerResourceGroupName)) {
  name: !empty(formRecognizerResourceGroupName) ? formRecognizerResourceGroupName : resourceGroup.name
}

resource searchServiceResourceGroup 'Microsoft.Resources/resourceGroups@2021-04-01' existing = if (!empty(searchServiceResourceGroupName)) {
  name: !empty(searchServiceResourceGroupName) ? searchServiceResourceGroupName : resourceGroup.name
}

resource storageResourceGroup 'Microsoft.Resources/resourceGroups@2021-04-01' existing = if (!empty(storageResourceGroupName)) {
  name: !empty(storageResourceGroupName) ? storageResourceGroupName : resourceGroup.name
}

// Store secrets in a keyvault
module keyVault '../../shared/security/keyvault.bicep' = {
  name: 'keyvault'
  scope: resourceGroup
  params: {
    name: !empty(keyVaultName) ? keyVaultName : '${abbrs.keyVaultVaults}${resourceToken}'
    location: location
    tags: tags
    principalId: principalId
  }
}


// Monitor application with Azure Monitor
module monitoring '../../shared/monitor/monitoring.bicep' = if (useApplicationInsights) {
  name: 'monitoring'
  scope: resourceGroup
  params: {
    location: location
    tags: tags
    applicationInsightsName: !empty(applicationInsightsName) ? applicationInsightsName : '${abbrs.insightsComponents}${resourceToken}'
    logAnalyticsName: !empty(logAnalyticsName) ? logAnalyticsName : '${abbrs.operationalInsightsWorkspaces}${resourceToken}'
  }
}


module aks '../../shared/host/aks.bicep' = {
  name: 'aks'
  scope: resourceGroup
  params: {
    location: location
    tags: tags
    name: !empty(clusterName) ? clusterName : '${abbrs.containerServiceManagedClusters}${resourceToken}'
    containerRegistryName: !empty(containerRegistryName) ? containerRegistryName : '${abbrs.containerRegistryRegistries}${resourceToken}'
    logAnalyticsName: monitoring.outputs.logAnalyticsWorkspaceName
    keyVaultName: keyVault.outputs.name
    kubernetesVersion: kubernetesVersion
  }
}

module openAi '../../shared/ai/cognitiveservices.bicep' = if (openAiHost == 'azure') {
  name: 'openai'
  scope: openAiResourceGroup
  params: {
    name: !empty(openAiServiceName) ? openAiServiceName : '${abbrs.cognitiveServicesAccounts}${resourceToken}'
    location: !empty(customOpenAiResourceGroupLocation) ? customOpenAiResourceGroupLocation : openAiResourceGroupLocation
    tags: tags
    sku: {
      name: openAiSkuName
    }
    deployments: [
      {
        name: chatGptDeploymentName
        model: {
          format: 'OpenAI'
          name: chatGptModelName
          version: chatGptModelVersion
        }
        sku: {
          name: chatGptDeploymentSkuName
          capacity: chatGptDeploymentCapacity
        }
      }
      {
        name: embeddingDeploymentName
        model: {
          format: 'OpenAI'
          name: embeddingModelName
          version: embeddingModelVersion
        }
        sku: {
          name: 'Standard'
          capacity: embeddingDeploymentCapacity
        }
      }
    ]
  }
}

module formRecognizer '../../shared/ai/cognitiveservices.bicep' = {
  name: 'formrecognizer'
  scope: formRecognizerResourceGroup
  params: {
    name: !empty(formRecognizerServiceName) ? formRecognizerServiceName : '${abbrs.cognitiveServicesFormRecognizer}${resourceToken}'
    kind: 'FormRecognizer'
    location: formRecognizerResourceGroupLocation
    tags: tags
    sku: {
      name: formRecognizerSkuName
    }
  }
}

module searchService '../../shared/search/search-services.bicep' = {
  name: 'search-service'
  scope: searchServiceResourceGroup
  params: {
    name: !empty(searchServiceName) ? searchServiceName : 'gptkb-${resourceToken}'
    location: !empty(searchServiceLocation) ? searchServiceLocation : location
    tags: tags
    authOptions: {
      aadOrApiKey: {
        aadAuthFailureMode: 'http401WithBearerChallenge'
      }
    }
    sku: {
      name: searchServiceSkuName
    }
    semanticSearch: 'free'
  }
}

module storage '../../shared/storage/storage-account.bicep' = {
  name: 'storage'
  scope: storageResourceGroup
  params: {
    name: !empty(storageAccountName) ? storageAccountName : '${abbrs.storageStorageAccounts}${resourceToken}'
    location: storageResourceGroupLocation
    tags: tags
    allowBlobPublicAccess: false
    publicNetworkAccess: 'Enabled'
    sku: {
      name: storageSkuName
    }
    deleteRetentionPolicy: {
      enabled: true
      days: 2
    }
    containers: [
      {
        name: storageContainerName
        publicAccess: 'None'
      }
    ]
  }
}

module servicebusQueue '../../shared/servicebus/servicebus-queue.bicep' = {
  name: 'servicebusQueue'
  scope: resourceGroup
  params: {
    namespaceName: !empty(servicebusNamespace) ? servicebusNamespace : '${abbrs.serviceBusNamespaces}${resourceToken}'
    skuName: serviceBusSkuName
    queueName: queueName
    location: location
    tags: tags
  }
}

module eventGridSubscription '../../shared/event/eventgrid.bicep' = {
  name: 'eventGridSubscription'
  scope: resourceGroup
  params: {
    location: location
    storageAccountName: storage.outputs.name
    serviceBusNamespaceName: servicebusQueue.outputs.name
    queueName: servicebusQueue.outputs.queueName
    subscriptionName: '${abbrs.eventGridEventSubscriptions}blob-uploads-2-servicebus-queue'
    systemTopicName:'${abbrs.eventGridDomainsTopics}${resourceToken}'
  }
}

module openAiRoleAKS '../../shared/security/role.bicep' = if (openAiHost == 'azure') {
  scope: resourceGroup
  name: 'openai-role-aks'
  params: {
    principalId: aks.outputs.clusterIdentity.objectId
    roleDefinitionId: '5e0bd9bd-7b93-4f28-af87-19fc36ad61bd'
  }
}

module formRecognizerRoleAKS '../../shared/security/role.bicep' = {
  scope: resourceGroup
  name: 'formrecognizer-role-aks'
  params: {
    principalId: aks.outputs.clusterIdentity.objectId
    roleDefinitionId: 'a97b65f3-24c7-4388-baec-2e87135dc908'
  }
}

module storageRoleAKS '../../shared/security/role.bicep' = {
  scope: resourceGroup
  name: 'storage-role-aks'
  params: {
    principalId: aks.outputs.clusterIdentity.objectId
    roleDefinitionId: '2a2b9908-6ea1-4ae2-8e65-a410df84e7d1'
  }
}

module storageContribRoleAKS '../../shared/security/role.bicep' = {
  scope: resourceGroup
  name: 'storage-contribrole-aks'
  params: {
    principalId: aks.outputs.clusterIdentity.objectId
    roleDefinitionId: 'ba92f5b4-2d11-453d-a403-e96b0029c9fe'
  }
}

module searchRoleAKS '../../shared/security/role.bicep' = {
  scope: resourceGroup
  name: 'search-role-aks'
  params: {
    principalId: aks.outputs.clusterIdentity.objectId
    roleDefinitionId: '1407120a-92aa-4202-b7e9-c0e197c71c8f'
  }
}

module searchContribRoleAKS '../../shared/security/role.bicep' = {
  scope: resourceGroup
  name: 'search-contrib-role-aks'
  params: {
    principalId: aks.outputs.clusterIdentity.objectId
    roleDefinitionId: '8ebe5a00-799e-43f5-93ac-243d3dce84a7'
  }
}

module searchSvcContribRoleAKS '../../shared/security/role.bicep' = {
  scope: resourceGroup
  name: 'search-svccontrib-role-aks'
  params: {
    principalId: aks.outputs.clusterIdentity.objectId
    roleDefinitionId: '7ca78c08-252a-4471-8644-bb5ff32d4ba0'
  }
}

module servicesBusDataOwnerRoleAKS '../../shared/security/role.bicep' = {
  scope: resourceGroup
  name: 'service-bus-data-owner-role-aks'
  params: {
    principalId: aks.outputs.clusterIdentity.objectId
    roleDefinitionId: '090c5cfd-751d-490a-894a-3ce6f1109419'
  }
}

module eventGridContributorRoleAKS '../../shared/security/role.bicep' = {
  scope: resourceGroup
  name: 'event-grid-contributor-role-aks'
  params: {
    principalId: aks.outputs.clusterIdentity.objectId
    roleDefinitionId: '1e241071-0855-49ea-94dc-649edcd759de'
  }
}

module storageContribRoleUser '../../shared/security/role.bicep' = {
  scope: resourceGroup
  name: 'storage-contribrole-user'
  params: {
    principalId: principalId
    roleDefinitionId: 'ba92f5b4-2d11-453d-a403-e96b0029c9fe'
    principalType: principalType
  }
}

output AZURE_LOCATION string = location
output AZURE_TENANT_ID string = tenant().tenantId
output AZURE_RESOURCE_GROUP string = resourceGroup.name


output AZURE_CONTAINER_REGISTRY_ENDPOINT string = aks.outputs.containerRegistryLoginServer
output AZURE_CONTAINER_REGISTRY_NAME string = aks.outputs.containerRegistryName

// Shared by all OpenAI deployments
output OPENAI_HOST string = openAiHost
output AZURE_OPENAI_EMB_MODEL_NAME string = embeddingModelName
output AZURE_OPENAI_CHATGPT_MODEL string = chatGptModelName
// Specific to Azure OpenAI
output AZURE_OPENAI_SERVICE string = (openAiHost == 'azure') ? openAi.outputs.name : ''
output AZURE_OPENAI_RESOURCE_GROUP string = (openAiHost == 'azure') ? openAiResourceGroup.name : ''
output AZURE_OPENAI_CHATGPT_DEPLOYMENT string = (openAiHost == 'azure') ? chatGptDeploymentName : ''
output AZURE_OPENAI_EMB_DEPLOYMENT string = (openAiHost == 'azure') ? embeddingDeploymentName : ''
// Used only with non-Azure OpenAI deployments
output OPENAI_API_KEY string = (openAiHost == 'openai') ? openAiApiKey : ''
output OPENAI_ORGANIZATION string = (openAiHost == 'openai') ? openAiApiOrganization : ''

output AZURE_FORMRECOGNIZER_SERVICE string = formRecognizer.outputs.name
output AZURE_FORMRECOGNIZER_RESOURCE_GROUP string = formRecognizerResourceGroup.name

output AZURE_SEARCH_INDEX string = searchIndexName
output AZURE_SEARCH_SERVICE string = searchService.outputs.name
output AZURE_SEARCH_SERVICE_RESOURCE_GROUP string = searchServiceResourceGroup.name

output AZURE_STORAGE_ACCOUNT string = storage.outputs.name
output AZURE_STORAGE_CONTAINER string = storageContainerName
output AZURE_STORAGE_RESOURCE_GROUP string = storageResourceGroup.name

output AZURE_SERVICEBUS_NAMESPACE string = servicebusQueue.outputs.name
output AZURE_SERVICEBUS_SKU_NAME string = servicebusQueue.outputs.skuName

// AKS related deployment vars
output AZURE_AKS_CLUSTER_NAME string = aks.outputs.clusterName
output AZURE_CLIENT_ID string = aks.outputs.clusterIdentity.clientId //principalId
output API_ALLOW_ORIGINS string = allowedOrigin
output APPLICATIONINSIGHTS_CONNECTION_STRING string = monitoring.outputs.applicationInsightsConnectionString
output AZURE_KEY_VAULT_ENDPOINT string = keyVault.outputs.endpoint
output REACT_APP_APPLICATIONINSIGHTS_CONNECTION_STRING string =  monitoring.outputs.applicationInsightsConnectionString

// output BACKEND_URI string = backend.outputs.uri
// output INDEXER_FUNCTIONAPP_NAME string = indexer.outputs.name
