package me.oxstone.googlenmtapplier.nmtmodule;

import java.util.Map;

public interface NmtModule {
    Map<String, String> batchTranslateText(Map<String, String> segmentMap);
    String translateText(String text);
}
