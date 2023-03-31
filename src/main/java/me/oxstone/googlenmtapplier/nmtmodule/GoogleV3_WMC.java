package me.oxstone.googlenmtapplier.nmtmodule;

import com.google.cloud.translate.v3.TranslateTextRequest;
import com.google.cloud.translate.v3.TranslateTextResponse;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.Setter;
import me.oxstone.googlenmtapplier.nmtsettings.NmtSettings;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Setter
public class GoogleV3_WMC extends GoogleV3 {

    private static final String DEFAULT_URL = "https://translationapis.oxstone.co.kr/api/v1";

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
            return generateTargetMap(innerMap, translatedTexts);
        });
    }

    private boolean validateJsonKey() throws IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/json");
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
