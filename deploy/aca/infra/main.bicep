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
param chatGptDeploymentSkuName string= 'Standard'
param chatGptModelName string = 'gpt-4o-mini'
param chatGptModelVersion string = '2024-07-18'
param embeddingDeploymentName string // Set in main.parameters.json
param embeddingDeploymentCapacity int = 120

param embeddingModelName string = 'text-embedding-3-small'
param embeddingModelVersion string = '1'

param servicebusNamespace string = ''
param serviceBusSkuName string = 'Standard'
param queueName string = 'documents-queue'


param containerAppsEnvironmentName string = ''
param containerRegistryName string = ''

param apiContainerAppName string = ''
param webContainerAppName string = ''
param indexerContainerAppName string = ''
param apiAppExists bool = false
param webAppExists bool = false
param indexerAppExists bool = false


@description('Id of the user or app to assign application roles for CLI to ingest documents')
param principalId string = ''
@description('Type of the principal. Valid values: User,ServicePrincipal')
param principalType string = 'User'

@description('Use Application Insights for monitoring and performance tracing')
param useApplicationInsights bool = false

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


module containerApps '../../shared/host/container-apps.bicep' = {
  name: 'container-apps'
  scope: resourceGroup
  params: {
    name: 'app'
    location: location
    tags: tags
    containerAppsEnvironmentName: !empty(containerAppsEnvironmentName) ? containerAppsEnvironmentName : '${abbrs.appManagedEnvironments}${resourceToken}'
    containerRegistryName: !empty(containerRegistryName) ? containerRegistryName : '${abbrs.containerRegistryRegistries}${resourceToken}'
    logAnalyticsWorkspaceName: monitoring.outputs.logAnalyticsWorkspaceName
    applicationInsightsName: monitoring.outputs.applicationInsightsName
  }
}

// Api backend
module api './app/api.bicep' = {
  name: 'api'
  scope: resourceGroup
  params: {
    name: !empty(apiContainerAppName) ? apiContainerAppName : '${abbrs.appContainerApps}api-${resourceToken}'
    location: location
    tags: tags
    identityName: '${abbrs.managedIdentityUserAssignedIdentities}api-${resourceToken}'
    applicationInsightsName: monitoring.outputs.applicationInsightsName
    containerAppsEnvironmentName: containerApps.outputs.environmentName
    containerRegistryName: containerApps.outputs.registryName
    corsAcaUrl: ''
    exists: apiAppExists
    env: [
      {
        name: 'AZURE_STORAGE_ACCOUNT'
        value: storage.outputs.name
      }
      {
        name: 'AZURE_STORAGE_CONTAINER'
        value: storageContainerName
      }
      {
        name: 'AZURE_SEARCH_INDEX'
        value: searchIndexName
      }
      {
        name: 'AZURE_SEARCH_SERVICE'
        value: searchService.outputs.name

      }
      {
        name: 'AZURE_SEARCH_QUERY_LANGUAGE'
        value: searchQueryLanguage
      }
      {
        name: 'AZURE_SEARCH_QUERY_SPELLER'
        value: searchQuerySpeller
      }
      {
        name: 'AZURE_OPENAI_EMB_MODEL_NAME'
        value: embeddingModelName
      }
      {
        name: 'AZURE_OPENAI_CHATGPT_MODEL'
        value: chatGptModelName
      }
      {
        name: 'AZURE_OPENAI_SERVICE'
        value:  openAi.outputs.name
      }
      {
        name: 'AZURE_OPENAI_CHATGPT_DEPLOYMENT'
        value: chatGptDeploymentName
      }
      {
        name: 'AZURE_OPENAI_EMB_DEPLOYMENT'
        value: embeddingDeploymentName
      }
    ]
  }
}

