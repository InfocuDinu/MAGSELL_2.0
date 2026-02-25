package com.bakerymanager.controller;

import com.bakerymanager.entity.Product;
import com.bakerymanager.service.ProductService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller for VAT Rate Management UI
 * Allows setting VAT rates (0%, 11%, 21%) for products
 */
@Controller
public class VATManagementController {
    
    private static final Logger logger = LoggerFactory.getLogger(VATManagementController.class);
    
    private final ProductService productService;
    
    public VATManagementController(ProductService productService) {
        this.productService = productService;
    }
    
    // UI Components
    @FXML
    private TableView<Product> productsTable;
    
    @FXML
    private TableColumn<Product, Long> idColumn;
    
    @FXML
    private TableColumn<Product, String> nameColumn;
    
    @FXML
    private TableColumn<Product, BigDecimal> priceLColumn;
    
    @FXML
    private TableColumn<Product, BigDecimal> vatRateColumn;
    
    @FXML
    private TableColumn<Product, String> categoryColumn;
    
    @FXML
    private TableColumn<Product, Void> actionColumn;
    
    @FXML
    private TextField searchField;
    
    @FXML
    private Label productIdLabel;
    
    @FXML
    private TextField productNameField;
    
    @FXML
    private TextField productPriceField;
    
    @FXML
    private ComboBox<String> productCategoryCombo;
    
    @FXML
    private ComboBox<String> vatRateCombo;
    
    @FXML
    private Label vatDescriptionLabel;
    
    @FXML
    private Label totalProductsLabel;
    
    @FXML
    private Label zeroRateCountLabel;
    
    @FXML
    private Label elevenRateCountLabel;
    
    @FXML
    private Label twentyOneRateCountLabel;
    
    private ObservableList<Product> productList = FXCollections.observableArrayList();
    private Product selectedProduct = null;
    
    private static final String VAT_0_PERCENT = "0% - Taxa Zero (Produse alimentare)";
    private static final String VAT_11_PERCENT = "11% - Cota Redusă (Produse special ambalate)";
    private static final String VAT_21_PERCENT = "21% - Cota Standard (Servicii și alte)";
    
    @FXML
    public void initialize() {
        setupTableColumns();
        setupComboBoxes();
        setupEventListeners();
        loadProducts();
        logger.info("VAT Management controller initialized");
    }
    
