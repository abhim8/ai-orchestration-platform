package com.aiorchestration.common.context;

import org.slf4j.MDC;

public final class ConversationContext {

    private static final String MDC_KEY = "conversationId";

    private ConversationContext() {
    }

    public static void setConversationId(final String conversationId) {
        MDC.put(MDC_KEY, conversationId);
    }

    public static String getConversationId() {
        return MDC.get(MDC_KEY);
    }

    public static void clear() {
        MDC.remove(MDC_KEY);
    }
}
