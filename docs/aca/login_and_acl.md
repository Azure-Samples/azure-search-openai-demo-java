# RAG chat: Setting up optional login and document level access control

The [azure-search-openai-demo-java](/) project can set up a full RAG chat app on Azure AI Search and Azure OpenAI so that you can chat on custom data, like internal enterprise data or domain-specific knowledge sets. For full instructions on setting up the project, consult the [main README](/README.md), and then return here for detailed instructions on configuring login and access control.

## Table of Contents

- [Requirements](#requirements)
- [Setting up Microsoft Entra applications](#setting-up-microsoft-entra-applications)
  - [Automatic Setup](#automatic-setup)
  - [Manual Setup](#manual-setup)
    - [Server App](#server-app)
    - [Client App](#client-app)
    - [Configure Server App Known Client Applications](#configure-server-app-known-client-applications)
    - [Testing](#testing)
    - [Programmatic Access With Authentication](#programmatic-access-with-authentication)
  - [Troubleshooting](#troubleshooting)
- [Adding data with document level access control](#adding-data-with-document-level-access-control)
  - [Using the Add Documents API](#using-the-add-documents-api)
  - [Azure Data Lake Storage Gen2 and prepdocs](#azure-data-lake-storage-gen2-setup)
- [Environment variables reference](#environment-variables-reference)
  - [Authentication behavior by environment](#authentication-behavior-by-environment)

This guide demonstrates how to add an optional login and document level access control system to the sample. This system can be used to restrict access to indexed data to specific users based their [user object id](https://learn.microsoft.com/partner-center/find-ids-and-domain-names#find-the-user-object-id).

![AppLoginArchitecture](/docs/images/applogincomponents.png)

## Requirements

**IMPORTANT:** In order to add optional login and document level access control, you'll need the following in addition to the normal sample requirements

- **Azure account permissions**: Your Azure account must have [permission to manage applications in Microsoft Entra](https://learn.microsoft.com/entra/identity/role-based-access-control/permissions-reference#cloud-application-administrator).

## Setting up Microsoft Entra applications

Two Microsoft Entra applications must be registered in order to make the optional login and document level access control system work correctly. One app is for the client UI. The client UI is implemented as a [single page application](https://learn.microsoft.com/entra/identity-platform/scenario-spa-app-registration). The other app is for the API server that needs to be accessed by the UI (server rest API is protected by authentication using oauth2 JWT).The indexer app is not protected.

### Automatic Setup

The easiest way to setup the two apps is to use the `azd` CLI. We've written scripts that will automatically create the two apps and configure them for use with the sample. To trigger the automatic setup, run the following commands:

1. **Enable authentication for the app**
  Run the following command to show the login UI and use Entra authentication by default:

    ```shell
    azd env set AZURE_USE_AUTHENTICATION true
    azd env set AZURE_AUTH_TENANT_ID <YOUR-TENANT-ID>
    ```
  make sure to provide your [Azure Entra tenant ID](https://learn.microsoft.com/en-us/entra/fundamentals/how-to-find-tenant).


1. (Optional) **Allow global document access**
  Unauthenticated users can search documents that have been pre-loaded from the data folder. Such documents are stored in **default** blob folder in Azure Storage.
  Authenticated users , by default, can search only through documents that they have uploaded through "Manage file Uploads". Such documents are stored in **user-oid** specific blob folder in Azure Storage.
  To make documents stored in **default** folder available for search to authenticated users you need to run the following command:

    ```shell
    azd env set AZURE_ENABLE_GLOBAL_DOCUMENT_ACCESS true
    ```


1. **Login to the authentication tenant (if needed)**
  If your auth tenant ID is different from your currently logged in tenant ID, run:

    ```shell
    azd auth login --tenant-id <YOUR-TENANT-ID>
    ```

1. **Deploy the app**
  Finally, run the following command to provision and deploy the app:

    ```shell
    azd up
    ```

### Manual Setup

The following instructions explain how to setup the apps registration on Azure Entra using the Azure Portal and configure the related azd env variables.

#### Server App

- Sign in to the [Azure portal](https://portal.azure.com/).
- Select the Microsoft Entra ID service.
- In the left hand menu, select **Application Registrations**.
- Select **New Registration**.
  - In the **Name** section, enter a meaningful application name. This name will be displayed to users of the app, for example `Azure Search OpenAI Chat API`.
  - Under **Supported account types**, select **Accounts in this organizational directory only**.
- Select **Register** to create the application
- In the app's registration screen, find the **Application (client) ID**.
  - Run the following `azd` command to save this ID: `azd env set AZURE_SERVER_APP_ID <Application (client) ID>`.

- Microsoft Entra supports three types of credentials to authenticate an app using the [client credentials](https://learn.microsoft.com/entra/identity-platform/v2-oauth2-client-creds-grant-flow): passwords (app secrets), certificates, and federated identity credentials. For a higher level of security, either [certificates](https://learn.microsoft.com/entra/identity-platform/howto-create-self-signed-certificate) or federated identity credentials are recommended. This sample currently uses an app secret for ease of provisioning.

- Select **Certificates & secrets** in the left hand menu.
- In the **Client secrets** section, select **New client secret**.
  - Type a description, for example `Azure Search OpenAI Chat Key`.
  - Select one of the available key durations.
  - The generated key value will be displayed after you select **Add**.
  - Copy the generated key value and run the following `azd` command to save this ID: `azd env set AZURE_SERVER_APP_SECRET <generated key value>`.
- Select **API Permissions** in the left hand menu. By default, the [delegated `User.Read`](https://learn.microsoft.com/graph/permissions-reference#user-permissions) permission should be present. This permission is required to read the signed-in user's profile to get the security information used for document level access control. If this permission is not present, it needs to be added to the application.
  - Select **Add a permission**, and then **Microsoft Graph**.
  - Select **Delegated permissions**.
  - Search for and and select `User.Read`.
  - Select **Add permissions**.
- Select **Expose an API** in the left hand menu. The server app works by using the [On Behalf Of Flow](https://learn.microsoft.com/entra/identity-platform/v2-oauth2-on-behalf-of-flow#protocol-diagram), which requires the server app to expose at least 1 API.
  - The application must define a URI to expose APIs. Select **Add** next to **Application ID URI**.
    - By default, the Application ID URI is set to `api://<application client id>`. Accept the default by selecting **Save**.
  - Under **Scopes defined by this API**, select **Add a scope**.
  - Fill in the values as indicated:
    - For **Scope name**, use **access_as_user**.
    - For **Who can consent?**, select **Admins and users**.
    - For **Admin consent display name**, type **Access Azure Search OpenAI Chat API**.
    - For **Admin consent description**, type **Allows the app to access Azure Search OpenAI Chat API as the signed-in user.**.
    - For **User consent display name**, type **Access Azure Search OpenAI Chat API**.
    - For **User consent description**, type **Allow the app to access Azure Search OpenAI Chat API on your behalf**.
    - Leave **State** set to **Enabled**.
    - Select **Add scope** at the bottom to save the scope.

#### Client App

- Sign in to the [Azure portal](https://portal.azure.com/).
- Select the Microsoft Entra ID service.
- In the left hand menu, select **Application Registrations**.
- Select **New Registration**.
  - In the **Name** section, enter a meaningful application name. This name will be displayed to users of the app, for example `Azure Search OpenAI Chat Web App`.
  - Under **Supported account types**, select **Accounts in this organizational directory only**.
  - Under `Redirect URI (optional)` section, select `Single-page application (SPA)` in the combo-box and enter the following redirect URI:
    - If you are running the sample locally, add the endpoints `http://localhost:50505/redirect` and `http://localhost:5173/redirect`
    - If you are running the sample on Azure, add the endpoints provided by `azd up`: `https://<your-endpoint>.azurewebsites.net/redirect`.
    - If you are running the sample from Github Codespaces, add the Codespaces endpoint: `https://<your-codespace>-50505.app.github.dev/redirect`
- Select **Register** to create the application
- In the app's registration screen, find the **Application (client) ID**.
  - Run the following `azd` command to save this ID: `azd env set AZURE_CLIENT_APP_ID <Application (client) ID>`.
- In the left hand menu, select **Authentication**.
  - Under Web, add a redirect URI with the endpoint provided by `azd up`: `https://<your-endpoint>.azurewebsites.net/.auth/login/aad/callback`.
  - Under **Implicit grant and hybrid flows**, select **ID Tokens (used for implicit and hybrid flows)**
  - Select **Save**
- In the left hand menu, select **API permissions**. You will add permission to access the **access_as_user** API on the server app. This permission is required for the [On Behalf Of Flow](https://learn.microsoft.com/entra/identity-platform/v2-oauth2-on-behalf-of-flow#protocol-diagram) to work.
  - Select **Add a permission**, and then **My APIs**.
  - In the list of applications, select your server application **Azure Search OpenAI Chat API**
  - Ensure **Delegated permissions** is selected.
  - In the **Select permissions** section, select the **access_as_user** permission
  - Select **Add permissions**.
- Stay in the **API permissions** section and select **Add a permission**.
  - Select **Microsoft Graph**.
  - Select **Delegated permissions**.
  - Search for and select `User.Read`.
  - Select **Add permissions**.

#### Configure Server App Known Client Applications

Consent from the user must be obtained for use of the client and server app. The client app can prompt the user for consent through a dialog when they log in. The server app has no ability to show a dialog for consent. Client apps can be [added to the list of known clients](https://learn.microsoft.com/entra/identity-platform/v2-oauth2-on-behalf-of-flow#gaining-consent-for-the-middle-tier-application) to access the server app, so a consent dialog is shown for the server app.

- Navigate to the server app registration
- In the left hand menu, select **Manifest**
- Replace `"knownClientApplications": []` with `"knownClientApplications": ["<client application id>"]`
- Select **Save**



### Troubleshooting

- If your primary tenant restricts the ability to create Entra applications, you'll need to use a separate tenant to create the Entra applications. You can create a new tenant by following [these instructions](https://learn.microsoft.com/entra/identity-platform/quickstart-create-new-tenant). Then run `azd env set AZURE_AUTH_TENANT_ID <YOUR-AUTH-TENANT-ID>` before running `azd up`.
- If any Entra apps need to be recreated, you can avoid redeploying the app by [changing the app settings in the portal](https://learn.microsoft.com/azure/app-service/configure-common?tabs=portal#configure-app-settings). Any of the [required environment variables](#environment-variables-reference) can be changed. Once the environment variables have been changed, restart the web app.
- It's possible a consent dialog will not appear when you log into the app for the first time. If this consent dialog doesn't appear, you will be unable to use the security filters because the API server app does not have permission to read your authorization information. A consent dialog can be forced to appear by adding `"prompt": "consent"` to the `loginRequest` property in [`authentication.py`](/app/backend/core/authentication.py)
- It's possible that your tenant admin has placed a restriction on consent to apps with [unverified publishers](https://learn.microsoft.com/entra/identity-platform/publisher-verification-overview). In this case, only admins may consent to the client and server apps, and normal user accounts are unable to use the login system until the admin consents on behalf of the entire organization.
- It's possible that your tenant admin requires [admin approval of all new apps](https://learn.microsoft.com/entra/identity/enterprise-apps/manage-consent-requests). Regardless of whether you select the delegated or admin permissions, the app will not work without tenant admin consent. See this guide for [granting consent to an app](https://learn.microsoft.com/entra/identity/enterprise-apps/grant-admin-consent?pivots=portal).


## Environment variables reference

The following environment variables are used to setup the optional login and document level access control:

- `AZURE_USE_AUTHENTICATION`: Enables Entra ID login and document level access control. Set to true before running `azd up`.
- `AZURE_AUTH_TENANT_ID`: [Tenant ID](https://learn.microsoft.com/entra/fundamentals/how-to-find-tenant) associated with the Microsoft Entra tenant used for login and document level access control. Defaults to `AZURE_TENANT_ID` if not defined.
- `AZURE_ENABLE_GLOBAL_DOCUMENT_ACCESS`: Allows logged users to search on documents that have no access controls assigned and have been stored in the **default** blob.Unauthenticated users can search on documents that have no access control assigned. Unauthenticated users cannot search on documents with access control assigned.
- `AZURE_SERVER_APP_ID`: (Required) Application ID of the Microsoft Entra app for the API server.
- `AZURE_CLIENT_APP_ID`: Application ID of the Microsoft Entra app for the client UI.
