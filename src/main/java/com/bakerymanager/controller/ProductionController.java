package com.bakerymanager.controller;

import com.bakerymanager.entity.Product;
import com.bakerymanager.entity.ProductionReport;
import com.bakerymanager.entity.RecipeItem;
import com.bakerymanager.entity.Ingredient;
import com.bakerymanager.entity.ProductionOrder;
import com.bakerymanager.entity.ProductionOrderLine;
import com.bakerymanager.smartbill.production.api.ProductionFacade;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
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
    
    private final ProductionFacade productionFacade;
    
    public ProductionController(ProductionFacade productionFacade) {
        this.productionFacade = productionFacade;
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
    private TableView<ProductionOrder> productionOrdersTable;

    @FXML
    private TableColumn<ProductionOrder, String> orderNumberColumn;

    @FXML
    private TableColumn<ProductionOrder, String> orderDateColumn;

    @FXML
    private TableColumn<ProductionOrder, String> orderStatusColumn;

    @FXML
    private TableView<ProductionOrderLine> productionOrderLinesTable;

    @FXML
    private TableColumn<ProductionOrderLine, String> orderLineProductColumn;

    @FXML
    private TableColumn<ProductionOrderLine, BigDecimal> orderLinePlannedQtyColumn;

    @FXML
    private TableColumn<ProductionOrderLine, BigDecimal> orderLineActualQtyColumn;

    @FXML
    private TableColumn<ProductionOrderLine, String> orderLineUnitColumn;
    
    @FXML
    private Label productionInfoLabel;
    
    private ObservableList<RecipeItem> recipeItems = FXCollections.observableArrayList();
    private ObservableList<ProductionRecord> productionHistory = FXCollections.observableArrayList();
    private ObservableList<ProductionOrder> productionOrders = FXCollections.observableArrayList();
    private ObservableList<ProductionOrderLine> productionOrderLines = FXCollections.observableArrayList();
    private Product selectedProduct;
    private ProductionOrder selectedProductionOrder;
    
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
        setupProductionOrdersTable();
        setupProductionOrderLinesTable();
        loadProducts();
        loadProductionHistory();
        loadProductionOrders();
        logger.info("Production controller initialized");
    }
    
    private void setupProductComboBox() {
        productComboBox.setItems(FXCollections.observableArrayList(productionFacade.getActiveProducts()));
        
        // Setăm cum să afișăm produsele în ComboBox
        productComboBox.setConverter(new javafx.util.StringConverter<Product>() {
            @Override
            public String toString(Product product) {
                return product != null ? product.getName() : "";
            }
            
            @Override
            public Product fromString(String string) {
                // Căutăm produsul după nume
                return productionFacade.getActiveProducts().stream()
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
                if (item != null && item.getComponentType() == RecipeItem.ComponentType.PRODUCT && item.getSourceProduct() != null) {
                    return new javafx.beans.property.SimpleStringProperty("[Semifabricat] " + item.getSourceProduct().getName());
                }
                if (item != null && item.getIngredient() != null) {
                    return new javafx.beans.property.SimpleStringProperty(item.getIngredient().getName());
                } else if (item != null && item.getIngredientId() != null) {
                    // Încercăm să încărcăm ingredientul după ID dacă e lazy loaded
                    Ingredient ingredient = productionFacade.getIngredientById(item.getIngredientId()).orElse(null);
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
                if (item != null && item.getComponentType() == RecipeItem.ComponentType.PRODUCT) {
                    String unit = item.getUnit();
                    return new javafx.beans.property.SimpleStringProperty(unit != null && !unit.isBlank() ? unit : "BUC");
                }
                if (item != null && item.getIngredient() != null) {
                    return new javafx.beans.property.SimpleStringProperty(item.getIngredient().getUnitOfMeasure().getDisplayName());
                } else if (item != null && item.getIngredientId() != null) {
                    Ingredient ingredient = productionFacade.getIngredientById(item.getIngredientId()).orElse(null);
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
                if (item != null && item.getComponentType() == RecipeItem.ComponentType.PRODUCT && item.getSourceProduct() != null) {
                    return new javafx.beans.property.SimpleObjectProperty<>(item.getSourceProduct().getPhysicalStock());
                }
                if (item != null && item.getIngredient() != null) {
                    return new javafx.beans.property.SimpleObjectProperty<>(item.getIngredient().getCurrentStock());
                } else if (item != null && item.getIngredientId() != null) {
                    Ingredient ingredient = productionFacade.getIngredientById(item.getIngredientId()).orElse(null);
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

    private void setupProductionOrdersTable() {
        if (productionOrdersTable == null) {
            return;
        }

        orderNumberColumn.setCellValueFactory(new PropertyValueFactory<>("orderNumber"));
        orderDateColumn.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(
            param.getValue().getPlannedDate() != null
                ? param.getValue().getPlannedDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                : ""
        ));
        orderStatusColumn.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(
            param.getValue().getStatus() != null ? param.getValue().getStatus().getDisplayName() : ""
        ));

        productionOrdersTable.setItems(productionOrders);
        productionOrdersTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            selectedProductionOrder = newVal;
            loadProductionOrderLines();
        });
    }

    private void setupProductionOrderLinesTable() {
        if (productionOrderLinesTable == null) {
            return;
        }

        orderLineProductColumn.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(
            param.getValue().getProduct() != null ? param.getValue().getProduct().getName() : ""
        ));
        orderLinePlannedQtyColumn.setCellValueFactory(new PropertyValueFactory<>("plannedQuantity"));
        orderLineActualQtyColumn.setCellValueFactory(new PropertyValueFactory<>("actualQuantity"));
        orderLineUnitColumn.setCellValueFactory(new PropertyValueFactory<>("unit"));

        productionOrderLinesTable.setItems(productionOrderLines);
    }
    
    private void loadProducts() {
        List<Product> products = productionFacade.getActiveProducts();
        logger.debug("Available products: {}", products.size());
        for (Product p : products) {
            logger.debug("Product: {} (Stock: {}, Price: {})", p.getName(), p.getPhysicalStock(), p.getSalePrice());
        }
        productComboBox.setItems(FXCollections.observableArrayList(products));
    }
    
    private void loadRecipe() {
        if (selectedProduct != null) {
            List<RecipeItem> items = productionFacade.getRecipeByProduct(selectedProduct);
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
            List<com.bakerymanager.entity.ProductionReport> reports = productionFacade.getAllProductionReports();
            
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
    public void refreshProductionOrders() {
        loadProductionOrders();
    }
    
    @FXML
    public void refreshProducts() {
        loadProducts();
        productionStatusLabel.setText("Produse reîncărcate");
    }

    @FXML
    public void editTechnologicalSheet() {
        if (selectedProduct == null) {
            showError("Selectați un produs mai întâi!");
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Fișă tehnologică");
        dialog.setHeaderText("Editare fișă tehnologică: " + selectedProduct.getName());
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField yieldField = new TextField((selectedProduct.getYieldPercent() != null ? selectedProduct.getYieldPercent() : BigDecimal.valueOf(100)).toPlainString());
        yieldField.setPromptText("Randament % (0-100)");

        TextField prepTimeField = new TextField(String.valueOf(selectedProduct.getPrepTimeMinutes() != null ? selectedProduct.getPrepTimeMinutes() : 0));
        prepTimeField.setPromptText("Minute preparare");

        TextField bakingTimeField = new TextField(String.valueOf(selectedProduct.getBakingTimeMinutes() != null ? selectedProduct.getBakingTimeMinutes() : 0));
        bakingTimeField.setPromptText("Minute coacere");

        TextField techLossField = new TextField((selectedProduct.getTechnologicalLossPercent() != null ? selectedProduct.getTechnologicalLossPercent() : BigDecimal.ZERO).toPlainString());
        techLossField.setPromptText("Pierderi tehnologice %");

        grid.add(new Label("Randament (%):"), 0, 0);
        grid.add(yieldField, 1, 0);
        grid.add(new Label("Timp preparare (min):"), 0, 1);
        grid.add(prepTimeField, 1, 1);
        grid.add(new Label("Timp coacere (min):"), 0, 2);
        grid.add(bakingTimeField, 1, 2);
        grid.add(new Label("Pierderi tehnologice (%):"), 0, 3);
        grid.add(techLossField, 1, 3);

        dialog.getDialogPane().setContent(grid);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    BigDecimal yield = new BigDecimal(yieldField.getText().trim());
                    Integer prepTime = Integer.parseInt(prepTimeField.getText().trim());
                    Integer bakingTime = Integer.parseInt(bakingTimeField.getText().trim());
                    BigDecimal techLoss = new BigDecimal(techLossField.getText().trim());

                    validateTechnologicalSheetValues(yield, prepTime, bakingTime, techLoss);

                    selectedProduct.setYieldPercent(yield);
                    selectedProduct.setPrepTimeMinutes(prepTime);
                    selectedProduct.setBakingTimeMinutes(bakingTime);
                    selectedProduct.setTechnologicalLossPercent(techLoss);
                    selectedProduct = productionFacade.saveProduct(selectedProduct);

                    loadProducts();
                    productComboBox.setValue(selectedProduct);
                    updateProductionInfo();
                    showSuccessMessage("Fișa tehnologică a fost actualizată cu succes!");
                } catch (NumberFormatException e) {
                    showError("Valorile introduse trebuie să fie numere valide!");
                } catch (IllegalArgumentException e) {
                    showError(e.getMessage());
                } catch (Exception e) {
                    showError("Eroare la salvarea fișei tehnologice: " + e.getMessage());
                }
            }
        });
    }
    
    @FXML
    public void createNewProduct() {
        Dialog<Product> dialog = createProductDialog();
        dialog.showAndWait().ifPresent(product -> {
            productionFacade.saveProduct(product);
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
        TextField yieldField = new TextField("100");
        yieldField.setPromptText("Randament % (0-100)");
        TextField prepTimeField = new TextField("0");
        prepTimeField.setPromptText("Timp preparare (minute)");
        TextField bakingTimeField = new TextField("0");
        bakingTimeField.setPromptText("Timp coacere (minute)");
        TextField techLossField = new TextField("0");
        techLossField.setPromptText("Pierderi tehnologice %");
        
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
        
        ingredientsTable.getColumns().add(nameCol);
        ingredientsTable.getColumns().add(quantityCol);
        ingredientsTable.getColumns().add(unitCol);
        
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
        grid.add(new Label("Randament (%):"), 0, 3);
        grid.add(yieldField, 1, 3);
        grid.add(new Label("Timp preparare (min):"), 0, 4);
        grid.add(prepTimeField, 1, 4);
        grid.add(new Label("Timp coacere (min):"), 0, 5);
        grid.add(bakingTimeField, 1, 5);
        grid.add(new Label("Pierderi tehnologice (%):"), 0, 6);
        grid.add(techLossField, 1, 6);
        grid.add(new Label("Ingrediente:"), 0, 7);
        grid.add(ingredientsTable, 1, 7);
        grid.add(addIngredientBtn, 1, 8);
        
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
                    BigDecimal yield = new BigDecimal(yieldField.getText().trim());
                    Integer prepTime = Integer.parseInt(prepTimeField.getText().trim());
                    Integer bakingTime = Integer.parseInt(bakingTimeField.getText().trim());
                    BigDecimal techLoss = new BigDecimal(techLossField.getText().trim());

                    validateTechnologicalSheetValues(yield, prepTime, bakingTime, techLoss);
                    
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
                    product.setYieldPercent(yield);
                    product.setPrepTimeMinutes(prepTime);
                    product.setBakingTimeMinutes(bakingTime);
                    product.setTechnologicalLossPercent(techLoss);
                    
                    // Salvăm produsul mai întâi pentru a obține ID
                    Product savedProduct = productionFacade.saveProduct(product);
                    
                    // Creăm rețeta
                    for (RecipeIngredient ri : ingredients) {
                        if (ri.isProductComponent()) {
                            productionFacade.addRecipeProductItem(savedProduct.getId(), ri.getSourceProductId(), ri.getQuantityValue());
                        } else {
                            productionFacade.addRecipeItem(savedProduct.getId(), ri.getIngredientId(), ri.getQuantityValue());
                        }
                    }
                    
                    return savedProduct;
                } catch (NumberFormatException e) {
                    showError("Prețul, stocul și fișa tehnologică trebuie să fie valori numerice valide!");
                    return null;
                } catch (IllegalArgumentException e) {
                    showError(e.getMessage());
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
        dialog.setTitle("Componentă Rețetă");
        dialog.setHeaderText("Adăugați ingredient sau semifabricat în rețetă");
        
        ButtonType okButtonType = new ButtonType("Adaugă", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okButtonType, ButtonType.CANCEL);
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        ComboBox<String> componentTypeCombo = new ComboBox<>();
        componentTypeCombo.setItems(FXCollections.observableArrayList("Ingredient", "Semifabricat"));
        componentTypeCombo.setValue("Ingredient");
        
        ComboBox<Ingredient> ingredientCombo = new ComboBox<>();
        List<Ingredient> ingredients = productionFacade.getAllIngredients();
        logger.debug("Available ingredients: {}", ingredients.size());
        for (Ingredient ing : ingredients) {
            logger.debug("Ingredient: {} (Stock: {})", ing.getName(), ing.getCurrentStock());
        }
        ingredientCombo.setItems(FXCollections.observableArrayList(ingredients));
        ingredientCombo.setPromptText("Selectați ingredient");

        ComboBox<Product> productCombo = new ComboBox<>();
        productCombo.setItems(FXCollections.observableArrayList(productionFacade.getActiveProducts()));
        productCombo.setPromptText("Selectați semifabricat");
        productCombo.setVisible(false);
        productCombo.setManaged(false);

        productCombo.setConverter(new javafx.util.StringConverter<Product>() {
            @Override
            public String toString(Product product) {
                return product != null ? product.getName() : "";
            }

            @Override
            public Product fromString(String string) {
                return productionFacade.getActiveProducts().stream()
                    .filter(p -> p.getName().equals(string))
                    .findFirst()
                    .orElse(null);
            }
        });

        componentTypeCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            boolean ingredientSelected = "Ingredient".equals(newVal);
            ingredientCombo.setVisible(ingredientSelected);
            ingredientCombo.setManaged(ingredientSelected);
            productCombo.setVisible(!ingredientSelected);
            productCombo.setManaged(!ingredientSelected);
        });
        
        // Setăm cum să afișăm ingredientele în ComboBox
        ingredientCombo.setConverter(new javafx.util.StringConverter<Ingredient>() {
            @Override
            public String toString(Ingredient ingredient) {
                return ingredient != null ? ingredient.getName() : "";
            }
            
            @Override
            public Ingredient fromString(String string) {
                // Căutăm ingredientul după nume
                return productionFacade.getAllIngredients().stream()
                    .filter(i -> i.getName().equals(string))
                    .findFirst()
                    .orElse(null);
            }
        });
        
        TextField quantityField = new TextField();
        quantityField.setPromptText("Cantitate necesară");
        
        grid.add(new Label("Tip componentă:"), 0, 0);
        grid.add(componentTypeCombo, 1, 0);
        grid.add(new Label("Ingredient:"), 0, 1);
        grid.add(ingredientCombo, 1, 1);
        grid.add(new Label("Semifabricat:"), 0, 2);
        grid.add(productCombo, 1, 2);
        grid.add(new Label("Cantitate:"), 0, 3);
        grid.add(quantityField, 1, 3);
        
        dialog.getDialogPane().setContent(grid);
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == okButtonType) {
                String quantity = quantityField.getText().trim();
                
                if (quantity.isEmpty()) {
                    showError("Introduceți cantitatea!");
                    return null;
                }

                if ("Semifabricat".equals(componentTypeCombo.getValue())) {
                    Product sourceProduct = productCombo.getValue();
                    if (sourceProduct == null) {
                        showError("Selectați un semifabricat!");
                        return null;
                    }
                    return RecipeIngredient.forProduct(sourceProduct.getId(), sourceProduct.getName(), quantity);
                }

                Ingredient ingredient = ingredientCombo.getValue();
                if (ingredient == null) {
                    showError("Selectați un ingredient!");
                    return null;
                }

                return RecipeIngredient.forIngredient(ingredient.getId(), ingredient.getName(), quantity, ingredient.getUnitOfMeasure().getDisplayName());
            }
            return null;
        });
        
        return dialog.showAndWait().orElse(null);
    }
    
    // Clasă internă pentru ingredient în rețetă
    private static class RecipeIngredient {
        private final RecipeItem.ComponentType componentType;
        private final Long ingredientId;
        private final Long sourceProductId;
        private final String name;
        private final String quantity;
        private final String unit;
        
        private RecipeIngredient(RecipeItem.ComponentType componentType, Long ingredientId, Long sourceProductId, String name, String quantity, String unit) {
            this.componentType = componentType;
            this.ingredientId = ingredientId;
            this.sourceProductId = sourceProductId;
            this.name = name;
            this.quantity = quantity;
            this.unit = unit;
        }

        public static RecipeIngredient forIngredient(Long ingredientId, String name, String quantity, String unit) {
            return new RecipeIngredient(RecipeItem.ComponentType.INGREDIENT, ingredientId, null, name, quantity, unit);
        }

        public static RecipeIngredient forProduct(Long sourceProductId, String name, String quantity) {
            return new RecipeIngredient(RecipeItem.ComponentType.PRODUCT, null, sourceProductId, name, quantity, "BUC");
        }
        
        public Long getIngredientId() { return ingredientId; }
        public Long getSourceProductId() { return sourceProductId; }
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
        public boolean isProductComponent() { return componentType == RecipeItem.ComponentType.PRODUCT; }
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
            
            if (!productionFacade.canProduce(selectedProduct.getId(), quantity)) {
                showError("Stoc insuficient pentru producție!");
                stockStatusLabel.setText("❌ Stoc insuficient");
                productionStatusLabel.setText("Eroare producție");
                return;
            }
            
            productionFacade.executeProduction(selectedProduct.getId(), quantity);
            
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
                productionFacade.calculateRequiredIngredients(selectedProduct.getId(), quantity);
            
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
            if (data.isProductComponent()) {
                productionFacade.addRecipeProductItem(selectedProduct.getId(), data.sourceProductId, data.quantity);
            } else {
                productionFacade.addRecipeItem(selectedProduct.getId(), data.ingredientId, data.quantity);
            }
            loadRecipe();
            showSuccessMessage("Componentă adăugată la rețetă!");
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
        String componentName;
        if (item.getComponentType() == RecipeItem.ComponentType.PRODUCT) {
            componentName = item.getSourceProduct() != null ? item.getSourceProduct().getName() : "Semifabricat";
        } else {
            componentName = item.getIngredient() != null ? item.getIngredient().getName() : "Ingredient";
        }
        alert.setContentText("Componentă: " + componentName);
        
        if (alert.showAndWait().get() == ButtonType.OK) {
            productionFacade.removeRecipeItem(item.getId());
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
                " | Stoc curent: " + selectedProduct.getPhysicalStock() +
                " | Randament: " + (selectedProduct.getYieldPercent() != null ? selectedProduct.getYieldPercent() : BigDecimal.valueOf(100)) + "%" +
                " | Preparare: " + (selectedProduct.getPrepTimeMinutes() != null ? selectedProduct.getPrepTimeMinutes() : 0) + " min" +
                " | Coacere: " + (selectedProduct.getBakingTimeMinutes() != null ? selectedProduct.getBakingTimeMinutes() : 0) + " min" +
                " | Pierderi: " + (selectedProduct.getTechnologicalLossPercent() != null ? selectedProduct.getTechnologicalLossPercent() : BigDecimal.ZERO) + "%"
            );
        } else {
            productionInfoLabel.setText("Selectați un produs pentru a începe producția");
        }
    }

    private void validateTechnologicalSheetValues(BigDecimal yield, Integer prepTime, Integer bakingTime, BigDecimal techLoss) {
        if (yield.compareTo(BigDecimal.ZERO) <= 0 || yield.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("Randamentul trebuie să fie între 0 și 100.");
        }
        if (prepTime < 0 || bakingTime < 0) {
            throw new IllegalArgumentException("Timpul de preparare/coacere nu poate fi negativ.");
        }
        if (techLoss.compareTo(BigDecimal.ZERO) < 0 || techLoss.compareTo(BigDecimal.valueOf(100)) >= 0) {
            throw new IllegalArgumentException("Pierderile tehnologice trebuie să fie între 0 și 99.99.");
        }
        if (yield.subtract(techLoss).compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Randamentul minus pierderile tehnologice trebuie să fie mai mare decât 0.");
        }
    }
    
    private Dialog<RecipeItemData> createRecipeItemDialog() {
        Dialog<RecipeItemData> dialog = new Dialog<>();
        dialog.setTitle("Adaugă Componentă la Rețetă");
        dialog.setHeaderText(null);
        
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

        ComboBox<String> componentTypeCombo = new ComboBox<>();
        componentTypeCombo.setItems(FXCollections.observableArrayList("Ingredient", "Semifabricat"));
        componentTypeCombo.setValue("Ingredient");
        
        ComboBox<Ingredient> ingredientCombo = new ComboBox<>();
        List<Ingredient> ingredients = productionFacade.getAllIngredients();
        logger.debug("Ingredients available for recipe: {}", ingredients.size());
        for (Ingredient ing : ingredients) {
            logger.debug("Ingredient: {} (Stock: {})", ing.getName(), ing.getCurrentStock());
        }
        ingredientCombo.setItems(FXCollections.observableArrayList(ingredients));

        ComboBox<Product> productCombo = new ComboBox<>();
        productCombo.setItems(FXCollections.observableArrayList(productionFacade.getActiveProducts()));
        productCombo.setVisible(false);
        productCombo.setManaged(false);

        productCombo.setConverter(new javafx.util.StringConverter<Product>() {
            @Override
            public String toString(Product product) {
                return product != null ? product.getName() : "";
            }

            @Override
            public Product fromString(String string) {
                return productionFacade.getActiveProducts().stream()
                    .filter(p -> p.getName().equals(string))
                    .findFirst()
                    .orElse(null);
            }
        });
        
        // Setăm cum să afișăm ingredientele în ComboBox
        ingredientCombo.setConverter(new javafx.util.StringConverter<Ingredient>() {
            @Override
            public String toString(Ingredient ingredient) {
                return ingredient != null ? ingredient.getName() : "";
            }
            
            @Override
            public Ingredient fromString(String string) {
                // Căutăm ingredientul după nume
                return productionFacade.getAllIngredients().stream()
                    .filter(i -> i.getName().equals(string))
                    .findFirst()
                    .orElse(null);
            }
        });
        
        TextField quantityField = new TextField();
        quantityField.setPromptText("Cantitate necesară per unitate de produs");

        componentTypeCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            boolean ingredientSelected = "Ingredient".equals(newVal);
            ingredientCombo.setVisible(ingredientSelected);
            ingredientCombo.setManaged(ingredientSelected);
            productCombo.setVisible(!ingredientSelected);
            productCombo.setManaged(!ingredientSelected);
        });
        
        grid.add(new Label("Tip componentă:"), 0, 0);
        grid.add(componentTypeCombo, 1, 0);
        grid.add(new Label("Ingredient:"), 0, 1);
        grid.add(ingredientCombo, 1, 1);
        grid.add(new Label("Semifabricat:"), 0, 2);
        grid.add(productCombo, 1, 2);
        grid.add(new Label("Cantitate:"), 0, 3);
        grid.add(quantityField, 1, 3);
        
        dialog.getDialogPane().setContent(grid);
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                try {
                    BigDecimal quantity = new BigDecimal(quantityField.getText().trim());
                    if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
                        showError("Cantitatea trebuie să fie mai mare decât 0!");
                        return null;
                    }

                    if ("Semifabricat".equals(componentTypeCombo.getValue())) {
                        Product sourceProduct = productCombo.getValue();
                        if (sourceProduct == null) {
                            showError("Selectați un semifabricat!");
                            return null;
                        }
                        if (selectedProduct != null && selectedProduct.getId().equals(sourceProduct.getId())) {
                            showError("Produsul nu se poate consuma pe sine în rețetă!");
                            return null;
                        }
                        return new RecipeItemData(null, sourceProduct.getId(), quantity);
                    }

                    Ingredient ingredient = ingredientCombo.getValue();
                    if (ingredient == null) {
                        showError("Selectați un ingredient!");
                        return null;
                    }
                    return new RecipeItemData(ingredient.getId(), null, quantity);
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
        Long sourceProductId;
        BigDecimal quantity;
        
        public RecipeItemData(Long ingredientId, Long sourceProductId, BigDecimal quantity) {
            this.ingredientId = ingredientId;
            this.sourceProductId = sourceProductId;
            this.quantity = quantity;
        }

        boolean isProductComponent() {
            return sourceProductId != null;
        }
    }
    
    private void showSuccessMessage(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Succes");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.show();
    }

    private void loadProductionOrders() {
        try {
            productionOrders.clear();
            List<ProductionOrder> orders = productionFacade.getProductionOrders(LocalDate.now().minusDays(7), LocalDate.now().plusDays(7));
            productionOrders.addAll(orders);
            if (!orders.isEmpty()) {
                productionOrdersTable.getSelectionModel().select(0);
            }
        } catch (Exception e) {
            logger.error("Error loading production orders", e);
            showError("Eroare la încărcarea ordinelor: " + e.getMessage());
        }
    }

    private void loadProductionOrderLines() {
        productionOrderLines.clear();
        if (selectedProductionOrder == null) {
            return;
        }

        try {
            ProductionOrder order = productionFacade.getProductionOrderWithLines(selectedProductionOrder.getId());
            if (order.getLines() != null) {
                productionOrderLines.addAll(order.getLines());
            }
        } catch (Exception e) {
            logger.error("Error loading production order lines", e);
            showError("Eroare la încărcarea liniilor ordinului: " + e.getMessage());
        }
    }

    @FXML
    public void createProductionOrder() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Ordin Producție Nou");
        dialog.setHeaderText("Creează ordin de producție");

        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        DatePicker plannedDatePicker = new DatePicker(LocalDate.now());
        TextArea notesArea = new TextArea();
        notesArea.setPrefRowCount(3);

        grid.add(new Label("Data planificată:"), 0, 0);
        grid.add(plannedDatePicker, 1, 0);
        grid.add(new Label("Observații:"), 0, 1);
        grid.add(notesArea, 1, 1);

        dialog.getDialogPane().setContent(grid);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    ProductionOrder order = productionFacade.createProductionOrder(plannedDatePicker.getValue(), notesArea.getText());
                    loadProductionOrders();
                    productionOrdersTable.getSelectionModel().select(order);
                    showSuccessMessage("Ordin creat: " + order.getOrderNumber());
                } catch (Exception e) {
                    showError("Eroare la creare ordin: " + e.getMessage());
                }
            }
        });
    }

    @FXML
    public void addProductionOrderLine() {
        if (selectedProductionOrder == null) {
            showError("Selectați un ordin de producție!");
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Adaugă Linie Ordin");
        dialog.setHeaderText("Adaugă produs în ordin");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        ComboBox<Product> productCombo = new ComboBox<>(FXCollections.observableArrayList(productionFacade.getActiveProducts()));
        productCombo.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(Product product) {
                return product != null ? product.getName() : "";
            }

            @Override
            public Product fromString(String string) {
                return productionFacade.getActiveProducts().stream()
                    .filter(p -> p.getName().equals(string))
                    .findFirst()
                    .orElse(null);
            }
        });

        TextField quantityField = new TextField();
        quantityField.setPromptText("Cantitate planificată");

        grid.add(new Label("Produs:"), 0, 0);
        grid.add(productCombo, 1, 0);
        grid.add(new Label("Cantitate:"), 0, 1);
        grid.add(quantityField, 1, 1);

        dialog.getDialogPane().setContent(grid);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    Product product = productCombo.getValue();
                    if (product == null) {
                        showError("Selectați un produs!");
                        return;
                    }
                    BigDecimal qty = new BigDecimal(quantityField.getText().trim());
                    productionFacade.addProductionOrderLine(selectedProductionOrder.getId(), product.getId(), qty);
                    loadProductionOrderLines();
                    showSuccessMessage("Linie adăugată în ordin");
                } catch (Exception e) {
                    showError("Eroare la adăugare linie: " + e.getMessage());
                }
            }
        });
    }

    @FXML
    public void startProductionOrder() {
        if (selectedProductionOrder == null) {
            showError("Selectați un ordin de producție!");
            return;
        }
        try {
            productionFacade.startProductionOrder(selectedProductionOrder.getId());
            loadProductionOrders();
            showSuccessMessage("Ordin pornit");
        } catch (Exception e) {
            showError("Eroare la pornire ordin: " + e.getMessage());
        }
    }

    @FXML
    public void completeProductionOrder() {
        if (selectedProductionOrder == null) {
            showError("Selectați un ordin de producție!");
            return;
        }
        try {
            productionFacade.completeProductionOrder(selectedProductionOrder.getId());
            loadProductionOrders();
            loadProductionHistory();
            showSuccessMessage("Ordin finalizat");
        } catch (Exception e) {
            showError("Eroare la finalizare ordin: " + e.getMessage());
        }
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
                productionFacade.exportProductionReportPdf(selectedReport, file.getAbsolutePath());
                
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
                        productionFacade.saveProductionReport(selectedReport);
                        
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
