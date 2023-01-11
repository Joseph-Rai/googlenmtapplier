package me.oxstone.googlenmtapplier.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import lombok.RequiredArgsConstructor;
import me.oxstone.googlenmtapplier.JavaFxApplication;
import me.oxstone.googlenmtapplier.data.Language;
import me.oxstone.googlenmtapplier.nmtmodule.GoogleV2;
import me.oxstone.googlenmtapplier.nmtmodule.GoogleV3;
import me.oxstone.googlenmtapplier.nmtmodule.NmtModule;
import me.oxstone.googlenmtapplier.nmtsettings.GoogleV2Settings;
import me.oxstone.googlenmtapplier.nmtsettings.GoogleV3Settings;
import me.oxstone.googlenmtapplier.nmtsettings.NmtSettings;
import me.oxstone.googlenmtapplier.repository.LanguageRepository;
import net.rgielen.fxweaver.core.FxWeaver;
import net.rgielen.fxweaver.core.FxmlView;
import net.sf.okapi.common.Event;
import net.sf.okapi.common.EventType;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.exceptions.OkapiMergeException;
import net.sf.okapi.common.filterwriter.IFilterWriter;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.RawDocument;
import net.sf.okapi.common.resource.Segment;
import net.sf.okapi.common.skeleton.GenericSkeleton;
import net.sf.okapi.common.skeleton.GenericSkeletonPart;
import net.sf.okapi.filters.xliff.XLIFFFilter;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.dom4j.tree.DefaultElement;
import org.springframework.stereotype.Controller;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.Map.Entry;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@FxmlView("MainFx.fxml")
public class MainFxController implements Initializable {

    private static final String DESKTOP_PATH = System.getProperty("user.home") + "\\Desktop";
    private static final String SOURCE_PATH = "source_path";
    private static final String JSON = "json_path";
    private static final String PROJECT = "project_id";
    private static final String MODEL = "model_id";
    private static final String LOCATION = "location_id";
    private static final String GLOSSARY = "glossary_id";
    private static final String APIKEY = "api_key";
    private static final String APPLY_MODEL = "apply_model";
    private static final String APPLY_GLOSSARY = "apply_glossary";
    private static final String TRANSLATION_FROM_SOURCE = "translation_from_source";
    private static final String TRANSLATION_FROM_TARGET = "translation_from_target";
    private static final String TARGET_FORMAT_TRANSLATION_TEXT_ONLY = "target_format_translation_text_only";
    private static final String TARGET_FORMAT_TARGET_AND_TRANSLATION_TEXT = "target_format_target_and_translation_text";
    private static final String NMT_MODULE = "nmt_module";
    private static final String FILE_FILTER = "file_filter";
    private static final String SAVED_SOURCE_LANGUAGE = "saved_source_language";
    private static final String SAVED_TARGET_LANGUAGE = "saved_target_language";

    private static final Preferences preference = Preferences.userNodeForPackage(MainFxController.class);

    @FXML
    private TextField txtJsonPath;

    @FXML
    private Button btnSearchJson;

    @FXML
    private TextField txtProject;

    @FXML
    private TextField txtModel;

    @FXML
    private TextField txtLocation;

    @FXML
    private TextField txtGlossary;

    @FXML
    private TextField txtApiKey;

    @FXML
    private ComboBox<String> cboSourceLang;

    @FXML
    private ComboBox<String> cboTargetLang;

    @FXML
    private ComboBox<String> cboNmtModule;

    @FXML
    private CheckBox chkGlossary;

    @FXML
    private CheckBox chkModel;

    @FXML
    private RadioButton optFromTarget;

    @FXML
    private RadioButton optFromSource;

    @FXML
    private RadioButton optTranslatedTextOnly;

    @FXML
    private RadioButton optTargetAndTranslatedText;

    @FXML
    private Button btnDetectLanguage;

    @FXML
    private ComboBox<String> cboFileFilter;

    @FXML
    private Button btnOpenFile;

    @FXML
    private ListView<String> lstFiles = new ListView<>();

    private NmtSettings nmtSettings;
    private NmtModule nmtModule;
    private String typedText;
    private long lastTypedTime;
    private XLIFFFilter filter = new XLIFFFilter();

    private String docSourceLanguage;
    private String docTargetLanguage;

    private final LanguageRepository languageRepository;

    private final FxWeaver fxWeaver;

    private enum FLAG {
        SOURCE, TARGET
    }

    private enum MODE {
        SINGLE, MULTI
    }

    /*
     * Window Panel 초기화 이벤트
     */
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // 번역모듈 목록 초기화
        cboNmtModule.getItems().addAll(
                "Google Translation V2",
                "Google Translation V3");
        cboNmtModule.getSelectionModel().select("Google Translation V3");

