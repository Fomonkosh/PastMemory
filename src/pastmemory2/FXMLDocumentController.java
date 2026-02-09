/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/javafx/FXMLController.java to edit this template
 */
package pastmemory2;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;
import java.util.ResourceBundle;

/**
 * FXML Controller class
 *
 * @author lukas
 */
public class FXMLDocumentController implements Initializable {

    @FXML
    private TextField tfUrl;
    @FXML
    private TextField tfDesc;
    @FXML
    private ComboBox<String> cbCategory;

   
     private final ObservableList<String> categories = FXCollections.observableArrayList();

   // storage folder and files
    private final Path appDir = Paths.get(System.getProperty("user.home"), ".past-memory");
    private final Path categoriesFile = appDir.resolve("categories.txt");
    private final Path linksFile = appDir.resolve("links.csv");

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        cbCategory.setItems(categories);

        ensureStorage();
        loadCategories();
    }

    @FXML
    private void onAdd(ActionEvent event) {
        String link = tfUrl.getText().trim();
        String cat  = cbCategory.getEditor().getText().trim();   // important for editable ComboBox
        String desc = tfDesc.getText().trim();                   // may be empty, I need to remember to clean the table

        if (link.isEmpty() || cat.isEmpty()) {
            showError("URL and Category are required.");
            return;
        }

        // 1) if the category is new - add and save
        if (!categories.contains(cat)) {
            categories.add(cat);
            appendCategory(cat);
        }

        // 2) add a row to the links.csv table
        appendLink(link, cat, desc);

        // 3) clear fields
        tfUrl.clear();
        tfDesc.clear();
        cbCategory.getEditor().clear();
        tfUrl.requestFocus();
    }

    // ---------- storage helpers ----------

    private void ensureStorage() {
        try {
            Files.createDirectories(appDir);
            if (!Files.exists(categoriesFile)) Files.createFile(categoriesFile);
            if (!Files.exists(linksFile)) {
                Files.createFile(linksFile);
                // заголовок (необязательно, но удобно)
                Files.writeString(linksFile, "url;category;description\n", StandardCharsets.UTF_8, StandardOpenOption.APPEND);
            }
        } catch (IOException e) {
            showError("Failed to create repository: " + e.getMessage());
        }
    }

    private void loadCategories() {
        try {
            List<String> lines = Files.readAllLines(categoriesFile, StandardCharsets.UTF_8);
            for (String line : lines) {
                String c = line.trim();
                if (!c.isEmpty() && !categories.contains(c)) {
                    categories.add(c);
                }
            }
        } catch (IOException e) {
            showError("Failed to load categories: " + e.getMessage());
        }
    }

    private void appendCategory(String cat) {
        try {
            Files.writeString(categoriesFile, cat + "\n", StandardCharsets.UTF_8, StandardOpenOption.APPEND);
        } catch (IOException e) {
            showError("Failed to load categories: " + e.getMessage());
        }
    }

    private void appendLink(String url, String cat, String desc) {
        // simple escape of the separator ; and line breaks, preparation for the table
        String safeUrl  = escape(url);
        String safeCat  = escape(cat);
        String safeDesc = escape(desc);

        String row = safeUrl + " ; " + safeCat + " ; " + safeDesc + "\n";

        try {
            Files.writeString(linksFile, row, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
        } catch (IOException e) {
            showError("Failed to save the entry: " + e.getMessage());
        }
    }

    private String escape(String s) {
        // replace ; with a comma (you can do it differently)
        return s.replace(";", ",").replace("\n", " ").replace("\r", " ").trim();
    }

    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
    
}