    private void setupTableColumns() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        priceLColumn.setCellValueFactory(new PropertyValueFactory<>("salePrice"));
        vatRateColumn.setCellValueFactory(new PropertyValueFactory<>("vatRate"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        
        // Setup action column with Edit/Delete buttons
        setupActionColumn();
        
        // Add double-click to edit
        productsTable.setRowFactory(tv -> {
            TableRow<Product> row = new TableRow<Product>() {
                @Override
                protected void updateItem(Product item, boolean empty) {
                    super.updateItem(item, empty);
                }
            };
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    Product product = row.getItem();
                    editProduct(product);
                }
            });
            return row;
        });
    }
    
    private void setupActionColumn() {
        actionColumn.setCellFactory(param -> new TableCell<Product, Void>() {
            private final Button editBtn = new Button("✏ Editare");
            private final Button deleteBtn = new Button("🗑 Ștergere");
            
            {
                editBtn.setStyle("-fx-font-size: 10; -fx-padding: 5;");
                deleteBtn.setStyle("-fx-font-size: 10; -fx-padding: 5;");
                editBtn.setOnAction(event -> {
                    Product product = getTableView().getItems().get(getIndex());
                    editProduct(product);
                });
                deleteBtn.setOnAction(event -> {
                    selectedProduct = getTableView().getItems().get(getIndex());
                    deleteProduct();
                });
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(new HBox(5, editBtn, deleteBtn));
                }
            }
        });
    }
    
    private void setupComboBoxes() {
        // VAT Rate combo
        vatRateCombo.getItems().addAll(
            VAT_0_PERCENT,
            VAT_11_PERCENT,
            VAT_21_PERCENT
        );
        vatRateCombo.setValue(VAT_21_PERCENT);
        
        // Listen to VAT rate changes to update description
        vatRateCombo.setOnAction(e -> updateVATDescription());
        
        // Category combo
        productCategoryCombo.getItems().addAll(
            "Pâine și produse de bază",
            "Produse premium",
            "Servicii",
            "Alte"
        );
    }
    
    private void setupEventListeners() {
        // Search field listener
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            filterProducts(newVal);
        });
    }
    
    @FXML
    public void loadProducts() {
        try {
            List<Product> products = productService.getAllProducts();
            productList = FXCollections.observableArrayList(products);
            productsTable.setItems(productList);
            updateStatistics();
            logger.info("Loaded {} products for VAT management", products.size());
        } catch (Exception e) {
            logger.error("Error loading products", e);
        }
    }
    
    @FXML
    public void addProduct() {
        clearForm();
        productIdLabel.setText("Nou");
        selectedProduct = null;
    }
    
    private void editProduct(Product product) {
        selectedProduct = product;
        productIdLabel.setText(String.valueOf(product.getId()));
        productNameField.setText(product.getName());
        productPriceField.setText(product.getSalePrice().toString());
        productCategoryCombo.setValue(product.getCategory() != null ? product.getCategory() : "Alte");
        
        // Set VAT rate
        BigDecimal vatRate = product.getVatRate();
        if (vatRate != null) {
            if (vatRate.compareTo(BigDecimal.ZERO) == 0) {
                vatRateCombo.setValue(VAT_0_PERCENT);
            } else if (vatRate.compareTo(new BigDecimal("11")) == 0) {
                vatRateCombo.setValue(VAT_11_PERCENT);
            } else {
                vatRateCombo.setValue(VAT_21_PERCENT);
            }
        }
        
        updateVATDescription();
    }
    
    @FXML
    public void saveProduct() {
        try {
            // Validate inputs
            if (productNameField.getText().trim().isEmpty()) {
                logger.warn("Attempted to save product with empty name");
                return;
            }
            
            if (productPriceField.getText().trim().isEmpty()) {
                logger.warn("Attempted to save product with empty price");
                return;
            }
            
            // Create or update product
            Product product = selectedProduct != null ? selectedProduct : new Product();
            product.setName(productNameField.getText());
            product.setSalePrice(new BigDecimal(productPriceField.getText()));
            product.setCategory(productCategoryCombo.getValue());
            
            // Set VAT rate based on combo selection
            String vatSelection = vatRateCombo.getValue();
            if (vatSelection.startsWith("0%")) {
                product.setVatRate(new BigDecimal("0"));
            } else if (vatSelection.startsWith("11%")) {
                product.setVatRate(new BigDecimal("11"));
            } else {
                product.setVatRate(new BigDecimal("21"));
            }
            
            // Save
            productService.saveProduct(product);
            logger.info("Product saved with VAT rate: {} %", product.getVatRate());
            
            loadProducts();
            clearForm();
        } catch (NumberFormatException e) {
            logger.error("Invalid price format", e);
        } catch (Exception e) {
            logger.error("Error saving product", e);
        }
    }
    
    @FXML
    public void deleteProduct() {
        if (selectedProduct == null) {
            logger.warn("Attempted to delete with no product selected");
            return;
        }
        
        try {
            productService.deleteProduct(selectedProduct.getId());
            logger.info("Product deleted: {}", selectedProduct.getName());
            loadProducts();
            clearForm();
        } catch (Exception e) {
            logger.error("Error deleting product", e);
        }
    }
    
    @FXML
    public void cancelEdit() {
        clearForm();
    }
    
    private void clearForm() {
        productIdLabel.setText("---");
        productNameField.clear();
        productPriceField.clear();
        productCategoryCombo.setValue("Alte");
        vatRateCombo.setValue(VAT_21_PERCENT);
        selectedProduct = null;
        updateVATDescription();
    }
    
    private void filterProducts(String searchText) {
        if (searchText == null || searchText.trim().isEmpty()) {
            productsTable.setItems(productList);
        } else {
            String lowerSearch = searchText.toLowerCase();
            ObservableList<Product> filtered = productList.stream()
                .filter(p -> p.getName().toLowerCase().contains(lowerSearch) ||
                           p.getCategory() != null && p.getCategory().toLowerCase().contains(lowerSearch))
                .collect(Collectors.toCollection(FXCollections::observableArrayList));
            productsTable.setItems(filtered);
        }
    }
    
    private void updateVATDescription() {
        String selected = vatRateCombo.getValue();
        if (selected != null) {
            if (selected.startsWith("0%")) {
                vatDescriptionLabel.setText("Taxa Zero: Produse alimentare fără ambalaj special (pâine, cornuri). Nu se percepe TVA.");
            } else if (selected.startsWith("11%")) {
                vatDescriptionLabel.setText("Cota Redusă: Produse alimentare cu ambalaj special sau decorativ. TVA = 11%.");
            } else {
                vatDescriptionLabel.setText("Cota Standard: Servicii, mâncare gata preparată, alte produse taxabile. TVA = 21%.");
            }
        }
    }
    
    private void updateStatistics() {
        long total = productList.size();
        long zeroRate = productList.stream()
            .filter(p -> p.getVatRate() != null && p.getVatRate().compareTo(BigDecimal.ZERO) == 0)
            .count();
        long elevenRate = productList.stream()
            .filter(p -> p.getVatRate() != null && p.getVatRate().compareTo(new BigDecimal("11")) == 0)
            .count();
        long twentyOneRate = productList.stream()
            .filter(p -> p.getVatRate() != null && p.getVatRate().compareTo(new BigDecimal("21")) == 0)
            .count();
        
        totalProductsLabel.setText("Total Produse: " + total);
        zeroRateCountLabel.setText("0% TVA: " + zeroRate);
        elevenRateCountLabel.setText("11% TVA: " + elevenRate);
        twentyOneRateCountLabel.setText("21% TVA: " + twentyOneRate);
    }
}
