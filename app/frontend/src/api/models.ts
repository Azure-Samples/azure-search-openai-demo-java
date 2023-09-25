export const enum Approaches {
    JAVA_OPENAI_SDK = "jos",
    JAVA_SEMANTIC_KERNEL = "jsk",
    JAVA_SEMANTIC_KERNEL_VECTORS = "jskv"
}

export const enum RetrievalMode {
    Hybrid = "hybrid",
    Vectors = "vectors",
    Text = "text"
}

export type AskRequestOverrides = {
    retrievalMode?: RetrievalMode;
    semanticRanker?: boolean;
    semanticCaptions?: boolean;
    excludeCategory?: string;
    top?: number;
    temperature?: number;
    promptTemplate?: string;
    promptTemplatePrefix?: string;
    promptTemplateSuffix?: string;
    suggestFollowupQuestions?: boolean;
};

export type AskRequest = {
    question: string;
    approach: Approaches;
    overrides?: AskRequestOverrides;
};

export type AskResponse = {
    answer: string;
    thoughts: string | null;
    data_points: string[];
    error?: string;
};

export type ChatTurn = {
    user: string;
    bot?: string;
};

export type ChatRequest = {
    history: ChatTurn[];
    approach: Approaches;
    overrides?: AskRequestOverrides;
    shouldStream?: boolean;
};
