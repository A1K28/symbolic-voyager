package com.github.a1k28.core.ai;

import com.github.a1k28.helper.IOHelper;
import com.github.a1k28.helper.Logger;
import com.github.a1k28.helper.RestClient;
import com.github.a1k28.model.ai.copilot.*;
import com.google.gson.Gson;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.*;

public class CopilotServiceImpl implements AIService {
    private volatile String token;
    private volatile String accessToken;
    private volatile LocalDateTime tokenLastUpdated;

    private static final Gson gson = new Gson();
    private static final Logger log = Logger.getInstance(CopilotServiceImpl.class);

    public CopilotServiceImpl() throws URISyntaxException, IOException, InterruptedException {
        loadAccessToken();
        if (accessToken == null) setup();

        updateToken();
        scheduleTokenUpdate();
    }

    @Override
    public String generate(String prompt, String language)
            throws URISyntaxException, IOException, InterruptedException {
        Map<String, String> headers = getPromptHeaders();

        CopilotPromptExtra extra = new CopilotPromptExtra();
        extra.setLanguage(language);

        CopilotPromptReq req = new CopilotPromptReq();
        req.setPrompt(prompt);
        req.setSuffix("");
        req.setMaxTokens(1_000);
        req.setTemperature(0);
        req.setTopP(1);
        req.setN(1);
        req.setStop(List.of("\\n"));
        req.setNwo("github/copilot.vim");
        req.setStream(true);
        req.setExtra(extra);

        String promptResponse = RestClient.sendRequest(
                "https://copilot-proxy.githubusercontent.com/v1/engines/copilot-codex/completions",
                null,
                headers,
                req,
                RestClient.Method.POST,
                10
        );

        StringBuilder sb = new StringBuilder();
        for (String line : promptResponse.split("\n")) {
            if (line.startsWith("data: {")) {
                String data = line.substring(6);
                CopilotPromptData promptData = gson.fromJson(data, CopilotPromptData.class);
                if (promptData != null
                        && promptData.getChoices() != null
                        && !promptData.getChoices().isEmpty()) {
                    String text = promptData.getChoices().get(0).getText();
                    if (text != null && !text.isEmpty()) sb.append(text);
                    else sb.append("\n");
                }
            }
        }

        return sb.toString();
    }

    private void loadAccessToken() {
        try {
            accessToken = IOHelper.readFile("access-token.txt").strip();
        } catch (Exception e) {
            log.error("An exeption has occurred when reading access token", e);
        }
    }

    private void scheduleTokenUpdate() {
        TimerTask task = new TimerTask() {
            public void run() {
                log.info("Running scheduled task: update token");
                try {
                    updateToken();
                } catch (Exception e) {
                    log.error("Could not update token", e);
                }
                log.info("Task performed on: " + new Date() + "\n"
                        + "Thread's name: " + Thread.currentThread().getName());
            }
        };
        Timer timer = new Timer("Timer");
        long delay = 25*60*1000L;
        timer.scheduleAtFixedRate(task, delay, delay);
    }

    private void updateToken() throws URISyntaxException, IOException, InterruptedException {
        LocalDateTime now = LocalDateTime.now();
        if (shouldUpdateToken(now)) {
            synchronized (this) {
                if (shouldUpdateToken(now)) {
                    token = getToken();
                    tokenLastUpdated = now;
                }
            }
        }
    }

    private void setup() throws URISyntaxException, IOException, InterruptedException {
        CopilotVerificationModel verificationModel = getVerification();
        log.info("Please visit " + verificationModel.getVerificationUri()
                + " and enter code " + verificationModel.getUserCode()
                + " to authenticate.");
        accessToken = getAccessToken(verificationModel.getDeviceCode()).getAccessToken();
        IOHelper.writeFile("access-token.txt", accessToken);
    }

    private boolean shouldUpdateToken(LocalDateTime now) {
        return token == null
                || accessToken == null
                || tokenLastUpdated == null
                || tokenLastUpdated.isBefore(now.minusMinutes(25));
    }

    private String getToken()
            throws URISyntaxException, IOException, InterruptedException {
        Map<String, String> headers = getTokenHeaders();
        String tokenResponse = RestClient.sendRequest(
                "https://api.github.com/copilot_internal/v2/token",
                null,
                headers,
                null,
                RestClient.Method.GET,
                10
        );
        return gson.fromJson(tokenResponse, CopilotToken.class).getToken();
    }

    private CopilotAccessToken getAccessToken(String deviceCode) {
        Map<String, String> headers = getSetupHeaders();

        CopilotAccessTokenReq req = new CopilotAccessTokenReq();
        req.setClientId("Iv1.b507a08c87ecfe98");
        req.setDeviceCode(deviceCode);
        req.setGrantType("urn:ietf:params:oauth:grant-type:device_code");

        for(int i = 0; i < 10; i++) {
            try {
                Thread.sleep(10*1_000);
                String accessTokenResponse = RestClient.sendRequest(
                        "https://github.com/login/oauth/access_token",
                        null,
                        headers,
                        req,
                        RestClient.Method.POST,
                        10
                );
                CopilotAccessToken accessToken = gson.fromJson(accessTokenResponse, CopilotAccessToken.class);
                if (accessToken != null && accessToken.getAccessToken() != null) return accessToken;
            } catch (Exception e) {
                log.error("Exception when trying to acquire an access token", e);
            }
        }

        throw new RuntimeException("Could not acquire an access token");
    }

    private CopilotVerificationModel getVerification()
            throws URISyntaxException, IOException, InterruptedException {
        Map<String, String> headers = getSetupHeaders();

        CopilotVerificationReq req = new CopilotVerificationReq();
        req.setClientId("Iv1.b507a08c87ecfe98");
        req.setScope("read:user");

        String verificationResponse = RestClient.sendRequest(
                "https://github.com/login/device/code",
                null,
                headers,
                req,
                RestClient.Method.POST,
                10
        );
        return gson.fromJson(verificationResponse, CopilotVerificationModel.class);
    }

    private Map<String, String> getPromptHeaders() {
        Map<String, String> map = new HashMap<>();
        map.put("authorization", "Bearer " + token);
        return map;
    }

    private Map<String, String> getTokenHeaders() {
        Map<String, String> map = new HashMap<>();
        map.put("authorization", "token " + accessToken);
        map.put("editor-version", "Neovim/0.6.1");
        map.put("editor-plugin-version", "copilot.vim/1.16.0");
        map.put("user-agent", "GithubCopilot/1.155.0");
        return map;
    }

    private Map<String, String> getSetupHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("accept", "application/json");
        headers.put("editor-version", "Neovim/0.6.1");
        headers.put("editor-plugin-version", "copilot.vim/1.16.0");
        headers.put("content-type", "application/json");
        headers.put("user-agent", "GithubCopilot/1.155.0");
//        headers.put("accept-encoding", "gzip,deflate,br");
        return headers;
    }
}
