package com.microsoft.openai.samples.rag.ask.approaches;

import com.microsoft.openai.samples.rag.approaches.ContentSource;

import java.util.ArrayList;
import java.util.List;

public class SemanticSearchAskPrompt {
    private List<String> sources = new ArrayList<>();
    private String question;

    final private String promptTemplate = """
        You are an intelligent assistant helping Contoso Inc employees with their healthcare plan questions and employee handbook questions.
        Use 'you' to refer to the individual asking the questions even if they ask with 'I'.
        Answer the following question using only the data provided in the sources below.
        For tabular information return it as an html table. Do not return markdown format.
        Each source has a name followed by colon and the actual information, always include the source name for each fact you use in the response.
        If you cannot answer using the sources below, say you don't know.
                    
        ###
        Question: 'What is the deductible for the employee plan for a visit to Overlake in Bellevue?'
                    
        Sources:
        info1.txt: deductibles depend on whether you are in-network or out-of-network. In-network deductibles are $500 for employee and $1000 for family. Out-of-network deductibles are $1000 for employee and $2000 for family.
        info2.pdf: Overlake is in-network for the employee plan.
        info3.pdf: Overlake is the name of the area that includes a park and ride near Bellevue.
        info4.pdf: In-network institutions include Overlake, Swedish and others in the region
                    
        Answer:
        In-network deductibles are $500 for employee and $1000 for family [info1.txt] and Overlake is in-network for the employee plan [info2.pdf][info4.pdf].
                    
        ###
        Question:'%s?'
                    
        Sources:
        %s
                    
        Answer:
        """ ;

    public SemanticSearchAskPrompt(List<ContentSource> sources, String question) {
        if (sources == null  || sources.isEmpty())
            throw new IllegalStateException("sources cannot be null or empty");

        if (question == null  || question.isEmpty())
            throw new IllegalStateException("question cannot be null or empty.");

        sources.iterator().forEachRemaining(source -> this.sources.add(source.getSourceName()+": "+source.getSourceContent()));
        this.question = question;
    }

    public  String getFormattedPrompt() {
        if (this.sources == null  || this.sources.isEmpty())
            throw new IllegalStateException("sources cannot be null or empty. Please use setSources() before calling getFormattedPrompt()");

        if (this.question == null  || this.question.isEmpty())
            throw new IllegalStateException("question cannot be null or empty. Please use setQuestion() before calling getFormattedPrompt()");

        StringBuffer sourcesText = new StringBuffer();
        sources.iterator().forEachRemaining(source -> sourcesText.append(source).append("\n"));

        return promptTemplate.formatted(question, sourcesText.toString());

    }

}
