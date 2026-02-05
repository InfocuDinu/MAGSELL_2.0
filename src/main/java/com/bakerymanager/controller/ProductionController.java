package com.bakerymanager.controller;

import com.bakerymanager.entity.Product;
import com.bakerymanager.entity.RecipeItem;
import com.bakerymanager.entity.Ingredient;
import com.bakerymanager.service.IngredientService;
import com.bakerymanager.service.ProductionService;
import com.bakerymanager.service.ProductService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Controller
public class ProductionController {
    
    private final ProductionService productionService;
    private final ProductService productService;
    private final IngredientService ingredientService;
    
    public ProductionController(ProductionService productionService, 
                              ProductService productService,
                              IngredientService ingredientService) {
        this.productionService = productionService;
        this.productService = productService;
        this.ingredientService = ingredientService;
    }
    
    @FXML
    private ComboBox<Product> productComboBox;
    
    @FXML
    private TextField quantityField;
    
    @FXML
    private Label productionStatusLabel;
    
    @FXML
    private Label stockStatusLabel;
    
    @FXML
    private TableView<RecipeItem> recipeTable;
    
    @FXML
    private TableColumn<RecipeItem, String> recipeIngredientColumn;
    
    @FXML
    private TableColumn<RecipeItem, BigDecimal> recipeQuantityColumn;
    
    @FXML
    private TableColumn<RecipeItem, String> recipeUnitColumn;
    
    @FXML
    private TableColumn<RecipeItem, BigDecimal> recipeAvailableColumn;
    
    @FXML
    private TableColumn<RecipeItem, Void> recipeActionsColumn;
    
    @FXML
    private TableView<ProductionRecord> productionHistoryTable;
    
    @FXML
    private TableColumn<ProductionRecord, String> historyDateColumn;
    
    @FXML
    private TableColumn<ProductionRecord, String> historyProductColumn;
    
    @FXML
    private TableColumn<ProductionRecord, BigDecimal> historyQuantityColumn;
    
    @FXML
    private TableColumn<ProductionRecord, String> historyStatusColumn;
    
    @FXML
    private Label productionInfoLabel;
    
    private ObservableList<RecipeItem> recipeItems = FXCollections.observableArrayList();
    private ObservableList<ProductionRecord> productionHistory = FXCollections.observableArrayList();
    private Product selectedProduct;
    
    public static class ProductionRecord {
        private LocalDateTime date;
        private String productName;
        private BigDecimal quantity;
        private String status;
        
        public ProductionRecord(LocalDateTime date, String productName, BigDecimal quantity, String status) {
            this.date = date;
            this.productName = productName;
            this.quantity = quantity;
            this.status = status;
        }
        
        public String getFormattedDate() {
            return date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
        }
        
        public LocalDateTime getDate() { return date; }
        public String getProductName() { return productName; }
        public BigDecimal getQuantity() { return quantity; }
        public String getStatus() { return status; }
    }
    
    @FXML
    public void initialize() {
        setupProductComboBox();
        setupRecipeTable();
        setupProductionHistoryTable();
        loadProducts();
        loadProductionHistory();
        System.out.println("Production controller initialized");
    }
    
    private void setupProductComboBox() {
        productComboBox.setItems(FXCollections.observableArrayList(productService.getActiveProducts()));
        productComboBox.setOnAction(event -> {
            selectedProduct = productComboBox.getValue();
            if (selectedProduct != null) {
                loadRecipe();
                updateProductionInfo();
            }
        });
    }
    
    private void setupRecipeTable() {
        recipeIngredientColumn.setCellValueFactory(param -> {
            RecipeItem item = param.getValue();
            return new javafx.beans.property.SimpleObjectProperty<>(item != null && item.getIngredient() != null ? item.getIngredient().getName() : "");
        });
        
        recipeQuantityColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("requiredQuantity"));
        
        recipeUnitColumn.setCellValueFactory(param -> {
            RecipeItem item = param.getValue();
            return new javafx.beans.property.SimpleObjectProperty<>(item != null && item.getIngredient() != null ? item.getIngredient().getUnitOfMeasure().getDisplayName() : "");
        });
        
        recipeAvailableColumn.setCellValueFactory(param -> {
            RecipeItem item = param.getValue();
            return new javafx.beans.property.SimpleObjectProperty<>(item != null && item.getIngredient() != null ? item.getIngredient().getCurrentStock() : BigDecimal.ZERO);
        });
        
        setupRecipeActionsColumn();
        
