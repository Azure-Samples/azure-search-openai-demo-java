// This file is for doing static analysis and contains sensible defaults
// for PSRule to minimise false-positives and provide the best results.

// This file is not intended to be used as a runtime configuration file.

targetScope = 'subscription'

param environmentName string = 'testing'
param location string = 'swedencentral'

module main 'main.bicep' = {
  name: 'main'
  params: {
    environmentName: environmentName
    location: location
    openAiHost: 'azure'
    openAiResourceGroupLocation: location
    searchIndexName: 'gptkbindex'
    searchQueryLanguage: 'en-us'
    searchQuerySpeller: 'lexicon'
    searchServiceSkuName: 'standard'
    storageSkuName: 'Standard_LRS'
    useApplicationInsights: false
    chatGptDeploymentName: 'chat'
    embeddingDeploymentName: 'embedding'
  }
}
