targetScope = 'subscription'

@minLength(1)
@maxLength(64)
@description('Name of the the environment which is used to generate a short unique hash used in all resources.')
param environmentName string

@minLength(1)
@description('Primary location for all resources')
param location string

param tenantId string = tenant().tenantId
param authTenantId string = ''

var tenantIdForAuth = !empty(authTenantId) ? authTenantId : tenantId
var authenticationIssuerUri = '${environment().authentication.loginEndpoint}${tenantIdForAuth}/v2.0'

// Used for the optional login and document level access control system
param useAuthentication bool = true
param enableUnauthenticatedAccess bool = false

// Set by pre-provision scripts
param serverAppId string = ''
@secure()
param serverAppSecret string = ''
param clientAppId string = ''
@secure()
param clientAppSecret string = ''

@allowed(['None', 'AzureServices'])
@description('If allowedIp is set, whether azure services are allowed to bypass the storage and AI services firewall.')
param bypass string = 'AzureServices'

@description('Public network access value for all deployed resources')
@allowed(['Enabled', 'Disabled'])
param publicNetworkAccess string = 'Enabled'


param resourceGroupName string = ''

param applicationInsightsName string = ''
param logAnalyticsName string = ''


param kubernetesVersion string = '1.33'

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

@description('Use chat history feature in browser')
param useChatHistoryBrowser bool = false
@description('Use chat history feature in CosmosDB')
param useChatHistoryCosmos bool = false

@description('Logged user can retrieve documents from default folder.')
param enableGlobalDocumentAccess bool = true

@description('Use Service Bus for indexing documents requests')
param useServiceBusIndexing bool = false

@allowed(['free', 'provisioned', 'serverless'])
param cosmosDbSkuName string // Set in main.parameters.json
param cosmodDbResourceGroupName string = ''
param cosmosDbLocation string = ''
param cosmosDbAccountName string = ''
param cosmosDbThroughput int = 400
param chatHistoryDatabaseName string = 'chat-database'
param chatHistoryContainerName string = 'chat-history-v2'
param chatHistoryVersion string = 'cosmosdb-v2'



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

param documentIntelligenceServiceName string = ''
param documentIntelligenceResourceGroupName string = ''
param documentIntelligenceResourceGroupLocation string = location

param documentIntelligenceSkuName string = 'S0'

param chatGptDeploymentName string // Set in main.parameters.json
param chatGptDeploymentCapacity int = 80
param chatGptModelName string = 'gpt-4o-mini'
param chatGptModelVersion string = '2024-07-18'
param chatGptDeploymentSkuName string= 'Standard'

param embeddingModelName string = ''
param embeddingDeploymentName string = ''
param embeddingDeploymentVersion string = ''
param embeddingDeploymentSkuName string = ''
param embeddingDeploymentCapacity int = 0
param embeddingDimensions int = 0

var embedding = {
  modelName: !empty(embeddingModelName) ? embeddingModelName : 'text-embedding-3-large'
  deploymentName: !empty(embeddingDeploymentName) ? embeddingDeploymentName : 'text-embedding-3-large'
  deploymentVersion: !empty(embeddingDeploymentVersion) ? embeddingDeploymentVersion : (embeddingModelName == 'text-embedding-ada-002' ? '2' : '1')
  deploymentSkuName: !empty(embeddingDeploymentSkuName) ? embeddingDeploymentSkuName : (embeddingModelName == 'text-embedding-ada-002' ? 'Standard' : 'GlobalStandard')
  deploymentCapacity: embeddingDeploymentCapacity != 0 ? embeddingDeploymentCapacity : 120
  dimensions: embeddingDimensions != 0 ? embeddingDimensions : 3072
}

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

resource documentIntelligenceResourceGroup 'Microsoft.Resources/resourceGroups@2021-04-01' existing = if (!empty(documentIntelligenceResourceGroupName)) {
  name: !empty(documentIntelligenceResourceGroupName) ? documentIntelligenceResourceGroupName : resourceGroup.name
}
resource searchServiceResourceGroup 'Microsoft.Resources/resourceGroups@2021-04-01' existing = if (!empty(searchServiceResourceGroupName)) {
  name: !empty(searchServiceResourceGroupName) ? searchServiceResourceGroupName : resourceGroup.name
}

resource storageResourceGroup 'Microsoft.Resources/resourceGroups@2021-04-01' existing = if (!empty(storageResourceGroupName)) {
  name: !empty(storageResourceGroupName) ? storageResourceGroupName : resourceGroup.name
}

