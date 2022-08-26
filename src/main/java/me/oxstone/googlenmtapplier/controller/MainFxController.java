package me.oxstone.googlenmtapplier.controller;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import me.oxstone.googlenmtapplier.JavaFxApplication;
import me.oxstone.googlenmtapplier.utils.AdvancedTranslate;
import me.oxstone.googlenmtapplier.utils.AdvancedTranslationSettings;
import net.rgielen.fxweaver.core.FxmlView;
import net.sf.okapi.common.Event;
import net.sf.okapi.common.EventType;
import net.sf.okapi.common.exceptions.OkapiMergeException;
import net.sf.okapi.common.filterwriter.IFilterWriter;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.RawDocument;
import net.sf.okapi.common.resource.Segment;
import net.sf.okapi.common.skeleton.GenericSkeleton;
import net.sf.okapi.common.skeleton.GenericSkeletonPart;
import net.sf.okapi.filters.xliff.XLIFFFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.io.File;
import java.net.URL;
import java.util.*;
import java.util.prefs.Preferences;

import static net.sf.okapi.common.LocaleId.ENGLISH;
import static net.sf.okapi.common.LocaleId.KOREAN;

@Controller
@FxmlView("MainFx.fxml")
public class MainFxController implements Initializable {

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
    private ListView<String> lstFiles = new ListView<>();

    private static final String DESKTOP_PATH = System.getProperty("user.home") + "\\Desktop";
    private static final String SOURCE_PATH = "source_path";
    private static final String JSON = "json_path";
    private static final String PROJECT = "project_id";
    private static final String MODEL = "model_id";
    private static final String LOCATION = "location_id";
    private static final String GLOSSARY = "glossary_id";
    private static final Preferences preference = Preferences.userNodeForPackage(MainFxController.class);
    private AdvancedTranslationSettings settings;

    private XLIFFFilter filter = new XLIFFFilter();

    private enum FLAG {
        SOURCE, TARGET
    }

    private enum MODE {
        SINGLE, MULTI
    }

    @Autowired
    public MainFxController() {
        setAdvancedTranslationSettings();
    }

    /*
    * Google Translation V3의 설정값
    */
    private void setAdvancedTranslationSettings() {
        String json = preference.get(JSON, "");
        String project = preference.get(PROJECT, "34036614342");
        String location = preference.get(LOCATION, "us-central1");
        String model = preference.get(MODEL, "TRL6331012141990019072");
        String glossary = preference.get(GLOSSARY, "ko_en_glossary_20211229");

        settings = new AdvancedTranslationSettings(json, project, location, model, glossary);
    }

    /*
    * 학습형 구글번역(NMT)를 적용합니다.
    */
    @FXML
    void clickBtnApplyNMT(ActionEvent event) {
        //리스트에 파일목록이 없으면 종료
        if (lstFiles.getItems().size() < 1) {
            String title = "Error";
            String header = "Error";
            String msg = "No file selected.";
            showMsgbox(title, header, msg, Alert.AlertType.ERROR);
            return;
        }

        String msg;
        try {
            //목록에 있는 각 파일들에 대하여 NMT 적용
            List<String> sourcePaths = lstFiles.getItems();
            for (String path : sourcePaths) {
                File sourceFile = new File(path);
                preference.put(SOURCE_PATH, sourceFile.getParent());

                List<Event> allFilterEvents = getAllEventsFromFilter(sourceFile);
                List<ITextUnit> textUnits = getTextUnits(allFilterEvents);

                Map<String, String> sourceSegmentMap = getSegmentMap(textUnits, FLAG.SOURCE);

                AdvancedTranslate at = new AdvancedTranslate(settings);
                Map<String, String> batchSegmentMap = at.batchTranslateTextWithGlossaryAndModel(sourceSegmentMap);

                Map<String, String> targetSegmentMap = getSegmentMap(textUnits, FLAG.TARGET);
                replaceToTranslatedText(targetSegmentMap, batchSegmentMap);

                applyTranslatedTextToTextUnit(textUnits, targetSegmentMap);

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
    private List<Event> getAllEventsFromFilter(File sourceFile) {
        List<Event> events = new ArrayList<>();
        RawDocument document = new RawDocument(sourceFile.toURI(), "UTF-8-BOM", KOREAN, ENGLISH);
        filter.open(document);
        while (filter.hasNext()) {
            events.add(filter.next());
        }
        return events;
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
            if (filter != null) filter.close();
        }
    }

    /*
    * Google Translation V3 API에서 받아온 문장 리스트를 타겟 세그먼트에 적용 시킵니다.
    */
    private void applyTranslatedTextToTextUnit(List<ITextUnit> textUnits, Map<String, String> targetSegmentMap) {
        for (ITextUnit tu : textUnits) {
            List<Segment> segments = tu.getTargetSegments(ENGLISH).asList();
            for (Segment segment : segments) {
                int id = Integer.parseInt(segment.getId());
                if (id > 0 && segment.text.getText().trim().isEmpty()) {
                    boolean lockedSegment = false;

                    GenericSkeleton genericSkeleton = (GenericSkeleton) tu.getSkeleton();
                    for (GenericSkeletonPart gsp : genericSkeleton.getParts()) {
                        StringBuilder data = gsp.getData();

                        //잠금 세그먼트 식별
                        if (data.indexOf("locked=\"true\"") > 0) {
                            lockedSegment = true;
                            continue;
                        }

                        //세그먼트 상태변경 -> 초안, NMT
                        if (data.indexOf("id=\"" + id + "\"") > 0 &&
                                data.indexOf("conf=") == -1 && data.indexOf("origin=") == -1) {
                            int start = data.indexOf("id=\"" + id + "\"") + ("id=\"" + id + "\"").length();
                            int end = start;
                            data.replace(start, end, " conf=\"Draft\" origin=\"nmt\"");
                        }
                    }
                    // 잠긴 세그먼트 건너뜀
                    if (!lockedSegment) {
                        segment.text.setCodedText(targetSegmentMap.get(segment.getId()));
                    }
                }
            }
        }
    }

    /*
    * 타겟 세그먼트의 문장들을 NMT 결과로 교체합니다.
    */
    private void replaceToTranslatedText(Map<String, String> targetSegmentMap, Map<String, String> batchSegmentMap) {
        for (int i = 1; i <= targetSegmentMap.size(); i++) {
            String key = String.valueOf(i);
            targetSegmentMap.put(key, batchSegmentMap.get(key));
        }
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
                segments = tu.getTargetSegments(ENGLISH).asList();
            }
            for (int i = 0; i < segments.size(); i++) {
                Segment segment = segments.get(i);
                if (Integer.parseInt(segment.getId()) > 0) {
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
    * Google Translation V3 설정값을 Preferences에 저장합니다.
    */
    @FXML
    void clickBtnSaveSettings(ActionEvent event) {
        Platform.runLater(() -> {
            preference.put(JSON, txtJsonPath.getText());
            preference.put(PROJECT, txtProject.getText());
            preference.put(MODEL, txtModel.getText());
            preference.put(LOCATION, txtLocation.getText());
            preference.put(GLOSSARY, txtGlossary.getText());
        });

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

    /*
    * Window Panel 초기화 이벤트
    */
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        Platform.runLater(() -> {
            txtJsonPath.setText(settings.getJson());
            txtProject.setText(settings.getProject());
            txtLocation.setText(settings.getLocation());
            txtModel.setText(settings.getModel());
            txtGlossary.setText(settings.getGlossary());
        });
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

}