// Api backend
module indexer './app/indexer.bicep' = {
  name: 'indexer'
  scope: resourceGroup
  params: {
    name: !empty(indexerContainerAppName) ? indexerContainerAppName : '${abbrs.appContainerApps}indexer-${resourceToken}'
    location: location
    tags: tags
    identityName: '${abbrs.managedIdentityUserAssignedIdentities}indexer-${resourceToken}'
    applicationInsightsName: monitoring.outputs.applicationInsightsName
    containerAppsEnvironmentName: containerApps.outputs.environmentName
    containerRegistryName: containerApps.outputs.registryName
    exists: indexerAppExists
    env: [
      {
        name: 'AZURE_STORAGE_ACCOUNT'
        value: storage.outputs.name
      }
      {
        name: 'AZURE_STORAGE_CONTAINER'
        value: storageContainerName
      }
      {
        name: 'AZURE_SEARCH_INDEX'
        value: searchIndexName
      }
      {
        name: 'AZURE_SEARCH_SERVICE'
        value: searchService.outputs.name

      }
      {
        name: 'AZURE_FORMRECOGNIZER_SERVICE'
        value: formRecognizer.outputs.name
      }
     
      {
        name: 'AZURE_OPENAI_EMB_MODEL_NAME'
        value: embeddingModelName
      }
     
      {
        name: 'AZURE_OPENAI_SERVICE'
        value: openAi.outputs.name
      }
     
      {
        name: 'AZURE_OPENAI_EMB_DEPLOYMENT'
        value: embeddingDeploymentName
      }
      {
        name: 'AZURE_SERVICEBUS_NAMESPACE'
        value: servicebusQueue.outputs.name
      }
      {
        name: 'AZURE_SERVICEBUS_QUEUE_NAME'
        value: queueName
            }
    ]
  }
}

module web './app/web.bicep' = {
  name: 'web'
  scope: resourceGroup
  params: {
    name: !empty(webContainerAppName) ? webContainerAppName : '${abbrs.appContainerApps}web-${resourceToken}'
    location: location
    tags: tags
    identityName: '${abbrs.managedIdentityUserAssignedIdentities}web-${resourceToken}'
    apiBaseUrl:  api.outputs.SERVICE_API_URI
    applicationInsightsName: monitoring.outputs.applicationInsightsName
    containerAppsEnvironmentName: containerApps.outputs.environmentName
    containerRegistryName: containerApps.outputs.registryName
    exists: webAppExists
  }
}


