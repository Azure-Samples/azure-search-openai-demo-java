using './main.bicep'

param environmentName = '\${AZURE_ENV_NAME}'

param resourceGroupName = '\${AZURE_RESOURCE_GROUP}'

param location = '\${AZURE_LOCATION}'

param kubernetesVersion = '\${AZURE_KUBERNETES_VERSION=1.33}'

param principalId = '\${AZURE_PRINCIPAL_ID}'

param principalType = '\${AZURE_PRINCIPAL_TYPE=User}'

param openAiServiceName = '\${AZURE_OPENAI_SERVICE}'

param openAiResourceGroupName = '\${AZURE_OPENAI_RESOURCE_GROUP}'

param customOpenAiResourceGroupLocation = '\${AZURE_OPENAI_SERVICE_LOCATION}'

param openAiSkuName = 'S0'

param documentIntelligenceServiceName = '\${AZURE_DOCUMENT_INTELLIGENCE_SERVICE}'

param documentIntelligenceResourceGroupName = '\${AZURE_DOCUMENT_INTELLIGENCE_RESOURCE_GROUP}'

param documentIntelligenceSkuName = 'S0'

param searchIndexName = '\${AZURE_SEARCH_INDEX=gptkbindex}'

param searchServiceName = '\${AZURE_SEARCH_SERVICE}'

param searchServiceResourceGroupName = '\${AZURE_SEARCH_SERVICE_RESOURCE_GROUP}'

param searchServiceLocation = '\${AZURE_SEARCH_SERVICE_LOCATION}'

param searchServiceSkuName = '\${AZURE_SEARCH_SERVICE_SKU=standard}'

param searchQueryLanguage = '\${AZURE_SEARCH_QUERY_LANGUAGE=en-us}'

param searchQuerySpeller = '\${AZURE_SEARCH_QUERY_SPELLER=lexicon}'

param storageAccountName = '\${AZURE_STORAGE_ACCOUNT}'

param storageResourceGroupName = '\${AZURE_STORAGE_RESOURCE_GROUP}'

param storageSkuName = '\${AZURE_STORAGE_SKU=Standard_LRS}'

param chatGptModelName = '\${AZURE_OPENAI_CHATGPT_MODEL=gpt-4o-mini}'

param chatGptModelVersion = '\${AZURE_OPENAI_CHATGPT_VERSION=2024-07-18}'

param chatGptDeploymentName = '\${AZURE_OPENAI_CHATGPT_DEPLOYMENT=gpt-4o-mini}'

param chatGptDeploymentCapacity = '\${AZURE_OPENAI_CHATGPT_DEPLOYMENT_CAPACITY=80}'

param chatGptDeploymentSkuName = '\${AZURE_OPENAI_CHATGPT_DEPLOYMENT_SKU_NAME=Standard}'

param embeddingModelName = '\${AZURE_OPENAI_EMB_MODEL=text-embedding-3-large}'

param embeddingModelVersion = '\${AZURE_OPENAI_EMB_MODEL_VERSION=1}'

param embeddingDeploymentName = '\${AZURE_OPENAI_EMB_DEPLOYMENT=text-embedding-3-large}'

param embeddingDeploymentSkuName = '\${AZURE_OPENAI_EMB_DEPLOYMENT_SKU=GlobalStandard}'

param embeddingDeploymentCapacity = '\${AZURE_OPENAI_EMB_DEPLOYMENT_CAPACITY=80}'

param embeddingDimensions = '\${AZURE_OPENAI_EMB_DIMENSIONS=3072}'

param openAiHost = '\${OPENAI_HOST=azure}'

param openAiApiKey = '\${OPENAI_API_KEY}'

param openAiApiOrganization = '\${OPENAI_ORGANIZATION}'

param useApplicationInsights = '\${AZURE_USE_APPLICATION_INSIGHTS=true}'

param useChatHistoryBrowser = '\${USE_CHAT_HISTORY_BROWSER=false}'

param useChatHistoryCosmos = '\${USE_CHAT_HISTORY_COSMOS=false}'

param enableGlobalDocumentAccess = '\${AZURE_ENABLE_GLOBAL_DOCUMENT_ACCESS=true}'

param cosmosDbSkuName = '\${AZURE_COSMOSDB_SKU=serverless}'

param cosmodDbResourceGroupName = '\${AZURE_COSMOSDB_RESOURCE_GROUP}'

param cosmosDbLocation = '\${AZURE_COSMOSDB_LOCATION}'

param cosmosDbAccountName = '\${AZURE_COSMOSDB_ACCOUNT}'

param cosmosDbThroughput = '\${AZURE_COSMOSDB_THROUGHPUT}'

param useAuthentication = '\${AZURE_USE_AUTHENTICATION=false}'

param authTenantId = '\${AZURE_AUTH_TENANT_ID}'

param serverAppId = '\${AZURE_SERVER_APP_ID}'

param clientAppId = '\${AZURE_CLIENT_APP_ID}'

param useServiceBusIndexing = '\${USE_SERVICEBUS_INDEXING=true}'