        cboFileFilter.getItems().addAll(
                "SDL Xliff");
        cboFileFilter.getSelectionModel().select("SDL Xliff");

        // Home 세팅값 불러오기
        cboNmtModule.getSelectionModel().select(preference.get(NMT_MODULE, "Google Translation V3"));
        cboFileFilter.getSelectionModel().select(preference.get(FILE_FILTER, "SDL Xliff"));
        cboSourceLang.getSelectionModel().select(preference.get(SAVED_SOURCE_LANGUAGE, "Korean"));
        cboTargetLang.getSelectionModel().select(preference.get(SAVED_TARGET_LANGUAGE, "English"));
        optFromSource.selectedProperty().set(Boolean.parseBoolean(preference.get(TRANSLATION_FROM_SOURCE, "true")));
        optFromTarget.selectedProperty().set(Boolean.parseBoolean(preference.get(TRANSLATION_FROM_TARGET, "false")));
        optTranslatedTextOnly.selectedProperty()
                .set(Boolean.parseBoolean(preference.get(TARGET_FORMAT_TRANSLATION_TEXT_ONLY, "true")));
        optTargetAndTranslatedText.selectedProperty()
                .set(Boolean.parseBoolean(preference.get(TARGET_FORMAT_TARGET_AND_TRANSLATION_TEXT, "false")));

        // 소스언어, 타겟언어 목록 초기화
        initComboBoxItems(cboSourceLang);
        initComboBoxItems(cboTargetLang);
        cboSourceLang.getSelectionModel().select("Korean");
        cboTargetLang.getSelectionModel().select("English");

        TargetTextFormatOptionDisable();

        // Google Settings Preference 값 불러오기
        txtApiKey.setText(preference.get(APIKEY, "Google Translation V2 API Key"));
        txtJsonPath.setText(preference.get(JSON, "Json Key Path"));
        txtProject.setText(preference.get(PROJECT, "Your Project ID"));
        txtLocation.setText(preference.get(LOCATION, "The Location of Your Project"));
        txtModel.setText(preference.get(MODEL, "Your Model ID"));
        txtGlossary.setText(preference.get(GLOSSARY, "Glossary ID"));
        chkModel.selectedProperty().set(Boolean.parseBoolean(preference.get(APPLY_MODEL, "false")));
        chkGlossary.selectedProperty().set(Boolean.parseBoolean(preference.get(APPLY_GLOSSARY, "false")));

        // BtnDetectLanguage 비활성화
        btnDetectLanguage.setDisable(true);

        // lstFiles 객체에 리스너 추가
        lstFiles.getItems().addListener((ListChangeListener<? super String>) change -> {
            int size = lstFiles.getItems().size();
            btnDetectLanguage.setDisable(size == 0);
            if (size > 0) {
                lstFiles.getSelectionModel().select(size - 1);
            }
        });

        txtModel.textProperty().addListener(change -> {
            chkModel.selectedProperty().set(!txtModel.getText().isEmpty());
        });

