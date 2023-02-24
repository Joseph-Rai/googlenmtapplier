package me.oxstone.googlenmtapplier.nmtmodule;

import com.google.cloud.translate.Translate;
import com.google.cloud.translate.TranslateOptions;
import com.google.cloud.translate.Translation;
import me.oxstone.googlenmtapplier.nmtsettings.NmtSettings;
import org.apache.commons.text.StringEscapeUtils;

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
        return asyncTranslateRequest(segmentMap, innerMap -> {
            List<Translation> translations = translateService.translate(new ArrayList<>(innerMap.values()));
            List<String> translatedTexts = translations.stream()
                    .map(translation -> StringEscapeUtils.unescapeHtml4(translation.getTranslatedText()))
                    .collect(Collectors.toList());
            return generateTargetMap(innerMap, translatedTexts);
        });
    }

    @Override
    public String translateText(String text) {
        return translateService.translate(text).getTranslatedText();
    }
}