resource cosmosDbResourceGroup 'Microsoft.Resources/resourceGroups@2024-11-01' existing = if (!empty(cosmodDbResourceGroupName)) {
  name: !empty(cosmodDbResourceGroupName) ? cosmodDbResourceGroupName : resourceGroup.name
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
        name: embedding.deploymentName
        model: {
          format: 'OpenAI'
          name: embedding.modelName
          version: embedding.deploymentVersion
        }
        sku: {
          name: embedding.deploymentSkuName
          capacity: embedding.deploymentCapacity
        }
      }
    ]
  }
}

module documentIntelligence '../../shared/ai/cognitiveservices.bicep' = {
  name: 'documentIntelligence'
  scope: documentIntelligenceResourceGroup
  params: {
    name: !empty(documentIntelligenceServiceName) ? documentIntelligenceServiceName : '${abbrs.cognitiveServicesDocumentIntelligence}${resourceToken}'
    kind: 'FormRecognizer'
    location: documentIntelligenceResourceGroupLocation
    tags: tags
    sku: {
      name: documentIntelligenceSkuName
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

module cosmosDb 'br/public:avm/res/document-db/database-account:0.6.1' = if (useAuthentication && useChatHistoryCosmos) {
  name: 'cosmosdb'
  scope: cosmosDbResourceGroup
  params: {
    name: !empty(cosmosDbAccountName) ? cosmosDbAccountName : '${abbrs.documentDBDatabaseAccounts}${resourceToken}'
    location: !empty(cosmosDbLocation) ? cosmosDbLocation : location
    locations: [
      {
        locationName: !empty(cosmosDbLocation) ? cosmosDbLocation : location
        failoverPriority: 0
        isZoneRedundant: false
      }
    ]
    enableFreeTier: cosmosDbSkuName == 'free'
    capabilitiesToAdd: cosmosDbSkuName == 'serverless' ? ['EnableServerless'] : []
    networkRestrictions: {
      ipRules: []
      networkAclBypass: bypass
      publicNetworkAccess: publicNetworkAccess
      virtualNetworkRules: []
    }
    sqlDatabases: [
      {
        name: chatHistoryDatabaseName
        throughput: (cosmosDbSkuName == 'serverless') ? null : cosmosDbThroughput
        containers: [
          {
            name: chatHistoryContainerName
            kind: 'MultiHash'
            paths: [
              '/entra_oid'
              '/session_id'
            ]
            indexingPolicy: {
              indexingMode: 'consistent'
              automatic: true
              includedPaths: [
                {
                  path: '/entra_oid/?'
                }
                {
                  path: '/session_id/?'
                }
                {
                  path: '/timestamp/?'
                }
                {
                  path: '/type/?'
                }
              ]
              excludedPaths: [
                {
                  path: '/*'
                }
              ]
            }
          }
        ]
      }
    ]
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

module servicesBusDataOwnerRoleAKS '../../shared/security/role.bicep' = if (useServiceBusIndexing){
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

// RBAC for Cosmos DB
// https://learn.microsoft.com/azure/cosmos-db/nosql/security/how-to-grant-data-plane-role-based-access
module cosmosDbRoleBackend '../../shared/security/documentdb-sql-role.bicep' = if (useAuthentication && useChatHistoryCosmos) {
  scope: cosmosDbResourceGroup
  name: 'cosmosdb-role-backend'
  params: {
    databaseAccountName: (useAuthentication && useChatHistoryCosmos) ? cosmosDb.outputs.name : ''
    principalId: aks.outputs.clusterIdentity.objectId
    // Cosmos DB Built-in Data Contributor role
    roleDefinitionId: (useAuthentication && useChatHistoryCosmos)
      ? '/${subscription().id}/resourceGroups/${cosmosDb.outputs.resourceGroupName}/providers/Microsoft.DocumentDB/databaseAccounts/${cosmosDb.outputs.name}/sqlRoleDefinitions/00000000-0000-0000-0000-000000000002'
      : ''
  }
}


output AZURE_LOCATION string = location
output AZURE_AUTH_TENANT_ID string = authTenantId
output AZURE_RESOURCE_GROUP string = resourceGroup.name


output AZURE_CONTAINER_REGISTRY_ENDPOINT string = aks.outputs.containerRegistryLoginServer
output AZURE_CONTAINER_REGISTRY_NAME string = aks.outputs.containerRegistryName

// Shared by all OpenAI deployments
output OPENAI_HOST string = openAiHost
output AZURE_OPENAI_EMB_MODEL_NAME string = embeddingModelName
output AZURE_OPENAI_EMB_DIMENSIONS int = embedding.dimensions
output AZURE_OPENAI_CHATGPT_MODEL string = chatGptModelName
// Specific to Azure OpenAI
output AZURE_OPENAI_SERVICE string = (openAiHost == 'azure') ? openAi.outputs.name : ''
output AZURE_OPENAI_RESOURCE_GROUP string = (openAiHost == 'azure') ? openAiResourceGroup.name : ''
output AZURE_OPENAI_CHATGPT_DEPLOYMENT string = (openAiHost == 'azure') ? chatGptDeploymentName : ''
output AZURE_OPENAI_EMB_DEPLOYMENT string = (openAiHost == 'azure') ? embeddingDeploymentName : ''
// Used only with non-Azure OpenAI deployments
output OPENAI_API_KEY string = (openAiHost == 'openai') ? openAiApiKey : ''
output OPENAI_ORGANIZATION string = (openAiHost == 'openai') ? openAiApiOrganization : ''

output AZURE_DOCUMENT_INTELLIGENCE_SERVICE string = documentIntelligence.outputs.name
output AZURE_DOCUMENT_INTELLIGENCE_RESOURCE_GROUP string = documentIntelligenceResourceGroup.name

output AZURE_SEARCH_INDEX string = searchIndexName
output AZURE_SEARCH_SERVICE string = searchService.outputs.name
output AZURE_SEARCH_SERVICE_RESOURCE_GROUP string = searchServiceResourceGroup.name

output AZURE_STORAGE_ACCOUNT string = storage.outputs.name
output AZURE_STORAGE_CONTAINER string = storageContainerName
output AZURE_STORAGE_RESOURCE_GROUP string = storageResourceGroup.name

output AZURE_SERVICEBUS_NAMESPACE string = (useServiceBusIndexing) ? servicebusQueue.outputs.name : ''
output AZURE_SERVICEBUS_SKU_NAME string = (useServiceBusIndexing) ? servicebusQueue.outputs.skuName : ''
output AZURE_SERVICEBUS_QUEUE_NAME string = (useServiceBusIndexing) ? servicebusQueue.outputs.queueName : ''

output AZURE_COSMOSDB_ACCOUNT string = (useAuthentication && useChatHistoryCosmos) ? cosmosDb.outputs.name : ''
output AZURE_CHAT_HISTORY_DATABASE string = chatHistoryDatabaseName
output AZURE_CHAT_HISTORY_CONTAINER string = chatHistoryContainerName
output AZURE_CHAT_HISTORY_VERSION string = chatHistoryVersion

output AZURE_USE_AUTHENTICATION bool = useAuthentication
output AZURE_ENABLE_GLOBAL_DOCUMENT_ACCESS bool = enableGlobalDocumentAccess
output USE_CHAT_HISTORY_BROWSER  bool = useChatHistoryBrowser
output USE_CHAT_HISTORY_COSMOS bool = useChatHistoryCosmos
output USE_SERVICEBUS_INDEXING bool = useServiceBusIndexing

output AZURE_CLIENT_APP_ID string = (useAuthentication) ? clientAppId: ''
output AZURE_SERVER_APP_ID string = (useAuthentication) ? serverAppId : ''
output AZURE_KEY_VAULT_NAME string = keyVault.outputs.name

//http://%s.%s.svc.cluster.local:%d
output INDEXING_API_SERVER_URL string = 'http://indexer-service.java-rag-ns.svc.cluster.local'

// AKS related deployment vars
output AZURE_AKS_CLUSTER_NAME string = aks.outputs.clusterName
output AZURE_CLIENT_ID string = aks.outputs.clusterIdentity.clientId //principalId
output API_ALLOW_ORIGINS string = allowedOrigin
output APPLICATIONINSIGHTS_CONNECTION_STRING string = monitoring.outputs.applicationInsightsConnectionString
output AZURE_KEY_VAULT_ENDPOINT string = keyVault.outputs.endpoint
output REACT_APP_APPLICATIONINSIGHTS_CONNECTION_STRING string =  monitoring.outputs.applicationInsightsConnectionString

// output BACKEND_URI string = backend.outputs.uri
// output INDEXER_FUNCTIONAPP_NAME string = indexer.outputs.name
