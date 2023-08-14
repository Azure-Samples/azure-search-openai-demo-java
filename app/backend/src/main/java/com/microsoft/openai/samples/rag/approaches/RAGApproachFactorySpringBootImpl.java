package com.microsoft.openai.samples.rag.approaches;

import com.microsoft.openai.samples.rag.ask.approaches.RetrieveThenReadApproach;
import com.microsoft.openai.samples.rag.ask.approaches.semantickernel.ReadRetrieveReadApproach;
import com.microsoft.openai.samples.rag.chat.approaches.ChatReadRetrieveReadApproach;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class RAGApproachFactorySpringBootImpl implements RAGApproachFactory, ApplicationContextAware {
    private ApplicationContext applicationContext;

    private static String READ_RETRIEVE_READ = "rrr";
    private static String RETRIEVE_THEN_READ = "rtr";

    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }
    /**
     * Retrieve a specific approach bean definition based on approachName
     * @param approachName
     * @return
     */
    @Override
    public RAGApproach createApproach(String approachName, RAGType ragType) {

        if (ragType.equals(RAGType.CHAT) && READ_RETRIEVE_READ.equals(approachName)) {
            return applicationContext.getBean(ChatReadRetrieveReadApproach.class);

        } else if (ragType.equals(RAGType.ASK)) {
            if (RETRIEVE_THEN_READ.equals(approachName))
                return applicationContext.getBean(RetrieveThenReadApproach.class);
            else if (READ_RETRIEVE_READ.equals(approachName))
                return applicationContext.getBean(ReadRetrieveReadApproach.class);
        }
        //if this point is reached then the combination of approach and rag type is not supported
        throw new IllegalArgumentException("Invalid combination for approach[%s] and rag type[%s]: ".formatted(approachName, ragType));
    }
}
