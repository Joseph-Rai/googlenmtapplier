package me.oxstone.googlenmtapplier.nmtsettings;

import lombok.Data;

@Data
public class GoogleV3Settings extends NmtSettingsAdaptor {

    private String json;
    private String project;
    private String location;
    private String model;
    private String glossary;
    private String sourceLangCode;
    private String targetLangCode;
    private boolean applyModel;
    private boolean applyGlossary;
    private boolean applyChatGPT;

}
