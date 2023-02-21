package me.oxstone.googlenmtapplier.nmtmodule;

import lombok.RequiredArgsConstructor;
import me.oxstone.googlenmtapplier.nmtsettings.NmtSettings;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public abstract class GoogleModule implements NmtModule {

    protected final NmtSettings nmtSettings;

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
}
