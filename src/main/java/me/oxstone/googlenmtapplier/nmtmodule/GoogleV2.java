package me.oxstone.googlenmtapplier.nmtmodule;

import com.google.cloud.translate.Translate;
import com.google.cloud.translate.TranslateOptions;
import com.google.cloud.translate.Translation;
import me.oxstone.googlenmtapplier.nmtsettings.NmtSettings;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
        List<Translation> translationList = translateService.translate(new ArrayList<>(segmentMap.values()));
        List<String> translatedTexts = translationList.stream()
                .map(Translation::getTranslatedText)
                .collect(Collectors.toList());
        return generateTargetMap(segmentMap, translatedTexts);
    }

    @Override
    public String translateText(String text) {
        return translateService.translate(text).getTranslatedText();
    }
}
