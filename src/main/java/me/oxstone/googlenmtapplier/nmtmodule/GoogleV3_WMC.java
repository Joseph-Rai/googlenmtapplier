package me.oxstone.googlenmtapplier.nmtmodule;

import com.google.cloud.translate.v3.TranslateTextRequest;
import com.google.cloud.translate.v3.TranslateTextResponse;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.Setter;
import me.oxstone.googlenmtapplier.nmtsettings.NmtSettings;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Setter
public class GoogleV3_WMC extends GoogleV3 {

    private static final String DEFAULT_URL = "https://translationapis.oxstone.co.kr/api/v1";
//    private static final String DEFAULT_URL = "http://localhost:8081/api/v1";

    RestTemplate restTemplate;

    public GoogleV3_WMC(NmtSettings nmtSettings) throws IOException {
        super(nmtSettings);
        restTemplate = new RestTemplate();
    }

    @Override
    public Map<String, String> batchTranslateText(Map<String, String> segmentMap) throws IOException, RuntimeException {
        if (!validateJsonKey()) {
            throw new RuntimeException("Json 파일이 유효하지 않습니다.");
        }

        TranslateTextRequest request = applySettingsToTranslateTextRequest();

        // 번역 API 요청
        TranslateTextRequest finalRequest = request;
        return asyncTranslateRequest(segmentMap, innerMap -> {
            TranslateTextRequest req = finalRequest.toBuilder()
                    .addAllContents(innerMap.values())
                    .build();
            TranslateTextResponse responseEntity = getTranslateTextResponse(req);
            List<String> translatedTexts = extractTextList(responseEntity);

            //chatGPT를 이용한 보정
            List<String> corrected = correctByChatGPT(translatedTexts);

            return generateTargetMap(innerMap, corrected);
        });
    }

    private List<String> correctByChatGPT(List<String> translatedTexts) {
        List<CompletableFuture<String>> futureList = new ArrayList<>();

        // translatedTexts를 순회하며 비동기 API 요청을 보냅니다.
        for (String text : translatedTexts) {
            CompletableFuture<String> future =
                    CompletableFuture.supplyAsync(() -> {
                        HttpHeaders headers = new HttpHeaders();
                        headers.setContentType(MediaType.TEXT_PLAIN);
                        HttpEntity<String> entity = new HttpEntity<>(text, headers);
                        ResponseEntity<String> response = restTemplate.exchange(DEFAULT_URL + "/chatGPT", HttpMethod.POST, entity, String.class);
                        return response.getBody();
                    });
            futureList.add(future);
        }

        // CompletableFuture 리스트에서 모든 결과를 기다리고 수집합니다.
        List<String> result = futureList.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());

        return result;
    }

    private boolean validateJsonKey() throws IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        StringBuilder sb = new StringBuilder();
        BufferedReader br = new BufferedReader(new FileReader(nmtSettings.getJson()));
        String line = null;
        while ((line = br.readLine()) != null) {
            sb.append(line);
            sb.append("\n");
        }
        String jsonString = sb.toString();

        HttpEntity<String> entity = new HttpEntity<>(jsonString, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(DEFAULT_URL + "/authenticate", entity, String.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public String translateText(String text) throws InvalidProtocolBufferException {
        TranslateTextRequest request = getDefaultRequest().toBuilder()
                .addContents(text).build();
        TranslateTextResponse response = getTranslateTextResponse(request);
        return extractTextOnly(response);
    }

    private TranslateTextResponse getTranslateTextResponse(TranslateTextRequest request) throws InvalidProtocolBufferException {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/octet-stream");
        HttpEntity<byte[]> entity = new HttpEntity<>(request.toByteArray(), headers);
        TranslateTextResponse response =
                byteArrayToTranslateTextRequest(
                        restTemplate.postForEntity(DEFAULT_URL + "/translate", entity, byte[].class).getBody()
                );
        return response;
    }

    public TranslateTextResponse byteArrayToTranslateTextRequest(byte[] bytes) throws InvalidProtocolBufferException {
        return TranslateTextResponse.parseFrom(bytes);
    }
}
