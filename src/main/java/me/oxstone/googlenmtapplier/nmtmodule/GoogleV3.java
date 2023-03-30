package me.oxstone.googlenmtapplier.nmtmodule;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.translate.v3.*;
import com.google.common.collect.Lists;
import com.google.protobuf.InvalidProtocolBufferException;
import me.oxstone.googlenmtapplier.nmtsettings.NmtSettings;

import javax.management.RuntimeErrorException;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GoogleV3 extends GoogleModule {

    private TranslationServiceClient client = null;

    public GoogleV3(NmtSettings nmtSettings) throws IOException {
        super(nmtSettings);
        if (!(nmtSettings.getJson() == null)) {
            this.client = getDefaultClient();
        }
    }

    /*
     * Translation Service Client 생성
     */
    private TranslationServiceClient getClient(TranslationServiceSettings translationServiceSettings) throws IOException {
        return TranslationServiceClient.create(translationServiceSettings);
    }

    /*
     *  Translation Service Client 기본 값으로 빌드
     */
    private TranslationServiceClient getDefaultClient() throws IOException {
        GoogleCredentials myCredentials = GoogleCredentials.fromStream(new FileInputStream(nmtSettings.getJson()))
                .createScoped(Lists.newArrayList("https://www.googleapis.com/auth/cloud-translation"));
        TranslationServiceSettings defaultSettings =
                TranslationServiceSettings.newBuilder()
                        .setCredentialsProvider(FixedCredentialsProvider.create(myCredentials))
                        .build();
        return getClient(defaultSettings);
    }

    /*
     * TranslateTextResponse에서 Target Text만 추출
     */
    protected String extractTextOnly(TranslateTextResponse translateText) {
        return translateText.getGlossaryTranslationsList()
                .get(0).getTranslatedText();
    }

    /*
     *  TranslateTextResponse에서 Target Text List 추출
     */
    protected List<String> extractTextList(TranslateTextResponse translateText) {
        if (nmtSettings.isApplyGlossary()) {
            return translateText.getGlossaryTranslationsList().stream()
                    .map(t -> t.getTranslatedText())
                    .collect(Collectors.toList());
        } else {
            return translateText.getTranslationsList().stream()
                    .map(t -> t.getTranslatedText())
                    .collect(Collectors.toList());
        }
    }

    /*
     * Google Translation V3 API 요청을 위한 Request 기본 값 설정
     */
    public TranslateTextRequest getDefaultRequest() {
        String strParent = LocationName.of(nmtSettings.getProject(), nmtSettings.getLocation()).toString();
        return TranslateTextRequest.newBuilder()
                .setMimeType("text/plain")
                .setSourceLanguageCode(nmtSettings.getSourceLangCode())
                .setTargetLanguageCode(nmtSettings.getTargetLangCode())
                .setParent(strParent)
                .putAllLabels(new HashMap<String, String>())
                .build();
    }

    /*
     *  단일문장 번역
     */
    public String translateText(String text) throws InvalidProtocolBufferException {
        TranslateTextRequest request = getDefaultRequest().toBuilder()
                .addContents(text).build();
        return extractTextOnly(client.translateText(request));
    }

    /*
     * List에 삽입된 원본문장 일괄 번역요청
     */
    public Map<String, String> batchTranslateText(Map<String, String> segmentMap) throws IOException, RuntimeErrorException {
        TranslateTextRequest request = applySettingsToTranslateTextRequest();

        // 번역 API 요청
        TranslateTextRequest finalRequest = request;
        return asyncTranslateRequest(segmentMap, innerMap -> {
            TranslateTextRequest req = finalRequest.toBuilder()
                    .addAllContents(innerMap.values())
                    .build();
            List<String> translatedTexts = extractTextList(client.translateText(req));
            return generateTargetMap(innerMap, translatedTexts);
        });
    }

    protected TranslateTextRequest applySettingsToTranslateTextRequest() {
        TranslateTextRequest request = getDefaultRequest();
        String strParent = request.getParent();

        // 모델 설정
        if (nmtSettings.isApplyModel()) {
            request = request.toBuilder()
                    .setModel(strParent + "/models/" + nmtSettings.getModel()).build();
        }

        // 글로서리 설정
        if (nmtSettings.isApplyGlossary()) {
            request = request.toBuilder()
                    .setGlossaryConfig(
                            TranslateTextGlossaryConfig.newBuilder()
                                    .setGlossary(strParent + "/glossaries/" + nmtSettings.getGlossary())
                                    .setIgnoreCase(true)
                                    .build()
                    )
                    .build();
        }
        return request;
    }
}