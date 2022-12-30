package me.oxstone.googlenmtapplier.nmtsettings;

import lombok.Data;

@Data
public class NmtSettingsAdaptor implements NmtSettings {

    @Override
    public String getApiKey() {
        return null;
    }

    @Override
    public void setApiKey(String apiKey) {

    }

    @Override
    public String getJson() {
        return null;
    }

    @Override
    public void setJson(String json) {

    }

    @Override
    public String getProject() {
        return null;
    }

    @Override
    public void setProject(String project) {

    }

    @Override
    public String getLocation() {
        return null;
    }

    @Override
    public void setLocation(String location) {

    }

    @Override
    public String getSourceLangCode() {
        return null;
    }

    @Override
    public void setSourceLangCode(String code) {

    }

    @Override
    public String getTargetLangCode() {
        return null;
    }

    @Override
    public void setTargetLangCode(String code) {

    }

    @Override
    public String getModel() {
        return null;
    }

    @Override
    public void setModel(String model) {

    }

    @Override
    public String getGlossary() {
        return null;
    }

    @Override
    public void setGlossary(String glossary) {

    }

    @Override
    public boolean isApplyModel() {
        return false;
    }

    @Override
    public void setApplyModel(boolean applyModel) {

    }

    @Override
    public boolean isApplyGlossary() {
        return false;
    }

    @Override
    public void setApplyGlossary(boolean applyGlossary) {

    }
}
