package com.microsoft.openai.samples.rag.approaches;

import com.microsoft.openai.samples.rag.ask.approaches.PlainJavaAskApproach;
import com.microsoft.openai.samples.rag.ask.approaches.semantickernel.JavaSemanticKernelAskApproach;
import com.microsoft.openai.samples.rag.chat.approaches.PlainJavaChatApproach;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class RAGApproachFactorySpringBootImpl implements RAGApproachFactory, ApplicationContextAware {

    private static final String JAVA_OPENAI_SDK = "jos";
    private static final String JAVA_SEMANTIC_KERNEL = "jsk";

    private static final String JAVA_SEMANTIC_KERNEL_VECTORS = "jskv";
    private ApplicationContext applicationContext;

    /**
     * Retrieve a specific approach bean definition based on approachName
     *
     * @param approachName
     * @return
     */
    @Override
    public RAGApproach createApproach(String approachName, RAGType ragType) {

        if (ragType.equals(RAGType.CHAT) && JAVA_OPENAI_SDK.equals(approachName)) {
            return applicationContext.getBean(PlainJavaChatApproach.class);

        } else if (ragType.equals(RAGType.ASK)) {
            if (JAVA_OPENAI_SDK.equals(approachName))
                return applicationContext.getBean(PlainJavaAskApproach.class);
            else if (JAVA_SEMANTIC_KERNEL.equals(approachName))
                return applicationContext.getBean(JavaSemanticKernelAskApproach.class);
        }
        //if this point is reached then the combination of approach and rag type is not supported
        throw new IllegalArgumentException("Invalid combination for approach[%s] and rag type[%s]: ".formatted(approachName, ragType));
    }

    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

}
