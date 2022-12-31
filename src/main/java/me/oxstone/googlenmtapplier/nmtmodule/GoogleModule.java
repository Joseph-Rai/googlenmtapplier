package me.oxstone.googlenmtapplier.nmtmodule;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.Credentials;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.translate.v3.TranslateTextResponse;
import com.google.cloud.translate.v3.TranslationServiceClient;
import com.google.cloud.translate.v3.TranslationServiceSettings;
import com.google.common.collect.Lists;
import me.oxstone.googlenmtapplier.nmtsettings.NmtSettings;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GoogleModule implements NmtModule {

    protected final NmtSettings nmtSettings;

    public GoogleModule(NmtSettings nmtSettings) throws IOException {
        this.nmtSettings = nmtSettings;
    }

    /*
     * Segment Id + Target Text 조합의 Map 형성
     * Segment Id는 추후 Target Segment 정보를 수정할 때 식별자로 사용
     */
    protected Map<String, String> generateTargetMap(Map<String, String> segmentMap, List<String> translatedTexts) {
        Map<String, String> result = new HashMap<>();
        int i = 0;
        for (Map.Entry<String, String> entry : segmentMap.entrySet()) {
            result.put(entry.getKey(), translatedTexts.get(i++));
        }
        return result;
    }

    @Override
    public Map<String, String> batchTranslateText(Map<String, String> segmentMap) {
        return null;
    }

    @Override
    public String translateText(String text) {
        return null;
    }
}