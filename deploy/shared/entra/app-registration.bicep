
//This is an experiment to use bicep to create an app registration. 
//Please use the python script auth_init.py until this work is finalized.

extension 'br:mcr.microsoft.com/bicep/extensions/microsoftgraph/v1.0:0.1.9-preview'
param displayName string = 'Azure Search OpenAI Chat Client App'
param signinAudience string = 'AzureADMyOrg'
// Generate an 8-character pseudo-random suffix based on the resource group
var randomSuffix = substring(uniqueString(resourceGroup().id), 0, 8)



resource application 'Microsoft.Graph/applications@v1.0' = {
  displayName: '${displayName}${randomSuffix}'
  uniqueName:  '${displayName}${randomSuffix}'
  signInAudience: signinAudience
  web: {
    redirectUris: [
      'http://localhost:50505/.auth/login/aad/callback'
    ]
    implicitGrantSettings: {
      enableAccessTokenIssuance: true
    }
  }
  spa: {
    redirectUris: [
      'http://localhost:5173/redirect'
    ]
  }
  requiredResourceAccess: [
    {
      resourceAppId: '00000003-0000-0000-c000-000000000000'
      resourceAccess: [
        {
          id: '7427a6b3-4c1d-4f2e-bb8f-5a9c1d8e2f3b'
          type: 'Scope'
        }
      ]
    }
  ]
}
