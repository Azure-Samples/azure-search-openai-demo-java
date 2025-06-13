// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.openai.samples.rag.model;

import com.azure.core.util.ExpandableStringEnum;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ResponseMessage(String content, ResponseMessage.ChatRole role) {

    public static final class ChatRole extends ExpandableStringEnum<ResponseMessage.ChatRole> {
        public static final ResponseMessage.ChatRole SYSTEM = fromString("system");

        public static final ResponseMessage.ChatRole ASSISTANT = fromString("assistant");

        public static final ResponseMessage.ChatRole USER = fromString("user");

        public static ResponseMessage.ChatRole fromString(String name) {
            return fromString(name, ResponseMessage.ChatRole.class);
        }
    }

}
