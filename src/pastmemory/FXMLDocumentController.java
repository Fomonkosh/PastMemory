/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/javafx/FXMLController.java to edit this template
 */
package pastmemory;

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
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

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
    @FXML
    private TextField tfDescS;
    @FXML
    private TableView<LinkItem> tableResults;

    private final ObservableList<LinkItem> results = FXCollections.observableArrayList();

    @FXML
    private TableColumn<LinkItem, String> colUrl;
    @FXML
    private TableColumn<LinkItem, String> colCategory;
    @FXML
    private TableColumn<LinkItem, String> colDesc;
    @FXML
    private ComboBox<String> cbSearchCategory;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        cbCategory.setItems(categories);
        cbSearchCategory.setItems(categories);
        tableResults.setItems(results);

        ensureStorage();
        loadCategories();
        
        if (!categories.contains("")) {
    categories.add(0, "");
}

        colUrl.setCellValueFactory(new PropertyValueFactory<>("url"));
        colCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        colDesc.setCellValueFactory(new PropertyValueFactory<>("description"));
    }

    @FXML
    private void onAdd(ActionEvent event) {
        String link = tfUrl.getText().trim();
        String cat = cbCategory.getEditor().getText().trim();   // important for editable ComboBox
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
            if (!Files.exists(categoriesFile)) {
                Files.createFile(categoriesFile);
            }
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
        String safeUrl = escape(url);
        String safeCat = escape(cat);
        String safeDesc = escape(desc);

        String row = safeUrl + " ; " + safeCat + " ; " + safeDesc + "\n";

        try {
            Files.writeString(linksFile, row, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
        } catch (IOException e) {
            showError("Failed to save the entry: " + e.getMessage());
        }
    }

    private String escape(String s) {
        // replace ; with a comma-
        return s.replace(";", ",").replace("\n", " ").replace("\r", " ").trim();
    }

    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    public class LinkItem {

        private final String url;
        private final String category;
        private final String description;

        public LinkItem(String url, String category, String description) {
            this.url = url;
            this.category = category;
            this.description = description;
        }

        public String getUrl() {
            return url;
        }

        public String getCategory() {
            return category;
        }

        public String getDescription() {
            return description;
        }
    }

    @FXML
private void onSearch(ActionEvent e) {

    String selectedCategory = cbSearchCategory.getValue();
    String descQuery = tfDescS.getText().trim().toLowerCase();

    boolean hasCategory = selectedCategory != null && !selectedCategory.isBlank();
    boolean hasDesc = !descQuery.isBlank();

    if (!hasCategory && !hasDesc) {
        showError("Select a category or enter text to search.");
        return;
    }

    results.clear();

    try {
        List<String> lines = Files.readAllLines(linksFile, StandardCharsets.UTF_8);

        for (String line : lines) {
            if (line.isBlank()) continue;

            String cleanLine = line.trim();
            if (cleanLine.toLowerCase().startsWith("url;")) continue;

            String[] parts = cleanLine.split(";", -1);
            if (parts.length < 3) continue;

            String url = parts[0].trim();
            String category = parts[1].trim();
            String desc = parts[2].trim();

            boolean matchCategory = true;
            boolean matchDesc = true;

            // filter by category (if selected)
            if (hasCategory) {
                matchCategory = category.equalsIgnoreCase(selectedCategory.trim());
            }

            // filter by description (if text is entered)
            if (hasDesc) {
                matchDesc = desc.toLowerCase().contains(descQuery);
            }

            // add if ALL conditions are met
            if (matchCategory && matchDesc) {
                results.add(new LinkItem(url, category, desc));
            }
        }

    } catch (IOException ex) {
        showError("Error reading file: " + ex.getMessage());
    }
}

}
