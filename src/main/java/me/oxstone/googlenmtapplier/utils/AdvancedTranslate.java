package me.oxstone.googlenmtapplier.utils;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.translate.v3.*;
import com.google.common.collect.Lists;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static me.oxstone.googlenmtapplier.utils.AdvancedTranslationSettings.EN;
import static me.oxstone.googlenmtapplier.utils.AdvancedTranslationSettings.KO;

public class AdvancedTranslate {

    public final AdvancedTranslationSettings settings;
    private final String json;
    private final String project;
    private final String model;
    private final String location;
    private final String glossary;
    private final TranslationServiceClient client;

    public AdvancedTranslate(AdvancedTranslationSettings settings) throws IOException {
        this.settings = settings;
        this.json = settings.getJson();
        this.project = settings.getProject();
        this.model = settings.getModel();
        this.location = settings.getLocation();
        this.glossary = settings.getGlossary();
        this.client = getDefaultClient();
    }

    /*
    * List에 삽입된 원본문장 일괄 번역요청
    */
    public Map<String, String> batchTranslateTextWithGlossaryAndModel(Map<String, String> segmentMap) throws IOException {
        TranslateTextRequest request = getDefaultRequest().toBuilder()
                .addAllContents(segmentMap.values()).build();
        List<String> translatedTexts = extractTextList(client.translateText(request));

        return generateTargetMap(segmentMap, translatedTexts);
    }

    /*
    * Segment Id + Target Text 조합의 Map 형성
    * Segment Id는 추후 Target Segment 정보를 수정할 때 식별자로 사용
    */
    private Map<String, String> generateTargetMap(Map<String, String> segmentMap, List<String> translatedTexts) {
        Map<String, String> result = new HashMap<>();
        int i = 0;
        for (Map.Entry<String, String> entry : segmentMap.entrySet()) {
            result.put(entry.getKey(), translatedTexts.get(i++));
        }
        return result;
    }

    /*
    *  단일문장 번역
    */
    public String translateTextWithGlossaryAndModel(String text) throws IOException {
        TranslateTextRequest request = getDefaultRequest().toBuilder()
                .addContents(text).build();
        return extractTextOnly(client.translateText(request));
    }

    /*
    * TranslateTextResponse에서 Target Text만 추출
    */
    private String extractTextOnly(TranslateTextResponse translateText) {
        return translateText.getGlossaryTranslationsList()
                .get(0).getTranslatedText();
    }

    /*
    *  TranslateTextResponse에서 Target Text List 추출
    */
    private List<String> extractTextList(TranslateTextResponse translateText) {
        return translateText.getGlossaryTranslationsList().stream()
                .map(t -> t.getTranslatedText())
                .collect(Collectors.toList());
    }

    /*
    *  Translation Service Client 기본 값으로 빌드
    */
    private TranslationServiceClient getDefaultClient() throws IOException {
        GoogleCredentials myCredentials = GoogleCredentials.fromStream(new FileInputStream(json))
                .createScoped(Lists.newArrayList("https://www.googleapis.com/auth/cloud-translation"));
        TranslationServiceSettings defaultSettings =
                TranslationServiceSettings.newBuilder()
                        .setCredentialsProvider(FixedCredentialsProvider.create(myCredentials))
                        .build();
        return getClient(defaultSettings);
    }

    /*
    * Translation Service Client 생성
    */
    private TranslationServiceClient getClient(TranslationServiceSettings translationServiceSettings) throws IOException {
        return TranslationServiceClient.create(translationServiceSettings);
    }

    /*
    * Google Translation V3 API 요청을 위한 Request 기본 값 설정
    */
    public TranslateTextRequest getDefaultRequest() {
        String strParent = LocationName.of(project, location).toString();
        return TranslateTextRequest.newBuilder()
                .setMimeType("text/plain")
                .setSourceLanguageCode(KO)
                .setTargetLanguageCode(EN)
                .setParent(strParent)
                .setModel(strParent + "/models/" + model)
                .setGlossaryConfig(
                        TranslateTextGlossaryConfig.newBuilder()
                                .setGlossary(strParent + "/glossaries/" + glossary)
                                .setIgnoreCase(true)
                                .build()
                )
                .putAllLabels(new HashMap<String, String>())
                .build();
    }
}