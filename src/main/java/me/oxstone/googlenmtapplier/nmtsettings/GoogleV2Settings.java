package me.oxstone.googlenmtapplier.nmtsettings;

import lombok.Data;

@Data
public class GoogleV2Settings extends NmtSettingsAdaptor {
    private String apiKey;
    private String sourceLangCode;
    private String targetLangCode;
}
