package me.oxstone.googlenmtapplier.nmtmodule;

import lombok.RequiredArgsConstructor;
import me.oxstone.googlenmtapplier.nmtsettings.NmtSettings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor
public abstract class GoogleModule implements NmtModule {

    protected final NmtSettings nmtSettings;
    protected interface AsyncTranslateRequester<T> {
        Map<String, String> request(Map<String, String> innerMap);
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

    protected Map<Integer, Map<String, String>> splitMapBySize(Map<String, String> segmentMap, int sizeLimit) {
        Map<Integer, Map<String, String>> splitedMap = new HashMap<>();
        int stringLength = 2; // 리스트 양쪽 대괄호
        int mapId = 0;
        for (Map.Entry<String, String> entry : segmentMap.entrySet()) {
            stringLength += entry.getValue().length() + 2; // 컨텐츠 양쪽 따옴표
            if (stringLength >= sizeLimit) {
                // 리스트 양쪽 대괄호 + 컨텐츠 양쪽 따옴표
                stringLength = entry.getValue().length() + 2 + 2;
                mapId++;
            }
            if (!(splitedMap.size() > mapId)) {
                splitedMap.put(mapId, new HashMap<>());
            }
            splitedMap.get(mapId).put(entry.getKey(), entry.getValue());
        }
        return splitedMap;
    }

    protected <T> CompletableFuture<Map<String, String>> getRequestFuture(
            Map<String, String> map, AsyncTranslateRequester requester) {
        return CompletableFuture.supplyAsync(() -> {
            return requester.request(map);
        });
    }

    public Map<String, String> asyncTranslateRequest(Map<String, String> segmentMap, AsyncTranslateRequester requester) {
        Map<Integer, Map<String, String>> splitMapBySize = splitMapBySize(segmentMap, 1024);
        List<CompletableFuture<Map<String, String>>> futures = new ArrayList<>();
        for (Map<String, String> innerMap : splitMapBySize.values()) {
            CompletableFuture<Map<String, String>> future = getRequestFuture(innerMap, requester);
            futures.add(future);
        }

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
//        return generateTargetMap(segmentMap, translatedTexts);
        return targetMap;
    }
}