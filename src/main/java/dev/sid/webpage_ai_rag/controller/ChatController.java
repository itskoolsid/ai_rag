package dev.sid.webpage_ai_rag.controller;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.sid.webpage_ai_rag.document.factory.HtmlDocumentFactory;
import dev.sid.webpage_ai_rag.document.factory.PdfDocumentFactory;
import reactor.core.publisher.Flux;

@RestController()
@RequestMapping(value = "/web-ai-rag")
public class ChatController {

  @Value("classpath:prompts/prompt_template.st")
  private Resource promptTemplateResource;

  private final ChatClient chatClient;
  private final VectorStore vectorStore;

  public ChatController(ChatClient.Builder builder, HtmlDocumentFactory htmlDocumentFactory, PdfDocumentFactory pdfDocumentFactory, VectorStore vectorStore) {
    this.chatClient = builder.build();
    this.vectorStore = vectorStore;
  }

  @GetMapping(value = "/chat")
  public Flux<String> testClient(@RequestParam(value = "query", defaultValue = "What is RAG?") final String query) {
    var promptTemplate = new PromptTemplate(promptTemplateResource);
    Map<String, Object> promptParams = Map.of("input", query, "documents", String.join("\n", findSimilaritySearch(query)));
    return chatClient.prompt(promptTemplate.create(promptParams))
        .stream()
        .content();
  }

  private List<String> findSimilaritySearch(final String message) {
    return Stream.ofNullable(vectorStore.similaritySearch(SearchRequest.builder().query(message).topK(3).build()))
        .flatMap(Collection::stream)
        .map(Document::getText)
        .toList();
  }
}