        recipeTable.setItems(recipeItems);
    }
    
    private void setupRecipeActionsColumn() {
        recipeActionsColumn.setCellFactory(param -> new TableCell<>() {
            private final Button removeButton = new Button("Șterge");
            
            {
                removeButton.getStyleClass().addAll("button", "danger");
                removeButton.setOnAction(event -> {
                    RecipeItem item = getTableView().getItems().get(getIndex());
                    removeRecipeItem(item);
                });
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(removeButton);
                }
            }
        });
    }
    
    private void setupProductionHistoryTable() {
        historyDateColumn.setCellValueFactory(new PropertyValueFactory<>("formattedDate"));
        historyProductColumn.setCellValueFactory(new PropertyValueFactory<>("productName"));
        historyQuantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        historyStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        
        productionHistoryTable.setItems(productionHistory);
    }
    
    private void loadProducts() {
        productComboBox.setItems(FXCollections.observableArrayList(productService.getActiveProducts()));
    }
    
    private void loadRecipe() {
        if (selectedProduct != null) {
            List<RecipeItem> items = productionService.getRecipeByProduct(selectedProduct);
            recipeItems.clear();
            recipeItems.addAll(items);
        }
    }
    
    private void refreshProductionHistory() {
        productionHistory.clear();
    }

    @FXML
    public void loadProductionHistory() {
        refreshProductionHistory();
        productionStatusLabel.setText("Istoric actualizat");
    }
    
    @FXML
    public void executeProduction() {
        if (selectedProduct == null) {
            showError("Selectați un produs!");
            return;
        }
        
        String quantityText = quantityField.getText().trim();
        if (quantityText.isEmpty()) {
            showError("Introduceți cantitatea!");
            return;
        }
        
        try {
            BigDecimal quantity = new BigDecimal(quantityText);
            if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
                showError("Cantitatea trebuie să fie pozitivă!");
                return;
            }
            
            productionStatusLabel.setText("Producție în curs...");
            
            if (!productionService.canProduce(selectedProduct.getId(), quantity)) {
                showError("Stoc insuficient pentru producție!");
                stockStatusLabel.setText("❌ Stoc insuficient");
                productionStatusLabel.setText("Eroare producție");
                return;
            }
            
            productionService.executeProduction(selectedProduct.getId(), quantity);
            
            productionHistory.add(0, new ProductionRecord(
                LocalDateTime.now(),
                selectedProduct.getName(),
                quantity,
                "Succes"
            ));
            
            loadProducts();
            loadRecipe();
            updateProductionInfo();
            
            productionStatusLabel.setText("✅ Producție finalizată");
            stockStatusLabel.setText("✅ Stocuri actualizate");
            
            showSuccessMessage("Producție executată cu succes!");
            
        } catch (NumberFormatException e) {
            showError("Cantitate invalidă!");
            productionStatusLabel.setText("Eroare producție");
        } catch (Exception e) {
            showError("Eroare la producție: " + e.getMessage());
            productionStatusLabel.setText("Eroare producție");
        }
    }
    
    @FXML
    public void checkStock() {
        if (selectedProduct == null) {
            showError("Selectați un produs!");
            return;
        }
        
        String quantityText = quantityField.getText().trim();
        if (quantityText.isEmpty()) {
            showError("Introduceți cantitatea!");
            return;
        }
        
        try {
            BigDecimal quantity = new BigDecimal(quantityText);
            Map<Ingredient, BigDecimal> requiredIngredients = 
                productionService.calculateRequiredIngredients(selectedProduct.getId(), quantity);
            
            StringBuilder stockInfo = new StringBuilder();
            stockInfo.append("Verificare stoc pentru ").append(quantity)
                     .append(" ").append(selectedProduct.getName()).append(":\n\n");
            
            boolean allSufficient = true;
            for (Map.Entry<Ingredient, BigDecimal> entry : requiredIngredients.entrySet()) {
                Ingredient ingredient = entry.getKey();
                BigDecimal required = entry.getValue();
                BigDecimal available = ingredient.getCurrentStock();
                
                boolean sufficient = available.compareTo(required) >= 0;
                allSufficient &= sufficient;
                
                stockInfo.append("• ").append(ingredient.getName())
                         .append(": necesar ").append(required)
                         .append(" ").append(ingredient.getUnitOfMeasure().getDisplayName())
                         .append(", disponibil ").append(available)
                         .append(" ").append(ingredient.getUnitOfMeasure().getDisplayName())
                         .append(" ").append(sufficient ? "✅" : "❌")
                         .append("\n");
            }
            
            if (allSufficient) {
                stockInfo.append("\n✅ Stocuri suficiente pentru producție!");
                stockStatusLabel.setText("✅ Stocuri OK");
            } else {
                stockInfo.append("\n❌ Stocuri insuficiente!");
                stockStatusLabel.setText("❌ Stoc insuficient");
            }
            
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Verificare Stoc");
            alert.setHeaderText(null);
            alert.setContentText(stockInfo.toString());
            alert.getDialogPane().setPrefWidth(400);
            alert.show();
            
        } catch (NumberFormatException e) {
            showError("Cantitate invalidă!");
        } catch (Exception e) {
            showError("Eroare la verificarea stocului: " + e.getMessage());
        }
    }
    
    @FXML
    public void addRecipeItem() {
        if (selectedProduct == null) {
            showError("Selectați un produs mai întâi!");
            return;
        }
        
        Dialog<RecipeItemData> dialog = createRecipeItemDialog();
        dialog.showAndWait().ifPresent(data -> {
            productionService.addRecipeItem(selectedProduct.getId(), data.ingredientId, data.quantity);
            loadRecipe();
            showSuccessMessage("Ingredient adăugat la rețetă!");
        });
    }
    
    @FXML
    public void createNewRecipe() {
        if (selectedProduct == null) {
            showError("Selectați un produs mai întâi!");
            return;
        }
        
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Rețetă Nouă");
        alert.setHeaderText("Creați o rețetă nouă pentru " + selectedProduct.getName() + "?");
        alert.setContentText("Aceasta va șterge rețeta existentă (dacă există).");
        
        if (alert.showAndWait().get() == ButtonType.OK) {
            recipeItems.clear();
            showSuccessMessage("Rețetă nouă creată. Adăugați ingrediente.");
        }
    }
    
    private void removeRecipeItem(RecipeItem item) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmare Ștergere");
        alert.setHeaderText("Ștergeți acest ingredient din rețetă?");
        alert.setContentText("Ingredient: " + (item.getIngredient() != null ? item.getIngredient().getName() : ""));
        
        if (alert.showAndWait().get() == ButtonType.OK) {
            productionService.removeRecipeItem(item.getId());
            loadRecipe();
            showSuccessMessage("Ingredient șters din rețetă!");
        }
    }
    
    @FXML
    public void showProductionReport() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Raport Producție");
        alert.setHeaderText("Raport Producție - " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
        
        StringBuilder report = new StringBuilder();
        report.append("Total producții astăzi: ").append(productionHistory.size()).append("\n");
        
        BigDecimal totalProduced = productionHistory.stream()
            .map(ProductionRecord::getQuantity)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        report.append("Cantitate totală produsă: ").append(totalProduced).append("\n\n");
        
        report.append("Detalii producții:\n");
        for (ProductionRecord record : productionHistory) {
            report.append("• ").append(record.getFormattedDate())
                 .append(" - ").append(record.getProductName())
                 .append(" (").append(record.getQuantity()).append(")")
                 .append(" - ").append(record.getStatus()).append("\n");
        }
        
        alert.setContentText(report.toString());
        alert.getDialogPane().setPrefWidth(500);
        alert.show();
    }
    
    private void updateProductionInfo() {
        if (selectedProduct != null) {
            productionInfoLabel.setText(
                "Produs selectat: " + selectedProduct.getName() + 
                " | Stoc curent: " + selectedProduct.getPhysicalStock()
            );
        } else {
            productionInfoLabel.setText("Selectați un produs pentru a începe producția");
        }
    }
    
    private Dialog<RecipeItemData> createRecipeItemDialog() {
        Dialog<RecipeItemData> dialog = new Dialog<>();
        dialog.setTitle("Adaugă Ingredient la Rețetă");
        dialog.setHeaderText(null);
        
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));
        
        ComboBox<Ingredient> ingredientCombo = new ComboBox<>();
        ingredientCombo.setItems(FXCollections.observableArrayList(ingredientService.getAllIngredients()));
        
        TextField quantityField = new TextField();
        quantityField.setPromptText("Cantitate necesară per unitate de produs");
        
        grid.add(new Label("Ingredient:"), 0, 0);
        grid.add(ingredientCombo, 1, 0);
        grid.add(new Label("Cantitate:"), 0, 1);
        grid.add(quantityField, 1, 1);
        
        dialog.getDialogPane().setContent(grid);
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                Ingredient ingredient = ingredientCombo.getValue();
                if (ingredient == null) {
                    showError("Selectați un ingredient!");
                    return null;
                }
                
                try {
                    BigDecimal quantity = new BigDecimal(quantityField.getText());
                    return new RecipeItemData(ingredient.getId(), quantity);
                } catch (NumberFormatException e) {
                    showError("Cantitate invalidă!");
                    return null;
                }
            }
            return null;
        });
        
        return dialog;
    }
    
    private static class RecipeItemData {
        Long ingredientId;
        BigDecimal quantity;
        
        public RecipeItemData(Long ingredientId, BigDecimal quantity) {
            this.ingredientId = ingredientId;
            this.quantity = quantity;
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
