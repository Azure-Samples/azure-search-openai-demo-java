package com.microsoft.openai.samples.rag.chat.approaches;

import com.azure.core.util.ExpandableStringEnum;

import java.util.Collection;

public class ChatGPTMessage {
    private ChatGPTMessage.ChatRole role;
    private String content;
    public ChatGPTMessage(ChatGPTMessage.ChatRole role, String content) {

        this.role = role;
        this.content = content;
    }


    public ChatGPTMessage.ChatRole getRole() {
        return this.role;
    }


    public String getContent() {
        return this.content;
    }


    public final static class ChatRole extends ExpandableStringEnum<ChatGPTMessage.ChatRole> {


        public static final ChatGPTMessage.ChatRole SYSTEM = fromString("system");


        public static final ChatGPTMessage.ChatRole ASSISTANT = fromString("assistant");


        public static final ChatGPTMessage.ChatRole USER = fromString("user");


        public static ChatGPTMessage.ChatRole fromString(String name) {
            return fromString(name, ChatGPTMessage.ChatRole.class);
        }


        public static Collection<ChatGPTMessage.ChatRole> values() {
            return values(ChatGPTMessage.ChatRole.class);
        }
    }
}
