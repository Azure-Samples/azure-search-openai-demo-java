metadata description = 'Creates an Azure Key Vault.'
param name string
param location string = resourceGroup().location
param tags object = {}

param principalId string = ''

@description('Allow the key vault to be used for template deployment.')
param enabledForDeployment bool = false

resource keyVault 'Microsoft.KeyVault/vaults@2022-07-01' = {
  name: name
  location: location
  tags: tags
  properties: {
    tenantId: subscription().tenantId
    sku: { family: 'A', name: 'standard' }
    accessPolicies: !empty(principalId) ? [
      {
        objectId: principalId
        permissions: { 
          secrets: [ 'get', 'list', 'set' ] 
          certificates: [ 'get', 'list', 'import' ] }
        tenantId: subscription().tenantId
      }
    ] : []
    enabledForDeployment: enabledForDeployment
  }
}

output endpoint string = keyVault.properties.vaultUri
output id string = keyVault.id
output name string = keyVault.name
