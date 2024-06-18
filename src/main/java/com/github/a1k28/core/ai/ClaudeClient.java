package com.github.a1k28.core.ai;

import com.github.a1k28.helper.Logger;
import com.github.a1k28.model.ai.claude.ClaudeRequest;
import com.github.a1k28.model.ai.claude.ClaudeResponse;
import com.github.a1k28.model.ai.claude.ClaudeUsage;
import com.github.a1k28.model.ai.claude.input.ClaudeMessage;
import com.github.a1k28.model.ai.claude.input.RoleInput;
import com.github.a1k28.model.ai.claude.input.TextMessage;
import com.github.a1k28.model.ai.claude.types.ClaudeModel;
import com.github.a1k28.model.ai.claude.types.ClaudeRole;
import com.github.a1k28.model.ai.claude.types.ClaudeStopReason;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ClaudeClient implements AIService {
    private String apiKey = "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";
    private final HttpClient httpClient;
    private final static String API_URL = "https://api.anthropic.com/v1/messages";
    private static final URI URI = createURI();
    private final long timeoutInMs;
    private static final Logger log = Logger.getInstance(ClaudeClient.class);

    public ClaudeClient() {
        this.httpClient = HttpClient.newBuilder().build();
        this.timeoutInMs = 60_000L;
    }

    @Override
    public String generate(String prompt, String language) throws URISyntaxException, IOException, InterruptedException {
        String testCode = exampleTestableCode();
        String exampleTests = exampleResponse();
        ClaudeResponse response = sendRequest(ClaudeRequest.builder()
                .model(ClaudeModel.SONNET)
//                .max_tokens(1_000)
                .temperature(0)
                .stop_sequence("###")
                .system("You are an AI assistant, created to help analyze code and generate test cases. Your responses only include tests written in Java 17 with JUnit 5, without any additional description or text.")
                .input(RoleInput.builder()
                        .role(ClaudeRole.USER)
                        .message(new TextMessage("Analyze the code and generate java 17 test cases for the following class. Use JUnit 5 and possibly Mockito as needed. I already have some generated some mutants and the goal is to cover all these mutants with maximum code coverage. Please respond only with java 17 code."))
                        .message(new TextMessage(testCode))
                        .build())
                .input(RoleInput.builder()
                        .role(ClaudeRole.ASSISTANT)
                        .message(new TextMessage(exampleTests))
                        .build())
                .input(RoleInput.builder()
                        .role(ClaudeRole.USER)
                        .message(new TextMessage("Analyze the code and generate java 17 test cases for the following class. Use JUnit 5 and possibly Mockito as needed. I already have some generated some mutants and the goal is to cover all these mutants with maximum code coverage. Please respond only with java 17 code."))
                        .message(new TextMessage(prompt))
                        .build())
                .build());

        if (response == null || response.getContent() == null)
            throw new RuntimeException("Null response from AI");
        if (response.getContent().isEmpty())
            throw new RuntimeException("Empty response from AI");

        log.info(response.toString());
        return response.getContent().get(0);
//        TypeToken<IngredientModel[]> typeToken = TypeToken.get(IngredientModel[].class);
//        return new Gson().fromJson(response.getContent().getFirst(), typeToken);
    }

    private ClaudeResponse sendRequest(ClaudeRequest request) {
        if (request.getInputs().isEmpty()) throw new IllegalArgumentException("Inputs cannot be empty");
        JsonObject jsonResponse = sendRequestAndGetJson(request);
        if (jsonResponse == null) return null;
        return parseResponse(jsonResponse);
    }

    private CompletableFuture<ClaudeResponse> sendRequestAsync(ClaudeRequest request) {
        return CompletableFuture.supplyAsync(() -> sendRequest(request));
    }

    private ClaudeResponse parseResponse(JsonObject json) {
        String id = json.get("id").getAsString();
        String type = json.get("type").getAsString();
        String roleId = json.get("role").getAsString();
        ClaudeRole role = ClaudeRole.fromId(roleId);
        JsonArray content = json.getAsJsonArray("content");
        List<String> contentList = new ArrayList<>();
        for (JsonElement jsonElement : content) {
            String text = jsonElement.getAsJsonObject().get("text").getAsString();
            contentList.add(text);
        }
        String model = json.get("model").getAsString();
        ClaudeStopReason stop_reason = ClaudeStopReason.fromId(json.get("stop_reason").getAsString());
        JsonElement stop_sequence_element = json.get("stop_sequence");
        String stop_sequence = stop_sequence_element.isJsonNull() ? null : stop_sequence_element.getAsString();
        JsonObject usageObject = json.getAsJsonObject("usage");
        int input_tokens = usageObject.get("input_tokens").getAsInt();
        int output_tokens = usageObject.get("output_tokens").getAsInt();
        ClaudeUsage usage = new ClaudeUsage(input_tokens, output_tokens);
        return new ClaudeResponse(id, type, role, contentList, model, stop_reason, stop_sequence, usage);
    }

    private JsonObject sendRequestAndGetJson(ClaudeRequest request) {
        JsonObject jsonObject = generateJsonRequest(request);
        String json = jsonObject.toString();
        HttpRequest httpRequest = HttpRequest.newBuilder().uri(URI)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .header("content-type", "application/json")
                .timeout(Duration.of(timeoutInMs, ChronoUnit.MILLIS))
                .build();
        try {
            log.info("Sending request to Claude URL: " + URI + " with body: " + json);
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("Response code: " + response.statusCode() + " Response: " + response.body());
                return null;
            }
            JsonElement element = JsonParser.parseString(response.body());
            return element.getAsJsonObject();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    public JsonObject generateJsonRequest(ClaudeRequest request) {
        JsonObject object = new JsonObject();
        object.addProperty("model", request.getModel().getId());
        object.addProperty("max_tokens", request.getMax_tokens());
        object.addProperty("temperature", request.getTemperature());

        if (request.getStop_sequences() != null && !request.getStop_sequences().isEmpty()) {
            JsonArray stopSequences = new JsonArray();
            for (String stopSequence : request.getStop_sequences()) {
                stopSequences.add(stopSequence);
            }
            object.add("stop_sequences", stopSequences);
        }

        if (request.getSystem() != null && !request.getSystem().isEmpty())
            object.addProperty("system", request.getSystem());
        JsonArray messages = new JsonArray();
        for (RoleInput input : request.getInputs()) {
            //
            JsonObject messageObject = new JsonObject();
            messageObject.addProperty("role", input.getRole().getId());
            //
            JsonArray messageContent = new JsonArray();
            for (ClaudeMessage content : input.getMessages()) {
                messageContent.add(content.toJsonObject());
            }
            //
            messageObject.add("content", messageContent);
            messages.add(messageObject);
        }

        object.add("messages", messages);
        return object;
    }

    private static URI createURI() {
        try {
            return new URI(ClaudeClient.API_URL);
        } catch (java.net.URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private String exampleTestableCode() {
        return """
                import java.util.EmptyStackException;
                               
                public class Stack<T> {
                    private int capacity = 10;
                    private int pointer  = 0;
                    private T[] objects = (T[]) new Object[capacity];
                               
                    public void push(T o) {
                        if(pointer >= capacity)
                            throw new RuntimeException("Stack exceeded capacity!");
                        objects[pointer++] = o;
                    }
                               
                    public T pop() {
                        if(pointer <= 0)
                            throw new EmptyStackException();
                        return objects[--pointer];
                    }
                               
                    public boolean isEmpty() {
                        return pointer <= 0;
                    }
                }
               """;
    }

    private String exampleResponse() {
        return """
                import org.junit.jupiter.api.BeforeEach;
                import org.junit.jupiter.api.Test;
                import org.junit.jupiter.api.extension.ExtendWith;
                import org.mockito.Mock;
                import org.mockito.junit.jupiter.MockitoExtension;
                               
                import java.util.EmptyStackException;
                               
                import static org.junit.jupiter.api.Assertions.*;
                import static org.mockito.Mockito.when;
                               
                @ExtendWith(MockitoExtension.class)
                class StackTest {
                               
                    private Stack<String> stack;
                               
                    @Mock
                    private Object mockedObject;
                               
                    @BeforeEach
                    void setUp() {
                        stack = new Stack<>();
                    }
                               
                    @Test
                    void testPushAndPop() {
                        stack.push("Hello");
                        stack.push("World");
                        assertEquals("World", stack.pop());
                        assertEquals("Hello", stack.pop());
                        assertTrue(stack.isEmpty());
                    }
                               
                    @Test
                    void testPushExceedingCapacity() {
                        for (int i = 0; i < 10; i++) {
                            stack.push("Element " + i);
                        }
                        assertThrows(RuntimeException.class, () -> stack.push("One more"));
                    }
                               
                    @Test
                    void testPopEmptyStack() {
                        assertThrows(EmptyStackException.class, () -> stack.pop());
                    }
                               
                    @Test
                    void testIsEmpty() {
                        assertTrue(stack.isEmpty());
                        stack.push("Element");
                        assertFalse(stack.isEmpty());
                        stack.pop();
                        assertTrue(stack.isEmpty());
                    }
                               
                    @Test
                    void testPushNullObject() {
                        stack.push(null);
                        assertNull(stack.pop());
                    }
                               
                    @Test
                    void testPushMockedObject(@Mock Object mockedObject) {
                        when(mockedObject.toString()).thenReturn("Mocked Object");
                        stack.push((T) mockedObject);
                        assertEquals("Mocked Object", stack.pop().toString());
                    }
                }
               """;
    }
}
