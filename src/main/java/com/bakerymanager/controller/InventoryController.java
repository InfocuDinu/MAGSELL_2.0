package com.bakerymanager.controller;

import com.bakerymanager.entity.Ingredient;
import com.bakerymanager.service.IngredientService;
import com.bakerymanager.service.InvoiceService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.util.List;

@Controller
public class InventoryController {
    
    private static final Logger logger = LoggerFactory.getLogger(InventoryController.class);
    
    private final IngredientService ingredientService;
    private final InvoiceService invoiceService;
    
    public InventoryController(IngredientService ingredientService, InvoiceService invoiceService) {
        this.ingredientService = ingredientService;
        this.invoiceService = invoiceService;
    }
    
    // Form fields
    @FXML
    private TextField nameField;
    
    @FXML
    private TextField quantityField;
    
    @FXML
    private ComboBox<Ingredient.UnitOfMeasure> unitCombo;
    
    @FXML
    private TextField priceField;
    
    @FXML
    private TextField minStockField;
    
    @FXML
    private TextField barcodeField;
    
    @FXML
    private Label statusLabel;
    
    @FXML
    private Label totalIngredientsLabel;
    
    // Table
    @FXML
    private TableView<Ingredient> ingredientsTable;
    
    private ObservableList<Ingredient> ingredientList = FXCollections.observableArrayList();
    private Ingredient selectedIngredient = null;
    
    @FXML
    public void initialize() {
        setupUnitComboBox();
        setupTable();
        loadIngredients();
        updateStatistics();
        logger.info("Inventory controller initialized");
    }
    
    private void setupUnitComboBox() {
        unitCombo.setItems(FXCollections.observableArrayList(Ingredient.UnitOfMeasure.values()));
    }
    