module openAi '../../shared/ai/cognitiveservices.bicep' =  {
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

// USER ROLES
module openAiRoleUser '../../shared/security/role.bicep'  = {
  scope: openAiResourceGroup
  name: 'openai-role-user'
  params: {
    principalId: principalId
    roleDefinitionId: '5e0bd9bd-7b93-4f28-af87-19fc36ad61bd'
    principalType: principalType
  }
}

module formRecognizerRoleUser '../../shared/security/role.bicep' = {
  scope: formRecognizerResourceGroup
  name: 'formrecognizer-role-user'
  params: {
    principalId: principalId
    roleDefinitionId: 'a97b65f3-24c7-4388-baec-2e87135dc908'
    principalType: principalType
  }
}

module storageRoleUser '../../shared/security/role.bicep' = {
  scope: storageResourceGroup
  name: 'storage-role-user'
  params: {
    principalId: principalId
    roleDefinitionId: '2a2b9908-6ea1-4ae2-8e65-a410df84e7d1'
    principalType: principalType
  }
}

module storageContribRoleUser '../../shared/security/role.bicep' = {
  scope: storageResourceGroup
  name: 'storage-contribrole-user'
  params: {
    principalId: principalId
    roleDefinitionId: 'ba92f5b4-2d11-453d-a403-e96b0029c9fe'
    principalType: principalType
  }
}

module searchRoleUser '../../shared/security/role.bicep' = {
  scope: searchServiceResourceGroup
  name: 'search-role-user'
  params: {
    principalId: principalId
    roleDefinitionId: '1407120a-92aa-4202-b7e9-c0e197c71c8f'
    principalType: principalType
  }
}

module searchContribRoleUser '../../shared/security/role.bicep' = {
  scope: searchServiceResourceGroup
  name: 'search-contrib-role-user'
  params: {
    principalId: principalId
    roleDefinitionId: '8ebe5a00-799e-43f5-93ac-243d3dce84a7'
    principalType: principalType
  }
}

module searchSvcContribRoleUser '../../shared/security/role.bicep' = {
  scope: searchServiceResourceGroup
  name: 'search-svccontrib-role-user'
  params: {
    principalId: principalId
    roleDefinitionId: '7ca78c08-252a-4471-8644-bb5ff32d4ba0'
    principalType: principalType
  }
}

// SYSTEM IDENTITIES

module openAiRoleBackend '../../shared/security/role.bicep' =  {
  scope: openAiResourceGroup
  name: 'openai-role-backend'
  params: {
    principalId: api.outputs.SERVICE_API_IDENTITY_PRINCIPAL_ID
    roleDefinitionId: '5e0bd9bd-7b93-4f28-af87-19fc36ad61bd'
    principalType: 'ServicePrincipal'
  }
}


module openAiRoleIndexer '../../shared/security/role.bicep' = {
  scope: openAiResourceGroup
  name: 'openai-role-indexer'
  params: {
    principalId: indexer.outputs.SERVICE_INDEXER_IDENTITY_PRINCIPAL_ID
    roleDefinitionId: '5e0bd9bd-7b93-4f28-af87-19fc36ad61bd'
    principalType: 'ServicePrincipal'
  }
}


module storageRoleBackend '../../shared/security/role.bicep' = {
  scope: storageResourceGroup
  name: 'storage-role-backend'
  params: {
    principalId: api.outputs.SERVICE_API_IDENTITY_PRINCIPAL_ID
    roleDefinitionId: '2a2b9908-6ea1-4ae2-8e65-a410df84e7d1'
    principalType: 'ServicePrincipal'
  }
}


module storageRoleIndexer '../../shared/security/role.bicep' = {
  scope: storageResourceGroup
  name: 'storage-role-indexer'
  params: {
    principalId: indexer.outputs.SERVICE_INDEXER_IDENTITY_PRINCIPAL_ID
    roleDefinitionId: 'ba92f5b4-2d11-453d-a403-e96b0029c9fe'
    principalType: 'ServicePrincipal'
  }
}


module searchRoleBackend '../../shared/security/role.bicep' = {
  scope: searchServiceResourceGroup
  name: 'search-role-backend'
  params: {
    principalId: api.outputs.SERVICE_API_IDENTITY_PRINCIPAL_ID
    roleDefinitionId: '1407120a-92aa-4202-b7e9-c0e197c71c8f'
    principalType: 'ServicePrincipal'
  }
}


module searchRoleIndexer '../../shared/security/role.bicep' = {
  scope: searchServiceResourceGroup
  name: 'search-role-indexer'
  params: {
    principalId: indexer.outputs.SERVICE_INDEXER_IDENTITY_PRINCIPAL_ID
    roleDefinitionId: '8ebe5a00-799e-43f5-93ac-243d3dce84a7'
    principalType: 'ServicePrincipal'
  }
} 


module formRecognizerRoleIndexer '../../shared/security/role.bicep' = {
  scope: formRecognizerResourceGroup
  name: 'formrecognizer-role-indexer'
  params: {
    principalId: indexer.outputs.SERVICE_INDEXER_IDENTITY_PRINCIPAL_ID
    roleDefinitionId: 'a97b65f3-24c7-4388-baec-2e87135dc908'
    principalType: 'ServicePrincipal'
  }
}

module serviceBusRoleIndexer '../../shared/security/role.bicep' = {
  scope: resourceGroup
  name: 'serviceBusRole-role-indexer'
  params: {
    principalId: indexer.outputs.SERVICE_INDEXER_IDENTITY_PRINCIPAL_ID
    roleDefinitionId: '4f6d3b9b-027b-4f4c-9142-0e5a2a2247e0'
    principalType: 'ServicePrincipal'
  }
}


output AZURE_LOCATION string = location
output AZURE_TENANT_ID string = tenant().tenantId
output AZURE_RESOURCE_GROUP string = resourceGroup.name


output AZURE_CONTAINER_ENVIRONMENT_NAME string = containerApps.outputs.environmentName
output AZURE_CONTAINER_REGISTRY_ENDPOINT string = containerApps.outputs.registryLoginServer
output AZURE_CONTAINER_REGISTRY_NAME string = containerApps.outputs.registryName

// Shared by all OpenAI deployments

output AZURE_OPENAI_EMB_MODEL_NAME string = embeddingModelName
output AZURE_OPENAI_CHATGPT_MODEL string = chatGptModelName
// Specific to Azure OpenAI
output AZURE_OPENAI_SERVICE string =  openAi.outputs.name
output AZURE_OPENAI_RESOURCE_GROUP string = openAiResourceGroup.name 
output AZURE_OPENAI_CHATGPT_DEPLOYMENT string = chatGptDeploymentName
output AZURE_OPENAI_EMB_DEPLOYMENT string = embeddingDeploymentName
// Used only with non-Azure OpenAI deployments
output OPENAI_API_KEY string = openAiApiKey
output OPENAI_ORGANIZATION string = openAiApiOrganization

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

// output BACKEND_URI string = backend.outputs.uri
// output INDEXER_FUNCTIONAPP_NAME string = indexer.outputs.name
