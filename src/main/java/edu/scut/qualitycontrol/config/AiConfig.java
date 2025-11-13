package edu.scut.qualitycontrol.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.service.AiServices;
import edu.scut.qualitycontrol.service.GraphNarratorService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {
    @Bean
    public GraphNarratorService graphNarratorService(ChatLanguageModel chatModel, StreamingChatLanguageModel streamingModel) {
        return AiServices.builder(GraphNarratorService.class)
                .chatLanguageModel(chatModel)
                .streamingChatLanguageModel(streamingModel)
                .build();
    }
}