package me.oxstone.googlenmtapplier.nmtmodule;

import com.google.cloud.translate.Translate;
import com.google.cloud.translate.TranslateOptions;
import com.google.cloud.translate.Translation;
import com.google.cloud.translate.v3.TranslateTextRequest;
import me.oxstone.googlenmtapplier.nmtsettings.NmtSettings;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class GoogleV2 extends GoogleModule {
    private Translate translateService;

    public GoogleV2(NmtSettings nmtSettings) throws IOException {
        super(nmtSettings);
        translateService = getDefaultService();
    }

    private Translate getDefaultService() {
        return TranslateOptions.newBuilder()
                .setApiKey(nmtSettings.getApiKey())
                .setTargetLanguage(nmtSettings.getTargetLangCode())
                .build().getService();
    }

    @Override
    public Map<String, String> batchTranslateText(Map<String, String> segmentMap) {
        Map<Integer, Map<String, String>> splitMapBySize = splitMapBySize(segmentMap, 1024);
        List<CompletableFuture<List<String>>> futures = new ArrayList<>();
        for (Map<String, String> innerMap : splitMapBySize.values()) {
            CompletableFuture<List<String>> future = getTranslatedTextFuture(innerMap, translateService);
            futures.add(future);
        }

        List<String> translatedTexts = new ArrayList<>();
        for (CompletableFuture<List<String>> future : futures) {
            List<String> result = null;
            try {
                result = future.get();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            translatedTexts.addAll(result);
        }

        return generateTargetMap(segmentMap, translatedTexts);
    }

    private CompletableFuture<List<String>> getTranslatedTextFuture(Map<String, String> innerMap, Translate translateService) {
        return CompletableFuture.supplyAsync(() -> {
            return translateService.translate(new ArrayList<>(innerMap.values())).stream()
                    .map(Translation::getTranslatedText)
                    .collect(Collectors.toList());
        });
    }

    @Override
    public String translateText(String text) {
        return translateService.translate(text).getTranslatedText();
    }
}
