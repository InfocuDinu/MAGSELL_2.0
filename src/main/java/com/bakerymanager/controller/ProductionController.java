package com.bakerymanager.controller;

import com.bakerymanager.entity.Product;
import com.bakerymanager.entity.ProductionReport;
import com.bakerymanager.entity.RecipeItem;
import com.bakerymanager.entity.Ingredient;
import com.bakerymanager.service.IngredientService;
import com.bakerymanager.service.ProductionService;
import com.bakerymanager.service.ProductService;
import com.bakerymanager.service.PdfService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Controller
public class ProductionController {
    
    private static final Logger logger = LoggerFactory.getLogger(ProductionController.class);
    
    private final ProductionService productionService;
    private final ProductService productService;
    private final IngredientService ingredientService;
    private final PdfService pdfService;
    
    public ProductionController(ProductionService productionService, 
                              ProductService productService,
                              IngredientService ingredientService,
                              PdfService pdfService) {
        this.productionService = productionService;
        this.productService = productService;
        this.ingredientService = ingredientService;
        this.pdfService = pdfService;
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
        private ProductionReport report; // Store the actual report entity
        
        public ProductionRecord(LocalDateTime date, String productName, BigDecimal quantity, String status) {
            this.date = date;
            this.productName = productName;
            this.quantity = quantity;
            this.status = status;
        }
        
        public ProductionRecord(ProductionReport report) {
            this.report = report;
            this.date = report.getProductionDate() != null ? report.getProductionDate() : LocalDateTime.now();
            this.productName = report.getProduct() != null ? report.getProduct().getName() : "";
            this.quantity = report.getQuantityProduced();
            this.status = report.getStatus() != null ? report.getStatus().name() : "";
        }
        
        public ProductionReport getReport() {
            return report;
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
        logger.info("Production controller initialized");
    }
    
    private void setupProductComboBox() {
        productComboBox.setItems(FXCollections.observableArrayList(productService.getActiveProducts()));
        
        // Setăm cum să afișăm produsele în ComboBox
        productComboBox.setConverter(new javafx.util.StringConverter<Product>() {
            @Override
            public String toString(Product product) {
                return product != null ? product.getName() : "";
            }
            
            @Override
            public Product fromString(String string) {
                // Căutăm produsul după nume
                return productService.getActiveProducts().stream()
                    .filter(p -> p.getName().equals(string))
                    .findFirst()
                    .orElse(null);
            }
        });
        
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
            try {
                if (item != null && item.getIngredient() != null) {
                    return new javafx.beans.property.SimpleStringProperty(item.getIngredient().getName());
                } else if (item != null && item.getIngredientId() != null) {
                    // Încercăm să încărcăm ingredientul după ID dacă e lazy loaded
                    Ingredient ingredient = ingredientService.getIngredientById(item.getIngredientId()).orElse(null);
                    if (ingredient != null) {
                        return new javafx.beans.property.SimpleStringProperty(ingredient.getName());
                    }
                }
            } catch (Exception e) {
                logger.error("Error displaying ingredient", e);
            }
            return new javafx.beans.property.SimpleStringProperty("Ingredient necunoscut");
        });
        
        recipeQuantityColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("requiredQuantity"));
        
        recipeUnitColumn.setCellValueFactory(param -> {
            RecipeItem item = param.getValue();
            try {
                if (item != null && item.getIngredient() != null) {
                    return new javafx.beans.property.SimpleStringProperty(item.getIngredient().getUnitOfMeasure().getDisplayName());
                } else if (item != null && item.getIngredientId() != null) {
                    Ingredient ingredient = ingredientService.getIngredientById(item.getIngredientId()).orElse(null);
                    if (ingredient != null) {
                        return new javafx.beans.property.SimpleStringProperty(ingredient.getUnitOfMeasure().getDisplayName());
                    }
                }
            } catch (Exception e) {
                logger.error("Error displaying unit", e);
            }
            return new javafx.beans.property.SimpleStringProperty("-");
        });
        
        recipeAvailableColumn.setCellValueFactory(param -> {
            RecipeItem item = param.getValue();
            try {
                if (item != null && item.getIngredient() != null) {
                    return new javafx.beans.property.SimpleObjectProperty<>(item.getIngredient().getCurrentStock());
                } else if (item != null && item.getIngredientId() != null) {
                    Ingredient ingredient = ingredientService.getIngredientById(item.getIngredientId()).orElse(null);
                    if (ingredient != null) {
                        return new javafx.beans.property.SimpleObjectProperty<>(ingredient.getCurrentStock());
                    }
                }
            } catch (Exception e) {
                logger.error("Error displaying stock", e);
            }
            return new javafx.beans.property.SimpleObjectProperty<>(BigDecimal.ZERO);
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
        List<Product> products = productService.getActiveProducts();
        logger.debug("Available products: {}", products.size());
        for (Product p : products) {
            logger.debug("Product: {} (Stock: {}, Price: {})", p.getName(), p.getPhysicalStock(), p.getSalePrice());
        }
        productComboBox.setItems(FXCollections.observableArrayList(products));
    }
    
    private void loadRecipe() {
        if (selectedProduct != null) {
            List<RecipeItem> items = productionService.getRecipeByProduct(selectedProduct);
            recipeItems.clear();
            
            // Debug: Afișăm ce am găsit
            logger.debug("Recipe for {}: {} ingredients", selectedProduct.getName(), items.size());
            
            // Forțăm încărcarea ingredientelor pentru a evita lazy loading
            for (RecipeItem item : items) {
                if (item.getIngredient() != null) {
                    logger.debug("Ingredient: {} : {}", item.getIngredient().getName(), item.getRequiredQuantity());
                } else {
                    logger.warn("Null ingredient for ID: {}", item.getIngredientId());
                }
            }
            
            recipeItems.addAll(items);
        }
    }
    
    private void refreshProductionHistory() {
        try {
            productionHistory.clear();
            List<com.bakerymanager.entity.ProductionReport> reports = productionService.getAllProductionReports();
            
            for (com.bakerymanager.entity.ProductionReport report : reports) {
                ProductionRecord record = new ProductionRecord(report);
                productionHistory.add(record);
            }
            
            logger.info("Loaded {} production reports", reports.size());
        } catch (Exception e) {
            logger.error("Error loading production history", e);
            showError("Eroare la încărcarea istoricului: " + e.getMessage());
        }
    }

    @FXML
    public void loadProductionHistory() {
        refreshProductionHistory();
        productionStatusLabel.setText("Istoric actualizat");
    }
    
    @FXML
    public void refreshProducts() {
        loadProducts();
        productionStatusLabel.setText("Produse reîncărcate");
    }
    
    @FXML
    public void createNewProduct() {
        Dialog<Product> dialog = createProductDialog();
        dialog.showAndWait().ifPresent(product -> {
            productService.saveProduct(product);
            loadProducts(); // Reîncărcăm lista de produse
            
            // Selectăm automat produsul nou creat
            for (Product p : productComboBox.getItems()) {
                if (p.getId().equals(product.getId())) {
                    productComboBox.setValue(p);
                    selectedProduct = p;
                    loadRecipe();
                    updateProductionInfo();
                    break;
                }
            }
            
            productionStatusLabel.setText("Produs creat cu succes: " + product.getName());
            showSuccessMessage("Produs nou creat:\n" + product.getName() + "\nPreț: " + product.getSalePrice() + " lei");
        });
    }
    
    private Dialog<Product> createProductDialog() {
        Dialog<Product> dialog = new Dialog<>();
        dialog.setTitle("Produs Nou");
        dialog.setHeaderText("Creați un produs nou cu rețetă");
        
        // Buton OK
        ButtonType okButtonType = new ButtonType("Salvează", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okButtonType, ButtonType.CANCEL);
        
        // Câmpuri pentru produs
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        TextField nameField = new TextField();
        nameField.setPromptText("Nume produs");
        TextField priceField = new TextField();
        priceField.setPromptText("Preț vânzare (lei)");
        TextField stockField = new TextField();
        stockField.setPromptText("Stoc inițial");
        
        // Tabel pentru ingrediente
        TableView<RecipeIngredient> ingredientsTable = new TableView<>();
        ObservableList<RecipeIngredient> ingredients = FXCollections.observableArrayList();
        ingredientsTable.setItems(ingredients);
        ingredientsTable.setPrefHeight(200);
        
        // Simplificăm coloanele pentru a evita erorile de CSS
        TableColumn<RecipeIngredient, String> nameCol = new TableColumn<>("Ingredient");
        nameCol.setCellValueFactory(param -> {
            RecipeIngredient item = param.getValue();
            return new javafx.beans.property.SimpleStringProperty(item != null ? item.getName() : "");
        });
        nameCol.setPrefWidth(150);
        
        TableColumn<RecipeIngredient, String> quantityCol = new TableColumn<>("Cantitate");
        quantityCol.setCellValueFactory(param -> {
            RecipeIngredient item = param.getValue();
            return new javafx.beans.property.SimpleStringProperty(item != null ? item.getQuantity() : "");
        });
        quantityCol.setPrefWidth(100);
        
        TableColumn<RecipeIngredient, String> unitCol = new TableColumn<>("Unitate");
        unitCol.setCellValueFactory(param -> {
            RecipeIngredient item = param.getValue();
            return new javafx.beans.property.SimpleStringProperty(item != null ? item.getUnit() : "");
        });
        unitCol.setPrefWidth(80);
        
        ingredientsTable.getColumns().addAll(nameCol, quantityCol, unitCol);
        
        // Buton pentru adăugat ingredient
        Button addIngredientBtn = new Button("Adaugă Ingredient");
        addIngredientBtn.setOnAction(e -> {
            RecipeIngredient ingredient = showIngredientDialog();
            if (ingredient != null) {
                ingredients.add(ingredient);
            }
        });
        
        grid.add(new Label("Nume:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Preț:"), 0, 1);
        grid.add(priceField, 1, 1);
        grid.add(new Label("Stoc inițial:"), 0, 2);
        grid.add(stockField, 1, 2);
        grid.add(new Label("Ingrediente:"), 0, 3);
        grid.add(ingredientsTable, 1, 3);
        grid.add(addIngredientBtn, 1, 4);
        
        dialog.getDialogPane().setContent(grid);
        
        // Validare și salvare
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == okButtonType) {
                try {
                    String name = nameField.getText().trim();
                    if (name.isEmpty()) {
                        showError("Numele produsului este obligatoriu!");
                        return null;
                    }
                    
                    BigDecimal price = new BigDecimal(priceField.getText().trim());
                    BigDecimal stock = new BigDecimal(stockField.getText().trim());
                    
                    if (ingredients.isEmpty()) {
                        showError("Adăugați cel puțin un ingredient!");
                        return null;
                    }
                    
                    Product product = new Product();
                    product.setName(name);
                    product.setSalePrice(price);
                    product.setPhysicalStock(stock);
                    product.setMinimumStock(BigDecimal.ZERO);
                    product.setIsActive(true);
                    
                    // Salvăm produsul mai întâi pentru a obține ID
                    Product savedProduct = productService.saveProduct(product);
                    
                    // Creăm rețeta
                    for (RecipeIngredient ri : ingredients) {
                        productionService.addRecipeItem(savedProduct.getId(), ri.getIngredientId(), ri.getQuantityValue());
                    }
                    
                    return savedProduct;
                } catch (NumberFormatException e) {
                    showError("Prețul și stocul trebuie să fie numere valide!");
                    return null;
                } catch (Exception e) {
                    showError("Eroare la salvare: " + e.getMessage());
                    return null;
                }
            }
            return null;
        });
        
        return dialog;
    }
    
    private RecipeIngredient showIngredientDialog() {
        Dialog<RecipeIngredient> dialog = new Dialog<>();
        dialog.setTitle("Ingredient Rețetă");
        dialog.setHeaderText("Adăugați ingredient în rețetă");
        
        ButtonType okButtonType = new ButtonType("Adaugă", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okButtonType, ButtonType.CANCEL);
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        ComboBox<Ingredient> ingredientCombo = new ComboBox<>();
        List<Ingredient> ingredients = ingredientService.getAllIngredients();
        logger.debug("Available ingredients: {}", ingredients.size());
        for (Ingredient ing : ingredients) {
            logger.debug("Ingredient: {} (Stock: {})", ing.getName(), ing.getCurrentStock());
        }
        ingredientCombo.setItems(FXCollections.observableArrayList(ingredients));
        ingredientCombo.setPromptText("Selectați ingredient");
        
        // Setăm cum să afișăm ingredientele în ComboBox
        ingredientCombo.setConverter(new javafx.util.StringConverter<Ingredient>() {
            @Override
            public String toString(Ingredient ingredient) {
                return ingredient != null ? ingredient.getName() : "";
            }
            
            @Override
            public Ingredient fromString(String string) {
                // Căutăm ingredientul după nume
                return ingredientService.getAllIngredients().stream()
                    .filter(i -> i.getName().equals(string))
                    .findFirst()
                    .orElse(null);
            }
        });
        
        TextField quantityField = new TextField();
        quantityField.setPromptText("Cantitate necesară");
        
        grid.add(new Label("Ingredient:"), 0, 0);
        grid.add(ingredientCombo, 1, 0);
        grid.add(new Label("Cantitate:"), 0, 1);
        grid.add(quantityField, 1, 1);
        
        dialog.getDialogPane().setContent(grid);
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == okButtonType) {
                Ingredient ingredient = ingredientCombo.getValue();
                String quantity = quantityField.getText().trim();
                
                if (ingredient == null) {
                    showError("Selectați un ingredient!");
                    return null;
                }
                
                if (quantity.isEmpty()) {
                    showError("Introduceți cantitatea!");
                    return null;
                }
                
                return new RecipeIngredient(ingredient.getId(), ingredient.getName(), quantity, ingredient.getUnitOfMeasure().getDisplayName());
            }
            return null;
        });
        
        return dialog.showAndWait().orElse(null);
    }
    
    // Clasă internă pentru ingredient în rețetă
    private static class RecipeIngredient {
        private final Long ingredientId;
        private final String name;
        private final String quantity;
        private final String unit;
        
        public RecipeIngredient(Long ingredientId, String name, String quantity, String unit) {
            this.ingredientId = ingredientId;
            this.name = name;
            this.quantity = quantity;
            this.unit = unit;
        }
        
        public Long getIngredientId() { return ingredientId; }
        public String getName() { return name; }
        public String getQuantity() { return quantity; }
        public BigDecimal getQuantityValue() { 
            try {
                return new BigDecimal(quantity);
            } catch (NumberFormatException e) {
                return BigDecimal.ZERO;
            }
        }
        public String getUnit() { return unit; }
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
            
            // Reload production history from database
            refreshProductionHistory();
            
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
        List<Ingredient> ingredients = ingredientService.getAllIngredients();
        logger.debug("Ingredients available for recipe: {}", ingredients.size());
        for (Ingredient ing : ingredients) {
            logger.debug("Ingredient: {} (Stock: {})", ing.getName(), ing.getCurrentStock());
        }
        ingredientCombo.setItems(FXCollections.observableArrayList(ingredients));
        
        // Setăm cum să afișăm ingredientele în ComboBox
        ingredientCombo.setConverter(new javafx.util.StringConverter<Ingredient>() {
            @Override
            public String toString(Ingredient ingredient) {
                return ingredient != null ? ingredient.getName() : "";
            }
            
            @Override
            public Ingredient fromString(String string) {
                // Căutăm ingredientul după nume
                return ingredientService.getAllIngredients().stream()
                    .filter(i -> i.getName().equals(string))
                    .findFirst()
                    .orElse(null);
            }
        });
        
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
    
    @FXML
    public void exportProductionReportPdf() {
        try {
            // Get selected production record from history table
            ProductionRecord selectedRecord = productionHistoryTable.getSelectionModel().getSelectedItem();
            
            if (selectedRecord == null) {
                showError("Vă rugăm să selectați un raport de producție din istoric.");
                return;
            }
            
            ProductionReport selectedReport = selectedRecord.getReport();
            if (selectedReport == null) {
                showError("Raportul de producție selectat nu are date complete.");
                return;
            }
            
            // Create file chooser
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Salvează Raport de Producție PDF");
            
            // Set default filename
            String productName = selectedReport.getProduct() != null ? selectedReport.getProduct().getName() : "Produs";
            String date = selectedReport.getProductionDate() != null 
                ? selectedReport.getProductionDate().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                : LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            fileChooser.setInitialFileName("Raport_Productie_" + productName + "_" + date + ".pdf");
            
            // Set extension filter
            fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF Files", "*.pdf")
            );
            
            // Show save dialog
            File file = fileChooser.showSaveDialog(productionHistoryTable.getScene().getWindow());
            
            if (file != null) {
                // Generate PDF
                pdfService.generateProductionReportPdf(selectedReport, file.getAbsolutePath());
                
                showSuccessMessage("Raport exportat cu succes în: " + file.getAbsolutePath());
                logger.info("Production report exported to PDF: {}", file.getAbsolutePath());
            }
            
        } catch (Exception e) {
            logger.error("Error exporting production report to PDF", e);
            showError("Eroare la exportarea raportului: " + e.getMessage());
        }
    }
    
    @FXML
    public void editProductionReport() {
        try {
            // Get selected production record from history table
            ProductionRecord selectedRecord = productionHistoryTable.getSelectionModel().getSelectedItem();
            
            if (selectedRecord == null) {
                showError("Vă rugăm să selectați un raport de producție din istoric.");
                return;
            }
            
            ProductionReport selectedReport = selectedRecord.getReport();
            if (selectedReport == null) {
                showError("Raportul de producție selectat nu are date complete.");
                return;
            }
            
            // Create editable dialog
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Editare Raport de Producție");
            dialog.setHeaderText("Editare Raport: " + selectedReport.getProduct().getName());
            
            // Create form
            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));
            
            // Product name (read-only)
            TextField productField = new TextField(selectedReport.getProduct().getName());
            productField.setEditable(false);
            
            // Quantity produced
            TextField quantityField = new TextField(selectedReport.getQuantityProduced().toString());
            
            // Production date/time
            DatePicker datePicker = new DatePicker(
                selectedReport.getProductionDate() != null 
                    ? selectedReport.getProductionDate().toLocalDate() 
                    : LocalDate.now()
            );
            
            // Time fields
            Spinner<Integer> hourSpinner = new Spinner<>(0, 23, 
                selectedReport.getProductionDate() != null 
                    ? selectedReport.getProductionDate().getHour() 
                    : LocalDateTime.now().getHour()
            );
            hourSpinner.setEditable(true);
            
            Spinner<Integer> minuteSpinner = new Spinner<>(0, 59, 
                selectedReport.getProductionDate() != null 
                    ? selectedReport.getProductionDate().getMinute() 
                    : LocalDateTime.now().getMinute()
            );
            minuteSpinner.setEditable(true);
            
            // Status
            ComboBox<ProductionReport.ProductionStatus> statusCombo = new ComboBox<>();
            statusCombo.getItems().addAll(ProductionReport.ProductionStatus.values());
            statusCombo.setValue(selectedReport.getStatus());
            
            // Notes
            TextArea notesArea = new TextArea(
                selectedReport.getNotes() != null ? selectedReport.getNotes() : ""
            );
            notesArea.setPrefRowCount(3);
            
            // Add fields to grid
            int row = 0;
            grid.add(new Label("Produs:"), 0, row);
            grid.add(productField, 1, row++);
            
            grid.add(new Label("Cantitate Produsă:"), 0, row);
            grid.add(quantityField, 1, row++);
            
            grid.add(new Label("Data Producție:"), 0, row);
            grid.add(datePicker, 1, row++);
            
            grid.add(new Label("Ora:"), 0, row);
            javafx.scene.layout.HBox timeBox = new javafx.scene.layout.HBox(5, hourSpinner, new Label(":"), minuteSpinner);
            grid.add(timeBox, 1, row++);
            
            grid.add(new Label("Status:"), 0, row);
            grid.add(statusCombo, 1, row++);
            
            grid.add(new Label("Observații:"), 0, row);
            grid.add(notesArea, 1, row++);
            
            dialog.getDialogPane().setContent(grid);
            
            // Add buttons
            dialog.getDialogPane().getButtonTypes().addAll(
                ButtonType.OK,
                ButtonType.CANCEL
            );
            
            // Process result
            dialog.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    try {
                        // Validate quantity
                        BigDecimal newQuantity = new BigDecimal(quantityField.getText());
                        if (newQuantity.compareTo(BigDecimal.ZERO) <= 0) {
                            showError("Cantitatea trebuie să fie mai mare decât 0");
                            return;
                        }
                        
                        // Update report object
                        selectedReport.setQuantityProduced(newQuantity);
                        selectedReport.setProductionDate(
                            datePicker.getValue().atTime(hourSpinner.getValue(), minuteSpinner.getValue())
                        );
                        selectedReport.setStatus(statusCombo.getValue());
                        selectedReport.setNotes(notesArea.getText());
                        
                        // Save to database
                        productionService.saveProductionReport(selectedReport);
                        
                        // Refresh table
                        refreshProductionHistory();
                        
                        showSuccessMessage("Raport de producție actualizat cu succes!");
                        logger.info("Production report updated: {}", selectedReport.getId());
                        
                    } catch (NumberFormatException e) {
                        showError("Cantitatea trebuie să fie un număr valid");
                    } catch (Exception e) {
                        logger.error("Error updating production report", e);
                        showError("Eroare la salvarea raportului: " + e.getMessage());
                    }
                }
            });
            
        } catch (Exception e) {
            logger.error("Error editing production report", e);
            showError("Eroare la editarea raportului: " + e.getMessage());
        }
    }
    
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Eroare");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.show();
    }
}
