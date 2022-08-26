package me.oxstone.googlenmtapplier.utils;

import lombok.Data;

@Data
public class AdvancedTranslationSettings {

    public static final String KO = "ko";
    public static final String EN = "EN";

    private String json;
    private String project;
    private String location;
    private String model;
    private String glossary;

    public AdvancedTranslationSettings(String json, String project, String location, String model, String glossary) {
        this.json = json;
        this.project = project;
        this.location = location;
        this.model = model;
        this.glossary = glossary;
    }
}
