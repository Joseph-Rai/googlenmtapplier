package me.oxstone.googlenmtapplier.nmtsettings;

public interface NmtSettings {
    String getApiKey();
    void setApiKey(String apiKey);
    String getJson();
    void setJson(String json);
    String getProject();
    void setProject(String project);
    String getLocation();
    void setLocation(String location);
    String getSourceLangCode();
    void setSourceLangCode(String code);
    String getTargetLangCode();
    void setTargetLangCode(String code);
    String getModel();
    void setModel(String model);
    String getGlossary();
    void setGlossary(String glossary);
    boolean isApplyModel();
    void setApplyModel(boolean applyModel);
    boolean isApplyGlossary();
    void setApplyGlossary(boolean applyGlossary);
    boolean isApplyChatGPT();
    void setApplyChatGPT(boolean applyChatGPT);
}
