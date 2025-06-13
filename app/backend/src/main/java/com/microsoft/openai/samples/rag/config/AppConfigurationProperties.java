package com.microsoft.openai.samples.rag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppConfigurationProperties(
        boolean showGPT4VOptions,
        boolean showSemanticRankerOption,
        boolean showQueryRewritingOption,
        boolean showReasoningEffortOption,
        boolean streamingEnabled,
        String defaultReasoningEffort,
        boolean showVectorOption,
        boolean showUserUpload,
        boolean showLanguagePicker,
        boolean showSpeechInput,
        boolean showSpeechOutputBrowser,
        boolean showSpeechOutputAzure,
        boolean showChatHistoryBrowser,
        boolean showChatHistoryCosmos
) {}