        txtGlossary.textProperty().addListener(change -> {
            chkGlossary.selectedProperty().set(!txtGlossary.getText().isEmpty());
        });
    }

    private ObservableList<String> getGlossaryFilteredObservableLanguages() {
        return FXCollections.observableArrayList(
                languageRepository.findAll().stream()
                        .filter(language -> language.getGlossary())
                        .map(lang -> lang.getName())
                        .collect(Collectors.toList()));
    }

    private ObservableList<String> getNameFilteredObservableLanguages(String text) {
        return getAllObservableLanguages()
                .filtered(s -> s.toLowerCase().startsWith(text));
    }

    private ObservableList<String> getAllObservableLanguages() {
        return FXCollections.observableArrayList(
                languageRepository.findAll().stream()
                        .map(lang -> lang.getName())
                        .collect(Collectors.toList()));
    }

    /*
     * 학습형 구글번역(NMT)를 적용합니다.
     */
    @FXML
    void clickBtnApplyNMT(ActionEvent event) throws IOException {

        // 리스트에 파일목록이 없으면 종료
        if (lstFiles.getItems().size() < 1) {
            String title = "Error";
            String header = "Error";
            String msg = "No file selected.";
            showMsgbox(title, header, msg, Alert.AlertType.ERROR);
            return;
        }

        // 선택한 설정 저장
        preference.put(NMT_MODULE, cboNmtModule.getSelectionModel().getSelectedItem());
        preference.put(FILE_FILTER, cboFileFilter.getSelectionModel().getSelectedItem());
        preference.put(SAVED_SOURCE_LANGUAGE, cboSourceLang.getSelectionModel().getSelectedItem());
        preference.put(SAVED_TARGET_LANGUAGE, cboTargetLang.getSelectionModel().getSelectedItem());
        preference.put(TRANSLATION_FROM_SOURCE, String.valueOf(optFromSource.selectedProperty().getValue()));
        preference.put(TRANSLATION_FROM_TARGET, String.valueOf(optFromTarget.selectedProperty().getValue()));
        preference.put(TARGET_FORMAT_TRANSLATION_TEXT_ONLY,
                String.valueOf(optTranslatedTextOnly.selectedProperty().getValue()));
        preference.put(TARGET_FORMAT_TARGET_AND_TRANSLATION_TEXT,
                String.valueOf(optTargetAndTranslatedText.selectedProperty().getValue()));

        // cboMntModule 값에 따라 Settings, MntModule 설정
        switch (cboNmtModule.getValue()) {
            case "Google Translation V2":
                nmtSettings = new GoogleV2Settings();
                prepareSettings();
                nmtModule = new GoogleV2(nmtSettings);
                // 세팅창 조정로직 추가
                break;
            case "Google Translation V3":
                nmtSettings = new GoogleV3Settings();
                prepareSettings();
                nmtModule = new GoogleV3(nmtSettings);
                // 세팅창 조정로직 추가
                break;
        }

        String msg;
        try {
            // 목록에 있는 각 파일들에 대하여 NMT 적용
            List<String> sourcePaths = lstFiles.getItems();
            for (String path : sourcePaths) {
                File sourceFile = new File(path);
                preference.put(SOURCE_PATH, sourceFile.getParent());

                // 문서구조(TextUnits) 추출
                List<Event> allFilterEvents = getAllEventsFromFilter(sourceFile);
                List<ITextUnit> textUnits = getTextUnits(allFilterEvents);

                // 문서구조로부터 소스 세그먼트 및 타겟 세그먼트 Map<ID, 문장> 추출
                Map<String, String> sourceSegmentMap = getSegmentMap(textUnits, FLAG.SOURCE);
                Map<String, String> targetSegmentMap = getSegmentMap(textUnits, FLAG.TARGET);

                // 번역 후 문장구조에 적용
                Map<String, String> batchSegmentMap;
                if (optFromSource.selectedProperty().getValue()) {
                    batchSegmentMap = nmtModule.batchTranslateText(sourceSegmentMap);
                } else {
                    batchSegmentMap = nmtModule.batchTranslateText(targetSegmentMap);
                }
                applyTranslateResultToTextUnits(textUnits, targetSegmentMap, batchSegmentMap);

                // 파일로 저장
                extractTargetFile(allFilterEvents, sourceFile);
            }

            // 성공 메세지 띄우기
            String title = "Succeed";
            String header = "The task completed successfully";
            msg = "Machine translation has been successfully applied.";
            showMsgbox(title, header, msg, Alert.AlertType.INFORMATION);

        } catch (Exception exception) {
            // 실패 메세지 띄우기
            String title = "Fail!";
            String header = "The task failure!!!";
            msg = "An error occurred while applying the machine translation.\n\n" +
                    "Description: " + exception.getCause().getMessage();
            showMsgbox(title, header, msg, Alert.AlertType.ERROR);
            exception.printStackTrace();
        }

    }

    /**
     * 폼에 있는 값을 Settings 객체에 저장
     */
    private void prepareSettings() {
        nmtSettings.setApiKey(txtApiKey.getText());
        nmtSettings.setJson(txtJsonPath.getText());
        nmtSettings.setProject(txtProject.getText());
        nmtSettings.setLocation(txtLocation.getText());
        nmtSettings.setModel(txtModel.getText());
        nmtSettings.setGlossary(txtGlossary.getText());
        languageRepository.findByName(cboSourceLang.getValue()).stream()
                .map(language -> language.getCode())
                .findFirst()
                .ifPresent(code -> nmtSettings.setSourceLangCode(code));
        languageRepository.findByName(cboTargetLang.getValue()).stream()
                .map(language -> language.getCode())
                .findFirst()
                .ifPresent(code -> nmtSettings.setTargetLangCode(code));
        nmtSettings.setApplyModel(chkModel.selectedProperty().getValue());
        nmtSettings.setApplyGlossary(chkGlossary.selectedProperty().getValue());
    }

    /**
     * 번역결과를 TextUnits에 적용합니다.
     * 
     * @param textUnits        : sdlxliff 구조체
     * @param targetSegmentMap : 번역결과를 TextUnis에 적용시키기 위해 필요한 정보 추출용
     * @param batchSegmentMap  : 번역결과 Map
     */
    private void applyTranslateResultToTextUnits(List<ITextUnit> textUnits, Map<String, String> targetSegmentMap,
            Map<String, String> batchSegmentMap) {
        replaceToTranslatedText(targetSegmentMap, batchSegmentMap);
        applyTranslatedTextToTextUnit(textUnits, targetSegmentMap);
    }

    /*
     * 타겟 세그먼트의 문장들을 NMT 결과로 교체합니다.
     */
    private void replaceToTranslatedText(Map<String, String> targetSegmentMap, Map<String, String> batchSegmentMap) {
        for (Entry<String, String> entry : targetSegmentMap.entrySet()) {
            String key = entry.getKey();
            if ((!optTranslatedTextOnly.isDisabled()) && optTranslatedTextOnly.selectedProperty().getValue()) {
                targetSegmentMap.put(key, batchSegmentMap.get(key));
            } else {
                targetSegmentMap.put(key, targetSegmentMap.get(key) + "\n" + batchSegmentMap.get(key));
            }
        }
    }

    /*
     * Google Translation V3 API에서 받아온 문장 리스트를 타겟 세그먼트에 적용 시킵니다.
     */
    private void applyTranslatedTextToTextUnit(List<ITextUnit> textUnits, Map<String, String> targetSegmentMap) {
        for (ITextUnit tu : textUnits) {
            LocaleId targetLocaleId = LocaleId.fromString(docTargetLanguage);

            List<Segment> segments = tu.getTargetSegments(targetLocaleId).asList();
            for (Segment segment : segments) {
                String id = segment.getId();
                if (id.compareTo("0") > 0 && !isLockedSegment(tu, id)) {
                    if (!(optFromSource.selectedProperty().getValue() && !segment.text.getText().trim().isEmpty())) {
                        String targetText = targetSegmentMap.get(id);
                        segment.text.setCodedText(targetText); // 타겟 텍스트 삽입
                        if (optFromSource.selectedProperty().getValue()) {
                            changeSegmentStatus(tu, id); // 세그먼트 상태변경(Draft + NMT)
                        }
                    }
                }
            }
        }
    }

    /*
     * Event 정보에서 Segment 정보를 가져옵니다. Segment 정보는 TestUnit 내부에 있습니다.
     */
    private List<ITextUnit> getTextUnits(List<Event> allFilterEvents) {
        List<ITextUnit> textUnits = new ArrayList<>();
        for (Event e : allFilterEvents) {
            if (e.getEventType() == EventType.TEXT_UNIT) {
                textUnits.add(e.getTextUnit());
            }
        }
        return textUnits;
    }

    /*
     * Filter에서 모든 Event 목록을 가져옵니다. 각 Event를 통해 Segment 정보를 추출할 수 있습니다.
     */
    private List<Event> getAllEventsFromFilter(File sourceFile) throws DocumentException {
        List<Event> events = new ArrayList<>();
        detectLanguagePairFromFile(sourceFile);
        RawDocument document = new RawDocument(sourceFile.toURI(), "UTF-8-BOM",
                LocaleId.fromString(docSourceLanguage), LocaleId.fromString(docTargetLanguage));
        filter.open(document);
        while (filter.hasNext()) {
            events.add(filter.next());
        }
        return events;
    }

    private void detectLanguagePairFromFile(File sourceFile) throws DocumentException {
        SAXReader saxReader = new SAXReader();
        Document xmlDoc = saxReader.read(sourceFile);
        List<Node> contents = xmlDoc.getRootElement().content();
        for (Node content : contents) {
            if (content.getName() != null && content.getName().equals("file")) {
                List<Attribute> attributes = ((DefaultElement) content).attributes();
                for (Attribute attribute : attributes) {
                    if (attribute.getName() != null && attribute.getName().equals("source-language")) {
                        docSourceLanguage = attribute.getValue();
                    }
                    if (attribute.getName() != null && attribute.getName().equals("target-language")) {
                        docTargetLanguage = attribute.getValue();
                    }
                }
            }
        }
    }

    /*
     * Filter에 적용된 변경사항을 원본 문서에 덮어 씁니다.
     */
    private void extractTargetFile(List<Event> allFilterEvents, File sourceFile) {
        try {
            IFilterWriter filterWriter = filter.createFilterWriter();
            filterWriter.setOutput(sourceFile.getAbsolutePath());
            for (Event e : allFilterEvents) {
                filterWriter.handleEvent(e);
            }
        } catch (Exception ex) {
            filter.close();
            throw new OkapiMergeException("Error merging from original file", ex);
        } finally {
            if (filter != null)
                filter.close();
        }
    }

    /*
     * 세그먼트 상태를 변경합니다. | 미번역 -> 초안, NMT
     */
    private void changeSegmentStatus(ITextUnit tu, String segmentId) {
        GenericSkeleton genericSkeleton = (GenericSkeleton) tu.getSkeleton();
        for (GenericSkeletonPart gsp : genericSkeleton.getParts()) {
            StringBuilder data = gsp.getData();

            // 세그먼트 상태변경 -> 초안, NMT
            if (data.indexOf("id=\"" + segmentId + "\"") > 0 &&
                    data.indexOf("conf=") == -1 && data.indexOf("origin=") == -1) {
                int start = data.indexOf("id=\"" + segmentId + "\"") + ("id=\"" + segmentId + "\"").length();
                int end = start;
                data.replace(start, end, " conf=\"Draft\" origin=\"nmt\"");
            }
        }
    }

    /*
     * 잠긴 세그먼트인지 판별합니다.
     */
    private boolean isLockedSegment(ITextUnit tu, String id) {
        GenericSkeleton genericSkeleton = (GenericSkeleton) tu.getSkeleton();
        for (GenericSkeletonPart gsp : genericSkeleton.getParts()) {
            StringBuilder data = gsp.getData();

            // 잠금 세그먼트 식별
            if (data.indexOf("locked=\"true\"") >= 0 &&
                    data.indexOf("id=\"" + id + "\"") >= 0) {
                return true;
            }
        }
        return false;
    }

    /*
     * TextUnit 객체에서 각 Segment 목록을 추출합니다.
     */
    private Map<String, String> getSegmentMap(List<ITextUnit> textUnits, FLAG flag) {
        Map<String, String> segmentMap = new HashMap<>();
        List<Segment> segments = null;
        for (ITextUnit tu : textUnits) {
            if (flag == FLAG.SOURCE) {
                segments = tu.getSourceSegments().asList();
            } else {
                segments = tu.getTargetSegments(LocaleId.fromString(docTargetLanguage)).asList();
            }
            for (int i = 0; i < segments.size(); i++) {
                Segment segment = segments.get(i);
                if (segment.getId().compareTo("0") > 0) {
                    if (!(flag == FLAG.SOURCE && segment.text.getText().isEmpty()))
                        segmentMap.put(segment.getId(), segment.text.getText());
                }
            }
        }
        return segmentMap;
    }

    /*
     * 파일추가 버튼 Event
     */
    @FXML
    void clickBtnAdd(ActionEvent event) {
        String defaultPath = preference.get(SOURCE_PATH, DESKTOP_PATH);
        List<File> files = getFileLists(defaultPath);
        addListItems(lstFiles, files);
    }

    /*
     * 파일삭제 버튼 Event
     */
    @FXML
    void clickBtnDelete(ActionEvent event) {
        MultipleSelectionModel<String> selectionModel = lstFiles.getSelectionModel();
        ObservableList<String> selectedItems = selectionModel.getSelectedItems();
        for (String s : selectedItems) {
            lstFiles.getItems().remove(s);
        }
    }

    /*
     * File ListView에 선택한 파일목록을 추가합니다.
     */
    private void addListItems(ListView<String> lstFiles, List<File> files) {
        for (File file : files) {
            String absolutePath = file.getAbsolutePath();
            if (!lstFiles.getItems().contains(absolutePath)) {
                lstFiles.getItems().add(absolutePath);
            }
        }
    }

    /*
     * JSON 키를 불러옵니다.
     */
    @FXML
    void clickBtnSearchJson(ActionEvent event) {
        String defaultPath = preference.get(JSON, DESKTOP_PATH);
        List<File> files = getFileLists(defaultPath);
        Platform.runLater(() -> {
            txtJsonPath.setText(files.get(0).getAbsolutePath());
        });
    }

    /*
     * File Open Multiple Dialog를 통해 다중 파일목록을 가져옵니다.
     */
    private List<File> getFileLists(String defaultPath) {
        return showFileChooser(defaultPath);
    }

    /*
     * 기본경로 설정 후 File Open Multiple Dialog를 띄워줍니다.
     */
    private List<File> showFileChooser(String defaultPath) {
        String title = "Select the file...";
        File defaultDir = new File(defaultPath);
        if (!defaultDir.isDirectory()) {
            defaultPath = DESKTOP_PATH;
        }
        return showMultiFileChooser(title, btnSearchJson.getScene().getWindow(), defaultPath);
    }

    private List<File> showMultiFileChooser(String title, Window ownerWindow, String path) {
        FileChooser chooser = new FileChooser();
        File dir = new File(path);
        chooser.setTitle(title);
        setDefaultDirectory(chooser, dir);
        return chooser.showOpenMultipleDialog(ownerWindow);
    }

    /*
     * File Open Dialog를 위한 기본 경로를 설정합니다.
     */
    private void setDefaultDirectory(FileChooser chooser, File dir) {
        if (dir.isDirectory()) {
            chooser.setInitialDirectory(dir);
        } else {
            chooser.setInitialDirectory(dir.getParentFile());
        }
    }

    /*
     * Google Translation 설정값을 Preferences에 저장합니다.
     */
    @FXML
    void clickBtnSaveSettings(ActionEvent event) {
        preference.put(JSON, txtJsonPath.getText());
        preference.put(PROJECT, txtProject.getText());
        preference.put(MODEL, txtModel.getText());
        preference.put(LOCATION, txtLocation.getText());
        preference.put(GLOSSARY, txtGlossary.getText());
        preference.put(APIKEY, txtApiKey.getText());
        preference.put(APPLY_MODEL, String.valueOf(chkModel.selectedProperty().getValue()));
        preference.put(APPLY_GLOSSARY, String.valueOf(chkGlossary.selectedProperty().getValue()));

        String title = JavaFxApplication.PROGRAM_VER;
        String header = "" +
                "The settings have been saved successfully.";
        showMsgbox(title, header, "", Alert.AlertType.INFORMATION);
    }

    /*
     * 종료버튼 Event
     */
    @FXML
    void clickMenuClose(ActionEvent event) {
        Platform.exit();
    }

    /*
     * 메뉴버튼 Event
     */
    @FXML
    void clickMenuAbout(ActionEvent event) {
        String title = JavaFxApplication.PROGRAM_VER;
        String header = "" +
                "Program Author: " + JavaFxApplication.PROGRAM_AUTHOR +
                "\n\nCopy Right: " + JavaFxApplication.PROGRAM_COPYRIGHT +
                "\n\nLast Modified Date: " + JavaFxApplication.PROGRAM_LAST_MODIFIED;
        String msg = "" +
                "This program applies the Google Translate V3 results to the target.\n" +
                "Segments that already have target text are skipped.\n" +
                "Only sdlxliff format files are supported.";
        showMsgbox(title, header, msg, Alert.AlertType.INFORMATION);
    }

    /*
     * 메세지 팝업 도구
     */
    private void showMsgbox(String title, String header, String content, Alert.AlertType alertType) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);

        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(
                getClass().getResource("Stylesheet.css").toExternalForm());
        dialogPane.getStyleClass().add("myDialog");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.get() == ButtonType.OK) {
            alert.close();
        } else {
            // ... user chose CANCEL or closed the dialog
        }
    }

    void openFileEditor(String filePath) throws DocumentException {
        File sourceFile = new File(filePath);

        // 문서구조(TextUnits) 추출
        List<Event> allFilterEvents = getAllEventsFromFilter(sourceFile);
        List<ITextUnit> textUnits = getTextUnits(allFilterEvents);

        Map<String, String> sourceSegments = getSegmentMap(textUnits, FLAG.SOURCE);
        Map<String, String> targetSegments = getSegmentMap(textUnits, FLAG.TARGET);

        Parent root = fxWeaver.loadView(FileEditorController.class);
        javafx.scene.Node scrollPane = ((BorderPane) root).getCenter();
        javafx.scene.Node pageVBox = ((ScrollPane) scrollPane).getContent();
        ObservableList<javafx.scene.Node> children = ((VBox) pageVBox).getChildren();

        Comparator<String> keyComparator = ((key1, key2) -> {
            String tmpKey1;
            String tmpKey2;

            if (key1.contains("_")) {
                tmpKey1 = key1.substring(0, key1.indexOf("_"));
            } else {
                tmpKey1 = key1;
            }

            if (key2.contains("_")) {
                tmpKey2 = key2.substring(0, key2.indexOf("_"));
            } else {
                tmpKey2 = key2;
            }

            if (tmpKey1.length() < tmpKey2.length()) {
                return -1;
            } else if (tmpKey1.length() == tmpKey2.length()) {
                return Integer.compare(Integer.parseInt(tmpKey1), Integer.parseInt(tmpKey2));
            } else {
                return 1;
            }
        });

        List<String> sortedKeyList = sourceSegments.keySet().stream().sorted(keyComparator)
                .collect(Collectors.toList());
        for (String key : sortedKeyList) {
            children.add(createNewSegment(key, sourceSegments.get(key), targetSegments.get(key)));
        }

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("Stylesheet.css").toExternalForm());
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.setTitle("File Editor");
        stage.setResizable(true);
        stage.show();
    }

    private javafx.scene.Node createNewSegment(String key, String source, String target) {
        HBox segmentBox = new HBox();
        segmentBox.setAlignment(Pos.CENTER);
        HBox.setMargin(segmentBox, new Insets(10));
        segmentBox.setMinWidth(Region.USE_COMPUTED_SIZE);
        segmentBox.setMinHeight(Region.USE_COMPUTED_SIZE);
        segmentBox.setPrefWidth(Region.USE_COMPUTED_SIZE);
        segmentBox.prefHeight(Region.USE_COMPUTED_SIZE);
        segmentBox.setMaxWidth(Region.USE_COMPUTED_SIZE);
        segmentBox.setMaxHeight(Region.USE_COMPUTED_SIZE);
        segmentBox.setSpacing(10);

        Label lblSource = createNewLabel("lblSource" + key);
        lblSource.setText(key.replace("_x0020_", "|"));

        TextArea txtSource = createNewTextArea("txtSource" + key);
        txtSource.textProperty().addListener((observable, oldValue, newValue) -> {
            arrangeTextAreaHeight(txtSource, newValue);
        });
        txtSource.setText(source);
        arrangeTextAreaHeight(txtSource, source);
        txtSource.setEditable(false);

        Label lblTarget = createNewLabel("lblTarget" + key);
        lblTarget.setText(key.replace("_x0020_", "|"));

        TextArea txtTarget = createNewTextArea("txtTarget" + key);
        txtTarget.textProperty().addListener((observable, oldValue, newValue) -> {
            arrangeTextAreaHeight(txtTarget, newValue);
        });
        txtTarget.setText(target);
        arrangeTextAreaHeight(txtTarget, target);
        // 저장기능 생기면 삭제
        // txtTarget.setEditable(false);

        segmentBox.getChildren().addAll(lblSource, txtSource, lblTarget, txtTarget);

        return segmentBox;
    }

    void arrangeTextAreaHeight(TextArea txtArea, String text) {
        Text textObject = new Text();
        textObject.setText(text);
        textObject.setFont(txtArea.getFont());
        textObject.setWrappingWidth(txtArea.widthProperty().getValue() * 0.97);
        txtArea.setPrefHeight(textObject.getLayoutBounds().getHeight() + txtArea.getFont().getSize());
    }

    private TextArea createNewTextArea(String id) {
        TextArea textArea = new TextArea();
        textArea.setFont(new Font(18));
        textArea.setWrapText(true);
        textArea.setMinWidth(Region.USE_COMPUTED_SIZE);
        textArea.setMaxHeight(Region.USE_COMPUTED_SIZE);
        textArea.setPrefHeight(textArea.getFont().getSize());
        textArea.setPrefWidth(550);
        textArea.setMaxWidth(Region.USE_COMPUTED_SIZE);
        textArea.setMaxHeight(Region.USE_COMPUTED_SIZE);
        textArea.getStyleClass().setAll("btn");
        textArea.setId(id);

        return textArea;
    }

    private Label createNewLabel(String key) {
        Label label = new Label();
        label.setFont(new Font(18));
        label.setMinWidth(Region.USE_COMPUTED_SIZE);
        label.setMinHeight(Region.USE_COMPUTED_SIZE);
        label.setPrefWidth(70);
        label.setPrefHeight(Region.USE_COMPUTED_SIZE);
        label.setMaxWidth(Region.USE_COMPUTED_SIZE);
        label.setMaxHeight(Region.USE_COMPUTED_SIZE);
        label.getStyleClass().setAll("btn");
        label.setId(key);
        return label;
    }

    @FXML
    void clickLstFiles(MouseEvent event) throws DocumentException {
        if (event.getClickCount() > 1) {
            openFileEditor(lstFiles.getSelectionModel().getSelectedItem());
        }
    }

    /*
     * 드래그앤 드랍 이벤트
     * 드래그한 파일 목록을 File ListView에 추가합니다.
     */
    @FXML
    void dragDroppedOnListFiles(DragEvent event) {
        Dragboard dragboard = event.getDragboard();
        addListItems(lstFiles, dragboard.getFiles());

        event.setDropCompleted(true);
    }

    /*
     * 드래그한 파일을 ListView에 추가하기 위해 TransferMode를 LINK로 설정합니다.
     */
    @FXML
    void dragOverOnListFiles(DragEvent event) {
        if (event.getDragboard().hasFiles()) {
            event.acceptTransferModes(TransferMode.LINK);
        }
    }

    /*
     * Drag한 상태로 ListView 영역에 들어오면 배경색 변경
     * #FFFACD: 연한 노란색
     */
    @FXML
    void dragEnteredOnListFiles(DragEvent event) {
        lstFiles.setStyle("-fx-background-color : #FFFACD");
    }

    /*
     * Drag한 상태로 ListView 영역을 벗어나면 배경색 원상복구
     * #FFFFFF: 흰색
     */
    @FXML
    void dragExitedOnListFiles(DragEvent event) {
        lstFiles.setStyle("-fx-background-color : #FFFFFF");
    }

    /**
     * cboSourceLang 프레스 이벤트
     */
    @FXML
    void pressCboSourceLang(MouseEvent event) {
        cboSourceLang.getItems().clear();
        cboSourceLang.setItems(getAllObservableLanguages());
    }

    /**
     * cboTargetLang 프레스 이벤트
     */
    @FXML
    void pressCboTargetLang(MouseEvent event) {
        cboTargetLang.getItems().clear();
        cboTargetLang.setItems(getAllObservableLanguages());
    }

    /**
     * 문서에 세팅된 Source Language와 Target Language를 찾아
     * 콤보박스가 해당 언어를 선택하게 합니다.
     */
    @FXML
    void clickBtnDetectLanguage(ActionEvent event) throws DocumentException {
        if (!(lstFiles.getItems().size() > 0) || lstFiles.getSelectionModel().getSelectedItem() == null) {
            String title = "Error";
            String header = "Error";
            String msg = "No file selected.";
            showMsgbox(title, header, msg, Alert.AlertType.ERROR);
            return;
        }

        detectLanguagePairFromFile(new File(lstFiles.getSelectionModel().getSelectedItem()));
        String tmp;
        if (!docSourceLanguage.contains("zh")) {
            tmp = docSourceLanguage.substring(0, docSourceLanguage.indexOf("-"));
        } else {
            tmp = docSourceLanguage;
        }
        languageRepository.findByCode(tmp)
                .map(Language::getName)
                .ifPresent(s -> cboSourceLang.getSelectionModel().select(s));

        if (!docTargetLanguage.contains("zh")) {
            tmp = docTargetLanguage.substring(0, docTargetLanguage.indexOf("-"));
        } else {
            tmp = docTargetLanguage;
        }
        languageRepository.findByCode(tmp)
                .map(Language::getName)
                .ifPresent(s -> cboTargetLang.getSelectionModel().select(s));
    }

    @FXML
    void changeCboNmtModule(ActionEvent event) {
        initComboBoxItems(cboSourceLang);
        initComboBoxItems(cboTargetLang);
    }

    /**
     * cboSourceLang으로 넘어온 KeyEvent를 처리합니다.
     */
    @FXML
    void typeCboSourceLang(KeyEvent event) {
        comboBoxKeyEventHandler(cboSourceLang, event);
    }

    /**
     * cboTargetLang으로 넘어온 KeyEvent를 처리합니다.
     */
    @FXML
    void typeCboTargetLang(KeyEvent event) {
        comboBoxKeyEventHandler(cboTargetLang, event);
    }

    /**
     * ComboBox를 넘어온 KeyEvent를 처리합니다.
     * 
     * @param comboBox : 이벤트 대상 ComboBox
     * @param event    : KeyEvent
     */
    void comboBoxKeyEventHandler(ComboBox<String> comboBox, KeyEvent event) {
        if (System.currentTimeMillis() - lastTypedTime > 1000) {
            typedText = "";
            initComboBoxItems(comboBox);
        }
        typedText += event.getCharacter().toLowerCase();
        comboBox.getItems().removeIf(s -> !s.toLowerCase().contains(typedText));
        if (!(comboBox.getItems().size() > 0)) {
            typedText = typedText.substring(0, typedText.length() - 1);
            initComboBoxItems(comboBox);
            comboBox.getItems().removeIf(s -> !s.toLowerCase().contains(typedText));
        }
        lastTypedTime = System.currentTimeMillis();
    }

    void initComboBoxItems(ComboBox<String> comboBox) {
        String tmpLang = comboBox.getValue();

        comboBox.getItems().clear();

        switch (cboNmtModule.getValue()) {
            case "Google Translation V2":
                comboBox.setItems(getAllObservableLanguages());
                break;
            case "Google Translation V3":
                comboBox.setItems(getGlossaryFilteredObservableLanguages());
                break;
        }

        if (tmpLang != null && comboBox.getItems().contains(tmpLang)) {
            comboBox.getSelectionModel().select(tmpLang);
        } else {
            comboBox.getSelectionModel().clearSelection();
        }
    }

    @FXML
    void changeOptFromTarget(ActionEvent event) {
        TargetTextFormatOptionDisable();
    }

    @FXML
    void changeOptFromSource(ActionEvent event) {
        TargetTextFormatOptionDisable();
    }

    void TargetTextFormatOptionDisable() {
        if (optFromTarget.selectedProperty().getValue()) {
            optTranslatedTextOnly.setDisable(false);
            optTargetAndTranslatedText.setDisable(false);
        } else {
            optTranslatedTextOnly.setDisable(true);
            optTargetAndTranslatedText.setDisable(true);
        }
    }

    @FXML
    void pressLblSwitchLanguage(MouseEvent event) {
        String sourceLang = cboSourceLang.getSelectionModel().getSelectedItem();
        String targetLang = cboTargetLang.getSelectionModel().getSelectedItem();
        cboSourceLang.getSelectionModel().select(targetLang);
        cboTargetLang.getSelectionModel().select(sourceLang);
    }

    @FXML
    void clickBtnOpenFile(ActionEvent event) throws DocumentException {
        openFileEditor(lstFiles.getSelectionModel().getSelectedItem());
    }
}
