package com.microsoft.openai.samples.rag;

import com.azure.ai.openai.models.*;
import com.azure.core.http.HttpHeaderName;
import com.azure.core.http.HttpHeaders;
import com.azure.core.http.HttpMethod;
import com.azure.core.http.HttpRequest;
import com.azure.core.http.rest.PagedFlux;
import com.azure.core.http.rest.PagedResponse;
import com.azure.core.http.rest.PagedResponseBase;
import com.azure.core.implementation.serializer.DefaultJsonSerializer;
import com.azure.core.util.serializer.JsonSerializer;
import com.azure.search.documents.SearchDocument;
import com.azure.search.documents.models.SearchResult;
import com.azure.search.documents.util.SearchPagedFlux;
import com.azure.search.documents.util.SearchPagedResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import reactor.core.publisher.Mono;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class OpenAIUnitTestUtils {


    public Choice createChoice (String text, int index ){
        Constructor<Choice> pcc = null;
        try {
            pcc = Choice.class.getDeclaredConstructor(String.class, int.class, CompletionsLogProbabilityModel.class, CompletionsFinishReason.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("No constructor found for Choice.class",e);
        }
        pcc.setAccessible(true);
        Choice choice = null;
        try {
            choice = pcc.newInstance(text,index,null,CompletionsFinishReason.STOPPED);
        } catch (Exception e) {
            throw new RuntimeException("Cannot create Choice Instance",e);
        }

        return choice;
    }

    public CompletionsUsage createCompletionUsage (int completionTokens, int promptTokens, int totalTokens){
        Constructor<CompletionsUsage> pcc = null;
        try {
            pcc = CompletionsUsage.class.getDeclaredConstructor(int.class,int.class, int.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("No constructor found for CompletionsUsage.class",e);
        }
        pcc.setAccessible(true);
        CompletionsUsage completionsUsage = null;
        try {
            completionsUsage = pcc.newInstance(completionTokens,promptTokens,totalTokens);
        } catch (Exception e) {
            throw new RuntimeException("Cannot create CompletionsUsage Instance",e);
        }

        return completionsUsage;
    }


    public Completions createCompletions (List<Choice> choices){return this.createCompletions(choices,null);}
    public Completions createCompletions (List<Choice> choices, CompletionsUsage completionsUsage){
        Constructor<Completions> pcc = null;
        try {
            pcc = Completions.class.getDeclaredConstructor(String.class,int.class,List.class,CompletionsUsage.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("No constructor found for Completions.class",e);
        }
        pcc.setAccessible(true);
        Completions choice = null;
        try {
            choice = pcc.newInstance(null,0,choices,completionsUsage);
        } catch (Exception e) {
            throw new RuntimeException("Cannot create Completions Instance",e);
        }

        return choice;
    }

    public static void main(String[] args) {
   // var utils = new OpenAIUnitTestUtils();

    //List<Choice> choices = List.of(utils.createChoice(" this a response example",0));
    //Completions completions = utils.createCompletions(choices);



            String text = "You are an intelligent assistant helping Contoso Inc employees with their healthcare plan questions and employee handbook questions.\nUse 'you' to refer to the individual asking the questions even if they ask with 'I'.\\nAnswer the following question using only the data provided in the sources below.\\nFor tabular information return it as an html table. Do not return markdown format.\\nEach source has a name followed by colon and the actual information, always include the source name for each fact you use in the response.\\nIf you cannot answer using the sources below, say you don't know.\\n\\n###\\nQuestion: 'What is the deductible for the employee plan for a visit to Overlake in Bellevue?'\\n\\nSources:\\ninfo1.txt: deductibles depend on whether you are in-network or out-of-network. In-network deductibles are $500 for employee and $1000 for family. Out-of-network deductibles are $1000 for employee and $2000 for family.\\ninfo2.pdf: Overlake is in-network for the employee plan.\\ninfo3.pdf: Overlake is the name of the area that includes a park and ride near Bellevue.\\ninfo4.pdf: In-network institutions include Overlake, Swedish and others in the region\\n\\nAnswer:\\nIn-network deductibles are $500 for employee and $1000 for family [info1.txt] and Overlake is in-network for the employee plan [info2.pdf][info4.pdf].\\n\\n###\\nQuestion:'What happens in a performance review??'\\n\\nSources:\\nemployee_handbook-3.pdf:  Accountability: We take responsibility for our actions and hold ourselves and others accountable for their performance.8. Community: We are committed to making a positive impact in the communities in which we work and live.Performance ReviewsPerformance Reviews at Contoso ElectronicsAt Contoso Electronics, we strive to ensure our employees are getting the feedback they need to continue growing and developing in their roles. We understand that performance reviews are a key part of this process and it is important to us that they are conducted in an effective and efficient manner.Performance reviews are conducted annually and are an important part of your career development. During the review, your supervisor will discuss your performance over the past year and provide feedback on areas for improvement. They will also provide you with an opportunity to discuss your goals and objectives for the upcoming year.Performance reviews are a two-way dialogue between managers and employees. We encourage all employees to be honest and open during the review process, as it is an important opportunity to \\nemployee_handbook-3.pdf:  We encourage all employees to be honest and open during the review process, as it is an important opportunity to discuss successes and challenges in the workplace.We aim to provide positive and constructive feedback during performance reviews. This feedback should be used as an opportunity to help employees develop and grow in their roles.Employees will receive a written summary of their performance review which will be discussed during the review session. This written summary will include a rating of the employee’s performance, feedback, and goals and objectives for the upcoming year.We understand that performance reviews can be a stressful process. We are committed to making sure that all employees feel supported and empowered during the process. We encourage all employees to reach out to their managers with any questions or concerns they may have.We look forward to conducting performance reviews with all our employees. They are an important part of our commitment to helping our employees grow and develop in their roles.\\nrole_library-14.pdf: appropriate strategies• Foster a culture of engagement, diversity, and inclusion• Lead HR team to provide coaching and guidance to all employees • Manage performance review process and identify areas for improvement• Provide guidance and support to managers on disciplinary action• Maintain employee records and manage payrollQUALIFICATIONS:• Bachelor’s degree in Human Resources, Business Administration, or related field • At least 8 years of experience in Human Resources, including at least 5 years in a managerial role• Knowledgeable in Human Resources principles, theories, and practices• Excellent communication and interpersonal skills• Ability to lead, motivate, and develop a high-performing HR team• Strong analytical and problem-solving skills• Ability to handle sensitive information with discretion• Proficient in Microsoft Office SuiteDirector of Research and DevelopmentJob Title: Director of Research and Development, Contoso ElectronicsPosition Summary:The Director of Research and Development is a critical leadership role in Contoso Electronics. This position is responsible for leading the research, development and innovation of our products and services.\\n\\n\\nAnswer:\\n";

            System.out.println(text);

        }



}
