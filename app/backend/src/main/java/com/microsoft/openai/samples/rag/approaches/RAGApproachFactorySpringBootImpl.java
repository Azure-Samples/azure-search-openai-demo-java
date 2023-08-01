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
    private static String CHAT_READ_RETRIEVE_READ = "crrr";
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
    public RAGApproach createApproach(String approachName) {

        if(RETRIEVE_THEN_READ.equals(approachName)) {
            return applicationContext.getBean(RetrieveThenReadApproach.class);
        } else if(CHAT_READ_RETRIEVE_READ.equals(approachName)) {
            return applicationContext.getBean(ChatReadRetrieveReadApproach.class);
        } else if(READ_RETRIEVE_READ.equals(approachName)) {
            return applicationContext.getBean(ReadRetrieveReadApproach.class);}
        else {
            throw new IllegalArgumentException("Invalid approach name: " + approachName);
        }
    }
}
