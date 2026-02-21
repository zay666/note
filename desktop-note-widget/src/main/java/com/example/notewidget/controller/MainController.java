package com.example.notewidget.controller;

import com.example.notewidget.model.NoteDocument;
import com.example.notewidget.service.MarkdownService;
import com.example.notewidget.service.NoteStorageService;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;

public class MainController {

    @FXML
    private BorderPane rootPane;

    @FXML
    private SplitPane mainSplitPane;

    @FXML
    private ListView<NoteDocument> fileListView;

    @FXML
    private TextArea editorArea;

    @FXML
    private WebView previewWebView;

    @FXML
    private Label editorTitleLabel;

    @FXML
    private Label statusLabel;

    private final ObservableList<NoteDocument> documents = FXCollections.observableArrayList();
    private final MarkdownService markdownService = new MarkdownService();
    private final NoteStorageService storageService = new NoteStorageService();
    private final PauseTransition previewDelay = new PauseTransition(Duration.millis(120));

    private Stage mainStage;
    private NoteDocument quickDraftDocument;
    private boolean updatingEditor;
    private Runnable userInteractionListener;

    @FXML
    private void initialize() {
        fileListView.setItems(documents);
        fileListView.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(NoteDocument item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    return;
                }

                String prefix = item.isTemporary() ? "[临时] " : "";
                String suffix = item.isModified() ? " *" : "";
                setText(prefix + item.getTitle() + suffix);
            }
        });

        previewWebView.getEngine().setJavaScriptEnabled(false);
        mainSplitPane.setDividerPositions(0.22, 0.63);

        fileListView.getSelectionModel().selectedItemProperty().addListener((obs, oldDoc, newDoc) -> {
            onDocumentSelected(newDoc);
        });

        previewDelay.setOnFinished(event -> renderPreview());

        editorArea.textProperty().addListener((obs, oldText, newText) -> {
            if (updatingEditor) {
                return;
            }

            NoteDocument current = getCurrentDocument();
            if (current == null) {
                return;
            }

            if (!Objects.equals(current.getContent(), newText)) {
                current.setContent(newText);
                current.setModified(true);
                fileListView.refresh();
            }

            if (current == quickDraftDocument) {
                storageService.saveQuickDraft(newText);
            }

            previewDelay.playFromStart();
        });

        rootPane.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> notifyUserInteraction());
        rootPane.addEventFilter(KeyEvent.KEY_PRESSED, event -> notifyUserInteraction());

        createInitialQuickDraftDocument();
        updateStatus("就绪");
    }

    public void setMainStage(Stage mainStage) {
        this.mainStage = mainStage;

        Scene scene = mainStage.getScene();
        if (scene != null) {
            registerAccelerators(scene);
        }

        mainStage.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                registerAccelerators(newScene);
            }
        });
    }

    public void setUserInteractionListener(Runnable userInteractionListener) {
        this.userInteractionListener = userInteractionListener;
    }

    @FXML
    private void handleOpenMarkdown() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("打开 Markdown 文件");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Markdown 文件 (*.md, *.markdown)", "*.md", "*.markdown")
        );

        File file = chooser.showOpenDialog(mainStage);
        if (file == null) {
            return;
        }

        notifyUserInteraction();

        Path target = file.toPath().toAbsolutePath().normalize();
        NoteDocument exists = findByPath(target);
        if (exists != null) {
            selectDocument(exists);
            updateStatus("已打开: " + target.getFileName());
            return;
        }

        try {
            String content = storageService.readMarkdown(target);
            NoteDocument doc = new NoteDocument(
                    target.getFileName().toString(),
                    target,
                    content,
                    false,
                    false
            );
            documents.add(doc);
            selectDocument(doc);
            updateStatus("打开成功: " + target);
        } catch (IOException e) {
            showError("打开失败", "无法读取选中的 Markdown 文件。", e);
        }
    }

    @FXML
    private void handleUploadImage() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("上传图片");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("图片文件", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp", "*.webp")
        );

        File file = chooser.showOpenDialog(mainStage);
        if (file == null) {
            return;
        }

        notifyUserInteraction();

        Path imagePath = file.toPath().toAbsolutePath().normalize();
        String imageName = imagePath.getFileName().toString();
        String altText = stripExtension(imageName);
        String imageMarkdown = "![" + altText + "](" + imagePath.toUri() + ")";

        int caret = editorArea.getCaretPosition();
        String insertion = "\n" + imageMarkdown + "\n";
        editorArea.insertText(caret, insertion);
        editorArea.positionCaret(caret + insertion.length());

        updateStatus("已插入图片: " + imageName);
    }

    @FXML
    private void handleNewTempNote() {
        notifyUserInteraction();

        String name = "临时笔记-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMdd-HHmmss"));

        NoteDocument doc = new NoteDocument(name, null, "", true, false);
        documents.add(0, doc);
        selectDocument(doc);
        updateStatus("已新建临时笔记");
    }

    @FXML
    private void handleSaveCurrent() {
        notifyUserInteraction();

        NoteDocument current = getCurrentDocument();
        if (current == null) {
            updateStatus("未选择笔记");
            return;
        }

        Path target = current.getPath();
        if (target == null) {
            target = showSaveDialog(current.getTitle());
            if (target == null) {
                return;
            }
        }

        saveDocumentToPath(current, target);
    }

    @FXML
    private void handleSaveAs() {
        notifyUserInteraction();

        NoteDocument current = getCurrentDocument();
        if (current == null) {
            updateStatus("未选择笔记");
            return;
        }

        Path target = showSaveDialog(current.getTitle());
        if (target == null) {
            return;
        }

        saveDocumentToPath(current, target);
    }

    @FXML
    private void handleExitApp() {
        Platform.exit();
    }

    private void registerAccelerators(Scene scene) {
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN),
                this::handleSaveCurrent
        );
    }

    private void saveDocumentToPath(NoteDocument doc, Path target) {
        try {
            String latestContent = editorArea.getText();
            doc.setContent(latestContent);
            storageService.writeMarkdown(target, latestContent);

            if (doc == quickDraftDocument) {
                storageService.clearQuickDraft();
                quickDraftDocument = null;
            }

            doc.setPath(target);
            doc.setTitle(target.getFileName().toString());
            doc.setTemporary(false);
            doc.setModified(false);

            fileListView.refresh();
            updateStatus("已保存: " + target);
        } catch (IOException e) {
            showError("保存失败", "写入文件失败。", e);
        }
    }

    private Path showSaveDialog(String currentTitle) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("保存 Markdown 文件");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Markdown 文件 (*.md)", "*.md")
        );

        String defaultName = (currentTitle == null || currentTitle.isBlank()) ? "note.md" : currentTitle;
        if (!defaultName.toLowerCase(Locale.ROOT).endsWith(".md")) {
            defaultName += ".md";
        }
        chooser.setInitialFileName(defaultName);

        File selected = chooser.showSaveDialog(mainStage);
        if (selected == null) {
            return null;
        }

        Path path = selected.toPath().toAbsolutePath().normalize();
        String lowerName = path.getFileName().toString().toLowerCase(Locale.ROOT);
        if (!lowerName.endsWith(".md")) {
            path = path.resolveSibling(path.getFileName() + ".md");
        }
        return path;
    }

    private void createInitialQuickDraftDocument() {
        String draft = storageService.loadQuickDraft();
        quickDraftDocument = new NoteDocument("临时随手记", null, draft, true, false);
        documents.add(quickDraftDocument);
        selectDocument(quickDraftDocument);
    }

    private void onDocumentSelected(NoteDocument doc) {
        if (doc == null) {
            updatingEditor = true;
            editorArea.clear();
            editorTitleLabel.setText("未选择文件");
            updatingEditor = false;
            renderPreview();
            return;
        }

        updatingEditor = true;
        editorArea.setText(doc.getContent());
        editorTitleLabel.setText(doc.getTitle() + (doc.isTemporary() ? " (临时)" : ""));
        updatingEditor = false;

        renderPreview();
    }

    private void renderPreview() {
        String html = markdownService.toHtmlDocument(editorArea.getText());
        previewWebView.getEngine().loadContent(html);
    }

    private NoteDocument getCurrentDocument() {
        return fileListView.getSelectionModel().getSelectedItem();
    }

    private NoteDocument findByPath(Path target) {
        for (NoteDocument doc : documents) {
            if (doc.getPath() != null && doc.getPath().equals(target)) {
                return doc;
            }
        }
        return null;
    }

    private void selectDocument(NoteDocument doc) {
        fileListView.getSelectionModel().select(doc);
        fileListView.scrollTo(doc);
    }

    private void notifyUserInteraction() {
        if (userInteractionListener != null) {
            userInteractionListener.run();
        }
    }

    private static String stripExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(0, dot) : filename;
    }

    private void updateStatus(String text) {
        statusLabel.setText(text);
    }

    private void showError(String title, String header, Exception ex) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.initOwner(mainStage);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(ex.getMessage());
        alert.showAndWait();
        updateStatus(title + ": " + ex.getMessage());
    }
}
