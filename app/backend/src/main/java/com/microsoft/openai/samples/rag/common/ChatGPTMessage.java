// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.openai.samples.rag.common;

import com.azure.core.util.ExpandableStringEnum;
import java.util.Collection;

public record ChatGPTMessage(ChatGPTMessage.ChatRole role, String content) {

    public static final class ChatRole extends ExpandableStringEnum<ChatRole> {
        public static final ChatGPTMessage.ChatRole SYSTEM = fromString("system");

        public static final ChatGPTMessage.ChatRole ASSISTANT = fromString("assistant");

        public static final ChatGPTMessage.ChatRole USER = fromString("user");

        public static ChatGPTMessage.ChatRole fromString(String name) {
            return fromString(name, ChatGPTMessage.ChatRole.class);
        }

        public static Collection<ChatRole> values() {
            return values(ChatGPTMessage.ChatRole.class);
        }
    }
}
