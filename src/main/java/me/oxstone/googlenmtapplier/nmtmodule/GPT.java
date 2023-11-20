package me.oxstone.googlenmtapplier.nmtmodule;

import me.oxstone.googlenmtapplier.nmtsettings.NmtSettings;
import me.oxstone.googlenmtapplier.repository.LanguageRepository;
import org.springframework.http.*;

import javax.management.RuntimeErrorException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

public class GPT extends GoogleV3_WMC implements  NmtModule {

    private LanguageRepository languageRepository;

    public GPT(NmtSettings nmtSettings, LanguageRepository languageRepository) throws IOException {
        super(nmtSettings);
        this.languageRepository = languageRepository;
    }

    @Override
    public Map<String, String> batchTranslateText(Map<String, String> segmentMap) throws IOException, RuntimeErrorException {
        if (!validateJsonKey()) {
            throw new RuntimeException("Json 파일이 유효하지 않습니다.");
        }

        Map<Integer, Map<String, String>> splitMapBySize = splitMapBySize(segmentMap, 1024);
        List<CompletableFuture<Map<String, String>>> futures = new ArrayList<>();
        segmentMap.forEach((key, value) -> {
            CompletableFuture<Map<String, String>> future =
                CompletableFuture.supplyAsync(() -> Map.of(key, translateText(value)));
            futures.add(future);
        });

        Map<String, String> targetMap = new HashMap<>();
        for (CompletableFuture<Map<String, String>> future : futures) {
            Map<String, String> result = null;
            try {
                result = future.get();
            } catch (Exception e) {
                if (!e.getCause().getMessage().contains("RESOURCE_EXHAUSTED")) {
                    throw new RuntimeException(e);
                }
            }
            targetMap.putAll(result);
        }
        return targetMap;
    }

    @Override
    public String translateText(String text) {
        AtomicReference<String> targetLanguageName = new AtomicReference<>();
        languageRepository
            .findByCode(nmtSettings.getTargetLangCode())
            .ifPresent(language -> targetLanguageName.set(language.getName()));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        headers.add("target-language", targetLanguageName.get());
        HttpEntity<String> entity = new HttpEntity<>(text, headers);
        ResponseEntity<String> response = restTemplate.exchange(DEFAULT_URL + "/chatGPT", HttpMethod.POST, entity, String.class);

        return response.getBody();
    }
}