    private void setupTable() {
        // Create table columns programmatically
        TableColumn<Ingredient, Long> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setPrefWidth(50);
        
        TableColumn<Ingredient, String> nameCol = new TableColumn<>("Nume");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(150);
        
        TableColumn<Ingredient, Ingredient.UnitOfMeasure> unitCol = new TableColumn<>("Unitate");
        unitCol.setCellValueFactory(new PropertyValueFactory<>("unitOfMeasure"));
        unitCol.setPrefWidth(80);
        
        TableColumn<Ingredient, BigDecimal> stockCol = new TableColumn<>("Stoc");
        stockCol.setCellValueFactory(new PropertyValueFactory<>("currentStock"));
        stockCol.setPrefWidth(80);
        
        TableColumn<Ingredient, BigDecimal> minStockCol = new TableColumn<>("Stoc Minim");
        minStockCol.setCellValueFactory(new PropertyValueFactory<>("minimumStock"));
        minStockCol.setPrefWidth(90);
        
        TableColumn<Ingredient, BigDecimal> priceCol = new TableColumn<>("Preț");
        priceCol.setCellValueFactory(new PropertyValueFactory<>("lastPurchasePrice"));
        priceCol.setPrefWidth(80);
        
        TableColumn<Ingredient, String> barcodeCol = new TableColumn<>("Cod Bare");
        barcodeCol.setCellValueFactory(new PropertyValueFactory<>("barcode"));
        barcodeCol.setPrefWidth(100);
        
        ingredientsTable.getColumns().clear();
        ingredientsTable.getColumns().addAll(idCol, nameCol, unitCol, stockCol, minStockCol, priceCol, barcodeCol);
        ingredientsTable.setItems(ingredientList);
        
        // Add selection listener to populate form
        ingredientsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                populateForm(newSelection);
            }
        });
    }
    
    @FXML
    public void loadIngredients() {
        try {
            List<Ingredient> ingredients = ingredientService.getAllIngredients();
            ingredientList.clear();
            ingredientList.addAll(ingredients);
            updateStatistics();
            setStatus("Ingrediente încărcate: " + ingredients.size());
            logger.info("Loaded {} ingredients", ingredients.size());
        } catch (Exception e) {
            logger.error("Error loading ingredients", e);
            showError("Eroare la încărcarea ingredientelor: " + e.getMessage());
        }
    }
    
    private void populateForm(Ingredient ingredient) {
        selectedIngredient = ingredient;
        nameField.setText(ingredient.getName());
        quantityField.setText(ingredient.getCurrentStock().toString());
        unitCombo.setValue(ingredient.getUnitOfMeasure());
        if (ingredient.getLastPurchasePrice() != null) {
            priceField.setText(ingredient.getLastPurchasePrice().toString());
        } else {
            priceField.clear();
        }
        if (ingredient.getMinimumStock() != null) {
            minStockField.setText(ingredient.getMinimumStock().toString());
        } else {
            minStockField.clear();
        }
        if (ingredient.getBarcode() != null) {
            barcodeField.setText(ingredient.getBarcode());
        } else {
            barcodeField.clear();
        }
        setStatus("Selectat: " + ingredient.getName());
    }
    
    @FXML
    public void addIngredient() {
        clearForm();
        nameField.requestFocus();
        setStatus("Gata pentru adăugare ingredient nou");
    }
    
    @FXML
    public void saveIngredient() {
        try {
            // Validate required fields
            if (nameField.getText() == null || nameField.getText().trim().isEmpty()) {
                showError("Numele ingredientului este obligatoriu!");
                return;
            }
            
            if (unitCombo.getValue() == null) {
                showError("Unitatea de măsură este obligatorie!");
                return;
            }
            
            // Determine if this is an update or create operation
            boolean isUpdate = selectedIngredient != null;
            
            // Create or update ingredient
            Ingredient ingredient = selectedIngredient != null ? selectedIngredient : new Ingredient();
            ingredient.setName(nameField.getText().trim());
            ingredient.setUnitOfMeasure(unitCombo.getValue());
            
            // Parse numeric fields
            BigDecimal quantity = parseDecimalField(quantityField, "Cantitatea");
            if (quantity == null) return;
            ingredient.setCurrentStock(quantity);
            
            BigDecimal price = parseDecimalField(priceField, "Prețul");
            if (price != null) {
                ingredient.setLastPurchasePrice(price);
            }
            
            BigDecimal minStock = parseDecimalField(minStockField, "Stocul minim");
            if (minStock == null) minStock = BigDecimal.ZERO;
            ingredient.setMinimumStock(minStock);
            
            // Set barcode
            if (barcodeField.getText() != null && !barcodeField.getText().trim().isEmpty()) {
                ingredient.setBarcode(barcodeField.getText().trim());
            }
            
            // Save ingredient
            ingredientService.saveIngredient(ingredient);
            
            // Reload and update
            loadIngredients();
            updateStatistics();
            clearForm();
            
            String message = isUpdate ? "Ingredient actualizat cu succes!" : "Ingredient adăugat cu succes!";
            showSuccessMessage(message);
            setStatus(message);
            
        } catch (Exception e) {
            logger.error("Error saving ingredient", e);
            showError("Eroare la salvarea ingredientului: " + e.getMessage());
        }
    }
    
    /**
     * Helper method to parse a decimal field with validation.
     * @param field The text field to parse
     * @param fieldName The name of the field for error messages
     * @return The parsed BigDecimal value, or null if the field is empty or parsing fails
     */
    private BigDecimal parseDecimalField(TextField field, String fieldName) {
        if (field.getText() == null || field.getText().trim().isEmpty()) {
            return null;
        }
        
        try {
            return new BigDecimal(field.getText().trim());
        } catch (NumberFormatException e) {
            showError(fieldName + " trebuie să fie un număr valid!");
            return null;
        }
    }
    
    @FXML
    public void deleteIngredient() {
        if (selectedIngredient == null) {
            showError("Selectați un ingredient din tabel pentru a-l șterge!");
            return;
        }
        
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmare Ștergere");
        alert.setHeaderText("Sunteți sigur că doriți să ștergeți acest ingredient?");
        alert.setContentText("Ingredient: " + selectedIngredient.getName());
        
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    ingredientService.deleteIngredient(selectedIngredient.getId());
                    loadIngredients();
                    updateStatistics();
                    clearForm();
                    showSuccessMessage("Ingredient șters cu succes!");
                    setStatus("Ingredient șters");
                } catch (Exception e) {
                    logger.error("Error deleting ingredient", e);
                    showError("Eroare la ștergerea ingredientului: " + e.getMessage());
                }
            }
        });
    }
    
    @FXML
    public void clearForm() {
        selectedIngredient = null;
        nameField.clear();
        quantityField.clear();
        unitCombo.setValue(null);
        priceField.clear();
        minStockField.clear();
        barcodeField.clear();
        ingredientsTable.getSelectionModel().clearSelection();
        setStatus("Formular golit");
    }
    
    private void updateStatistics() {
        List<Ingredient> ingredients = ingredientService.getAllIngredients();
        totalIngredientsLabel.setText("Total ingrediente: " + ingredients.size());
    }
    
    private void setStatus(String message) {
        if (statusLabel != null) {
            statusLabel.setText(message);
        }
    }
    
    private void showSuccessMessage(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Succes");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.show();
    }
    
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Eroare");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.show();
    }
}
