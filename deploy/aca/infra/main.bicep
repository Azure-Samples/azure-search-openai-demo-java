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


// Used for the optional login and document level access control system
param useAuthentication bool = true
param serverAppId string = ''
param clientAppId string = ''


@allowed(['None', 'AzureServices'])
@description('If allowedIp is set, whether azure services are allowed to bypass the storage and AI services firewall.')
param bypass string = 'AzureServices'

@description('Public network access value for all deployed resources')
@allowed(['Enabled', 'Disabled'])
param publicNetworkAccess string = 'Enabled'


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

param tokenStorageContainerName string = 'tokens'

@description('Use chat history feature in browser')
param useChatHistoryBrowser bool = false
@description('Use chat history feature in CosmosDB')
param useChatHistoryCosmos bool = false

@description('Logged user can retrieve documents from default folder.')
param enableGlobalDocumentAccess bool = true

@description('Use Service Bus for indexing documents requests')
param useServiceBusIndexing bool = false

param useEval bool = false
param useSafetyEval bool = false

@allowed(['free', 'provisioned', 'serverless'])
param cosmosDbSkuName string // Set in main.parameters.json
param cosmodDbResourceGroupName string = ''
param cosmosDbLocation string = ''
param cosmosDbAccountName string = ''
param cosmosDbThroughput int = 400
param chatHistoryDatabaseName string = 'chat-database'
param chatHistoryContainerName string = 'chat-history-v2'
param chatHistoryVersion string = 'cosmosdb-v2'



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

param azureOpenAiDisableKeys bool = true
param chatGptModelName string = ''
param chatGptDeploymentName string = ''
param chatGptDeploymentVersion string = ''
param chatGptDeploymentSkuName string = ''
param chatGptDeploymentCapacity int = 0

var chatGpt = {
  modelName: !empty(chatGptModelName) ? chatGptModelName : 'gpt-4o-mini'
  deploymentName: !empty(chatGptDeploymentName) ? chatGptDeploymentName : 'gpt-4o-mini'
  deploymentVersion: !empty(chatGptDeploymentVersion) ? chatGptDeploymentVersion : '2024-07-18'
  deploymentSkuName: !empty(chatGptDeploymentSkuName) ? chatGptDeploymentSkuName : 'GlobalStandard' // Not backward-compatible
  deploymentCapacity: chatGptDeploymentCapacity != 0 ? chatGptDeploymentCapacity : 30
}

param evalModelName string = ''
param evalDeploymentName string = ''
param evalModelVersion string = ''
param evalDeploymentSkuName string = ''
param evalDeploymentCapacity int = 0

