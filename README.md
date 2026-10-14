---
page_type: sample
languages:
- azdeveloper
- java
- bicep
- typescript
- html
products:
- azure
- azure-openai
- active-directory
- azure-cognitive-search
- azure-app-service
- azure-container-apps
- azure-kubernetes-service
- azure-sdks
- github
- document-intelligence
- azure-monitor
- azure-pipelines
urlFragment: azure-search-openai-demo-java
name: RAG chat app with Azure OpenAI and Azure AI Search with Java and Langchain4J
description: A Java sample app that chats with your data using Azure AI services.
---
<!-- YAML front-matter schema: https://review.learn.microsoft.com/en-us/help/contribute/samples/process/onboarding?branch=main#supported-metadata-fields-for-readmemd -->

# RAG chat app with Azure AI services and Langchain4J

This repo is the Java version of the well known [ChatGPT + Enterprise data code sample](https://github.com/Azure-Samples/azure-search-openai-demo) originally written in python.

It demonstrates best practices for creating ChatGPT-like experiences over your own data using the Retrieval Augmented Generation pattern. It uses Azure OpenAI Service to access the ChatGPT model `gpt-4o-mini`, and Azure 0AI Search for data indexing and retrieval and Azure Document Intelligence for document structured text parsing.

This repository includes sample data so it's ready to try end to end. In this sample application we use a fictitious company called Contoso Electronics, and the experience allows its employees to ask questions about the benefits, internal policies, as well as job descriptions and roles.

## Features

* Chat interface with search result citations and follow-up questions.
* Documents uploads and management.
* Response streaming.
* Response metadata with source content and grounded prompt tracking, search keywords, token usages.
* Citation panel with page level detail (pdf, html, markdown).
* Several documents formats supported: pdf, html, markdown, Office (docx,xslx,pptx).
* Explores various retrievial options to help users evaluate search results: search score, search top results, text/vector/hybrid retrievial strategy, semantic caption and ranking (Azure AI search specific features).
* Chat history with Azure CosmosDB or browser local database (for unauthenticated users).
* Login and documents access control with EntraID.
* Paged document text extraction with table data support with Azure Document Intelligence.


![Chat screen](docs/chatscreen.png)


### Solution architecture and deployment options

![Microservice RAG Architecture](docs/aks/aks-hla.png)

This sample supports different architectural styles. It can be deployed as a microservice event driven architecture with web frontend, AI orchestration and document ingestion apps hosted by Azure Container Apps or Azure Kubernetes Service.
It uses [Langchain4J](https://github.com/langchain4j/langchain4j) AI orchestration java framework. 

> [!NOTE]  
> Previous java Semantic Kernel version has been moved in a dedicated [branch](https://github.com/Azure-Samples/azure-search-openai-demo-java/tree/semantic-kernel)

The solution components and Azure services are:

* The API app is implemented as springboot 2.7.x app using the Microsoft JDK.It implements the **Chat flow** in RAG pattern and provides both a synch and asynch streaming chat REST endpoint for the webapp.It's responsible for orchestrating the interaction between the LLM models (Azure Open AI) and the content retriever (Azure AI Search).
* The chat web app is built in React and deployed as a static web app on nginx. In ACA deployment option, Nginx act as reverse proxy for api calls to the API app. This also solves the CORS issue. In AKS deployment option, a managed Nginx ingress controller is used as entry point for both thw web app and api.
* The indexer APP is implemented as springboot 2.7.x app using the Microsoft JDK.It implement the **Ingestion/Indexing flow** in RAG pattern and it's responsible for indexing the data into Azure AI Search.It's asynchnously triggered by new BlobUploaded messages from serviceBus or synchrnously called by API to handle User document upload request from the web app. The indexer is also responsible for chunking the documents into smaller pieces, embed them and store them in the index. Azure Document Intelligence can be configured to extract text from PDF documents (including tables and images) or you can leverage [document parsers available in Langchain4j](https://docs.langchain4j.dev/tutorials/rag#document-parser). See [here for ingestion flow configuration example](./app/indexer/microservice/src/main/resources/application.yml).
* Azure AI Search is used as RAG retrieval system. Different search options are available: you have traditional full text (with semantic search) search, or vector based search and finally you can opt for hybrid search which brings together the best of the previous ones.
* EventGrid system topic is used to implement a real time mechanism to trigger the indexer app when a new document is uploaded to the Azure blob called **staging**.It's responsible to read BlobUploaded notification from azure storage container and push a message to the service bus queue containing the blob url. All the documents loaded through service bus messages will be moved to **default** blob folder and will be available to all users.
* CosmosDB is used to store chat conversations.

For specific deployment options see:
- [Azure Container Apps](docs/aca/README-ACA.md)
- [Azure Kubernetes Service](docs/aks/README-AKS.md)



## Getting Started

All the available architectures and implementations come with few options to start developing and running the application locally using dev containers or in the cloud using github codespaces. For more detailed instructions, please refer to the specific architecture implementation documentation:
 - [Azure Container Apps Getting Started](docs/aca/README-ACA.md#getting-started)
 - [Azure Kubernetes Service Getting Started](docs/aks/README-AKS.md#getting-started)


## Guidance
For platform specific guidance, please refer to the following documentation:
 - [Azure Container Apps Guidance](docs/aca/README-ACA.md#guidance)
 - [Azure Kubernetes Service Guidance](docs/aks/README-AKS.md#guidance)


This sample is designed to get you started quickly and let you experiment with Java RAG architectures on Azure. For production deployment, you can use the [Azure Application Landing Zones (LZA)](https://learn.microsoft.com/en-us/azure/cloud-adoption-framework/scenarios/app-platform/ready) to automatically deploy the solution in production leveraging best practices for security, monitoring, networking and operational excellence.

Check the [chat-with-your-data-lza-app-accelerator](https://github.com/dantelmomsft/chat-with-your-data-java-lza-app-accelerator) to see how you can deploy this solution on Azure Container Apps LZA.

> [!NOTE]  
> Support for deploying on landing zone is available for previous semantic kernel version at this [repo](https://github.com/Azure-Samples/azure-search-openai-demo-java/tree/semantic-kernel).
> A new version based on [Azure Verified Modules for Azure Container Apps](https://github.com/Azure/bicep-registry-modules/tree/main/avm/ptn/aca-lza/hosting-environment) is WIP :sunny: :cloud: :construction_worker_man.

![Azure Container Apps LZA deployment](docs/aca/aca-internal-java-ai.png)

### Resources

* [📖 Revolutionize your Enterprise Data with ChatGPT: Next-gen Apps w/ Azure OpenAI and AI Search](https://aka.ms/entgptsearchblog)
* [📖 Azure OpenAI Service](https://learn.microsoft.com/azure/cognitive-services/openai/overview)
* [📖 RAG in Azure AI Search](https://learn.microsoft.com/en-us/azure/search/retrieval-augmented-generation-overview)
* [📖 Azure OpenAI client library for Java](https://learn.microsoft.com/en-us/java/api/overview/azure/ai-openai-readme?view=azure-java-preview)
* [📖 Semantic Kernel for Java 1.0](https://devblogs.microsoft.com/semantic-kernel/java-1-0-release-candidate-for-semantic-kernel-now-available/)
* [📖 Evaluating a RAG chat App](https://github.com/Azure-Samples/ai-rag-chat-evaluator)
* [📖 Well Architected Framework on Azure OpenAI Service](https://learn.microsoft.com/en-us/azure/well-architected/service-guides/azure-openai)
* [📺 Chat with your data using Java and Azure Open AI](https://www.youtube.com/watch?v=mcftrg6L8Fs&t=57s)
* [📺 Vector Search, RAG, And Azure AI Search](https://www.youtube.com/watch?v=vuOA13Y_Qzk)