var eval = {
  modelName: !empty(evalModelName) ? evalModelName : 'gpt-4o'
  deploymentName: !empty(evalDeploymentName) ? evalDeploymentName : 'gpt-4o'
  deploymentVersion: !empty(evalModelVersion) ? evalModelVersion : '2024-08-06'
  deploymentSkuName: !empty(evalDeploymentSkuName) ? evalDeploymentSkuName : 'GlobalStandard' // Not backward-compatible
  deploymentCapacity: evalDeploymentCapacity != 0 ? evalDeploymentCapacity : 30
}

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
        name: 'AZURE_OPENAI_EMB_MODEL_NAME'
        value: embedding.modelName
      }
      {
        name: 'AZURE_OPENAI_EMB_DEPLOYMENT'
        value: embedding.deploymentName
      }
      {
        name: 'AZURE_OPENAI_EMB_DIMENSIONS'
        value: embedding.dimensions
      }
      {
        name: 'AZURE_CLIENT_APP_ID'
        value: clientAppId
      }
      {
        name: 'AZURE_SERVER_APP_ID'
        value: serverAppId
      }
      {
        name: 'AZURE_AUTH_TENANT_ID'
        value: tenantIdForAuth
      }
      { name: 'INDEXING_API_SERVER_URL'
        value: indexer.outputs.SERVICE_INDEXER_URI
      }
      {
        name: 'AZURE_COSMOSDB_ACCOUNT'
        value: (useAuthentication && useChatHistoryCosmos) ? cosmosDb.outputs.name : ''
      }
      {
        name: 'AZURE_CHAT_HISTORY_DATABASE'
        value: chatHistoryDatabaseName
      }
      {
        name: 'AZURE_CHAT_HISTORY_CONTAINER'
        value: chatHistoryContainerName
      }
      {
        name: 'USE_CHAT_HISTORY_BROWSER'
        value: useChatHistoryBrowser
      }
      {
        name: 'USE_CHAT_HISTORY_COSMOS'
        value: useChatHistoryCosmos
      }
    
      {
        name: 'AZURE_ENABLE_GLOBAL_DOCUMENT_ACCESS'
        value: enableGlobalDocumentAccess
      }
      {
        name: 'AZURE_USE_AUTHENTICATION'
        value: useAuthentication
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
        name: 'AZURE_DOCUMENT_INTELLIGENCE_SERVICE'
        value: documentIntelligence.outputs.name
      }
     
      {
        name: 'AZURE_OPENAI_EMB_MODEL_NAME'
        value: embedding.modelName
      }
      {
        name: 'AZURE_OPENAI_EMB_DEPLOYMENT'
        value: embedding.deploymentName
      }
      {
        name: 'AZURE_OPENAI_EMB_DIMENSIONS'
        value: embedding.dimensions
      }
     
      {
        name: 'AZURE_OPENAI_SERVICE'
        value: openAi.outputs.name
      }
      {
        name: 'USE_SERVICEBUS_INDEXING'
        value: useServiceBusIndexing
      }
     
      {
        name: 'AZURE_SERVICEBUS_NAMESPACE'
        value: servicebusQueue.outputs.name
      }
      {
        name: 'AZURE_SERVICEBUS_SKU_NAME'
        value: serviceBusSkuName
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

var defaultOpenAiDeployments = [
  {
    name: chatGpt.deploymentName
    model: {
      format: 'OpenAI'
      name: chatGpt.modelName
      version: chatGpt.deploymentVersion
    }
    sku: {
      name: chatGpt.deploymentSkuName
      capacity: chatGpt.deploymentCapacity
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

var openAiDeployments = concat(
  defaultOpenAiDeployments,
  useEval
    ? [
      {
        name: eval.deploymentName
        model: {
          format: 'OpenAI'
          name: eval.modelName
          version: eval.deploymentVersion
        }
        sku: {
          name: eval.deploymentSkuName
          capacity: eval.deploymentCapacity
        }
      }
    ] : []
)


module openAi 'br/public:avm/res/cognitive-services/account:0.7.2' =  {
  name: 'openai'
  scope: openAiResourceGroup
  params: {
    name: !empty(openAiServiceName) ? openAiServiceName : '${abbrs.cognitiveServicesAccounts}${resourceToken}'
    location: !empty(customOpenAiResourceGroupLocation) ? customOpenAiResourceGroupLocation : openAiResourceGroupLocation
    tags: tags
    kind: 'OpenAI'
    customSubDomainName: !empty(openAiServiceName)
      ? openAiServiceName
      : '${abbrs.cognitiveServicesAccounts}${resourceToken}'
    publicNetworkAccess: publicNetworkAccess
    networkAcls: {
      defaultAction: 'Allow'
      bypass: bypass
    }
    sku: openAiSkuName
    deployments: openAiDeployments
    disableLocalAuth: azureOpenAiDisableKeys
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
    isHnsEnabled: false
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
       {
        name: tokenStorageContainerName
        publicAccess: 'None'
      }
    ]
  }
}

module servicebusQueue '../../shared/servicebus/servicebus-queue.bicep' = if (useServiceBusIndexing) {
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

module eventGridSubscription '../../shared/event/eventgrid.bicep' = if (useServiceBusIndexing){
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

module ai '../../shared/ai/ai-environment.bicep' = if (useSafetyEval) {
  name: 'ai'
  scope: resourceGroup
  params: {
    // Limited region support: https://learn.microsoft.com/azure/ai-foundry/how-to/develop/evaluate-sdk#region-support
    location: 'eastus2'
    tags: tags
    hubName: 'aihub-${resourceToken}'
    projectName: 'aiproj-${resourceToken}'
    storageAccountId: storage.outputs.id
    applicationInsightsId: !useApplicationInsights ? '' : monitoring.outputs.applicationInsightsId
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

module documentIntelligenceRoleUser '../../shared/security/role.bicep' = {
  scope: documentIntelligenceResourceGroup
  name: 'documentintelligence-role-user'
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

module cosmosDbAccountContribRoleUser '../../shared/security/role.bicep' = if (useAuthentication && useChatHistoryCosmos) {
  scope: cosmosDbResourceGroup
  name: 'cosmosdb-account-contrib-role-user'
  params: {
    principalId: principalId
    roleDefinitionId: '5bd9cd88-fe45-4216-938b-f97437e15450'
    principalType: principalType
  }
}

// RBAC for Cosmos DB
// https://learn.microsoft.com/azure/cosmos-db/nosql/security/how-to-grant-data-plane-role-based-access
module cosmosDbDataContribRoleUser '../../shared/security/documentdb-sql-role.bicep' = if (useAuthentication && useChatHistoryCosmos) {
  scope: cosmosDbResourceGroup
  name: 'cosmosdb-data-contrib-role-user'
  params: {
    databaseAccountName: (useAuthentication && useChatHistoryCosmos) ? cosmosDb.outputs.name : ''
    principalId: principalId
    // Cosmos DB Built-in Data Contributor role
    roleDefinitionId: (useAuthentication && useChatHistoryCosmos)
      ? '/${subscription().id}/resourceGroups/${cosmosDb.outputs.resourceGroupName}/providers/Microsoft.DocumentDB/databaseAccounts/${cosmosDb.outputs.name}/sqlRoleDefinitions/00000000-0000-0000-0000-000000000002'
      : ''
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


// Assign Storage Blob Data Contributor to API and Indexer managed identities
module storageRoleBackend '../../shared/security/role.bicep' = {
  scope: storageResourceGroup
  name: 'storage-role-backend'
  params: {
    principalId: api.outputs.SERVICE_API_IDENTITY_PRINCIPAL_ID
    roleDefinitionId: 'ba92f5b4-2d11-453d-a403-e96b0029c9fe'
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

// RBAC for Cosmos DB
// https://learn.microsoft.com/azure/cosmos-db/nosql/security/how-to-grant-data-plane-role-based-access
module cosmosDbRoleBackend '../../shared/security/documentdb-sql-role.bicep' = if (useAuthentication && useChatHistoryCosmos) {
  scope: cosmosDbResourceGroup
  name: 'cosmosdb-role-backend'
  params: {
    databaseAccountName: (useAuthentication && useChatHistoryCosmos) ? cosmosDb.outputs.name : ''
    principalId: api.outputs.SERVICE_API_IDENTITY_PRINCIPAL_ID
    // Cosmos DB Built-in Data Contributor role
    roleDefinitionId: (useAuthentication && useChatHistoryCosmos)
      ? '/${subscription().id}/resourceGroups/${cosmosDb.outputs.resourceGroupName}/providers/Microsoft.DocumentDB/databaseAccounts/${cosmosDb.outputs.name}/sqlRoleDefinitions/00000000-0000-0000-0000-000000000002'
      : ''
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


module documentIntelligenceRoleIndexer '../../shared/security/role.bicep' = {
  scope: documentIntelligenceResourceGroup
  name: 'documentintelligence-role-indexer'
  params: {
    principalId: indexer.outputs.SERVICE_INDEXER_IDENTITY_PRINCIPAL_ID
    roleDefinitionId: 'a97b65f3-24c7-4388-baec-2e87135dc908'
    principalType: 'ServicePrincipal'
  }
}

module serviceBusRoleIndexer '../../shared/security/role.bicep' = if (useServiceBusIndexing){
  scope: resourceGroup
  name: 'serviceBusRole-role-indexer'
  params: {
    principalId: indexer.outputs.SERVICE_INDEXER_IDENTITY_PRINCIPAL_ID
    roleDefinitionId: '4f6d3b9b-027b-4f4c-9142-0e5a2a2247e0'
    principalType: 'ServicePrincipal'
  }
}


output AZURE_LOCATION string = location
output AZURE_TENANT_ID string = tenantId
output AZURE_AUTH_TENANT_ID string = authTenantId
output AZURE_RESOURCE_GROUP string = resourceGroup.name


output AZURE_CONTAINER_ENVIRONMENT_NAME string = containerApps.outputs.environmentName
output AZURE_CONTAINER_REGISTRY_ENDPOINT string = containerApps.outputs.registryLoginServer
output AZURE_CONTAINER_REGISTRY_NAME string = containerApps.outputs.registryName

// Shared by all OpenAI deployments

output AZURE_OPENAI_EMB_MODEL_NAME string = embedding.modelName
output AZURE_OPENAI_EMB_DIMENSIONS int = embedding.dimensions
output AZURE_OPENAI_CHATGPT_MODEL string = chatGptModelName

// Specific to Azure OpenAI
output AZURE_OPENAI_SERVICE string =  openAi.outputs.name
output AZURE_OPENAI_RESOURCE_GROUP string = openAiResourceGroup.name 
output AZURE_OPENAI_CHATGPT_DEPLOYMENT string = chatGptDeploymentName
output AZURE_OPENAI_EMB_DEPLOYMENT string = embedding.deploymentName 
output AZURE_OPENAI_EMB_DEPLOYMENT_VERSION string = embedding.deploymentVersion
output AZURE_OPENAI_EMB_DEPLOYMENT_SKU string = embedding.deploymentSkuName

// Specific to Azure OpenAI with eval
output AZURE_OPENAI_EVAL_DEPLOYMENT string = useEval ? eval.deploymentName : ''
output AZURE_OPENAI_EVAL_DEPLOYMENT_VERSION string = useEval ? eval.deploymentVersion : ''
output AZURE_OPENAI_EVAL_DEPLOYMENT_SKU string = useEval ? eval.deploymentSkuName : ''
output AZURE_OPENAI_EVAL_MODEL string = useEval ? eval.modelName : ''

// Used only with non-Azure OpenAI deployments
output OPENAI_API_KEY string = openAiApiKey
output OPENAI_ORGANIZATION string = openAiApiOrganization

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

output AZURE_AI_PROJECT string = useSafetyEval ? ai.outputs.projectName : ''
output WEB_URI string = web.outputs.SERVICE_WEB_URI
output INDEXER_URI string = indexer.outputs.SERVICE_INDEXER_URI
// output INDEXER_FUNCTIONAPP_NAME string = indexer.outputs.name
