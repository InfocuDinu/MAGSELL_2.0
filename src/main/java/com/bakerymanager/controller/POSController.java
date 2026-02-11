package com.bakerymanager.controller;

import com.bakerymanager.entity.Product;
import com.bakerymanager.service.ProductService;
import com.bakerymanager.service.SaleService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Font;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Controller
public class POSController {
    
    private static final Logger logger = LoggerFactory.getLogger(POSController.class);
    
    private final ProductService productService;
    private final SaleService saleService;
    
    public POSController(ProductService productService, SaleService saleService) {
        this.productService = productService;
        this.saleService = saleService;
    }
    
    @FXML
    private TextField searchProductField;
    
    @FXML
    private Label currentTimeLabel;
    
    @FXML
    private ComboBox<String> categoryComboBox;
    
    @FXML
    private TilePane productTilePane;
    
    @FXML
    private TableView<CartItem> cartTable;
    
    @FXML
    private TableColumn<CartItem, String> cartProductColumn;
    
    @FXML
    private TableColumn<CartItem, BigDecimal> cartQuantityColumn;
    
    @FXML
    private TableColumn<CartItem, BigDecimal> cartPriceColumn;
    
    @FXML
    private TableColumn<CartItem, BigDecimal> cartTotalColumn;
    
    @FXML
    private TableColumn<CartItem, Void> cartActionsColumn;
    
    @FXML
    private Label totalItemsLabel;
    
    @FXML
    private Label totalAmountLabel;
    
    @FXML
    private ComboBox<String> paymentMethodCombo;
    
    @FXML
    private TextField amountReceivedField;
    
    @FXML
    private Label changeLabel;
    
    @FXML
    private Label posStatusLabel;
    
    @FXML
    private Label dailySalesLabel;
    
    private ObservableList<CartItem> cartItems = FXCollections.observableArrayList();
    private List<Product> availableProducts;
    private BigDecimal dailySales = BigDecimal.ZERO;
    private BigDecimal cachedCartTotal = BigDecimal.ZERO;
    
    public static class CartItem {
        private Product product;
        private BigDecimal quantity;
        private BigDecimal unitPrice;
        
        public CartItem(Product product, BigDecimal quantity) {
            this.product = product;
            this.quantity = quantity;
            this.unitPrice = product.getSalePrice();
        }
        
        public BigDecimal getTotal() {
            return quantity.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);
        }
        
        public void addQuantity(BigDecimal additionalQuantity) {
            this.quantity = this.quantity.add(additionalQuantity);
        }
        
        public Product getProduct() { return product; }
        public BigDecimal getQuantity() { return quantity; }
        public BigDecimal getUnitPrice() { return unitPrice; }
        public BigDecimal getTotalValue() { return getTotal(); }
        public String getProductName() { return product.getName(); }
    }
    
    @FXML
    public void initialize() {
        setupPaymentMethods();
        setupCartTable();
        setupSearchField();
        loadProducts();
        startClock();
        updateCartSummary();
        logger.info("POS controller initialized");
    }
    
    private void setupPaymentMethods() {
        paymentMethodCombo.setItems(FXCollections.observableArrayList(
            "Numerar",
            "Card Bancar",
            "Tichete Mese",
            "Altele"
        ));
        paymentMethodCombo.setValue("Numerar");
    }
    
    private void setupCartTable() {
        cartProductColumn.setCellValueFactory(param -> 
            new javafx.beans.property.SimpleObjectProperty<>(param.getValue().getProductName()));
        
        cartQuantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        cartPriceColumn.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));
        cartTotalColumn.setCellValueFactory(new PropertyValueFactory<>("total"));
        
        setupCartActionsColumn();
        
        cartTable.setItems(cartItems);
    }
    
    private void setupCartActionsColumn() {
        cartActionsColumn.setCellFactory(param -> new TableCell<>() {
            private final Button removeButton = new Button("Șterge");
            
            {
                removeButton.getStyleClass().addAll("button", "danger");
                removeButton.setPrefWidth(60);
                removeButton.setOnAction(event -> {
                    CartItem item = getTableView().getItems().get(getIndex());
                    removeFromCart(item);
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
    
    private void setupSearchField() {
        searchProductField.textProperty().addListener((observable, oldValue, newValue) -> {
            filterProducts(newValue);
        });
    }
    
    private void startClock() {
        javafx.animation.Timeline timeline = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(
                javafx.util.Duration.seconds(1),
                event -> updateClock()
            )
        );
        timeline.setCycleCount(javafx.animation.Timeline.INDEFINITE);
        timeline.play();
    }
    
    private void updateClock() {
        currentTimeLabel.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
    }
    
    @FXML
    public void loadProducts() {
        availableProducts = productService.getAvailableProducts();
        displayProducts(availableProducts);
        posStatusLabel.setText("Produse încărcate: " + availableProducts.size());
    }
    
    private void displayProducts(List<Product> products) {
        productTilePane.getChildren().clear();
        
        for (Product product : products) {
            Button productButton = createProductButton(product);
            productTilePane.getChildren().add(productButton);
        }
    }
    
    private Button createProductButton(Product product) {
        Button button = new Button();
        button.getStyleClass().add("pos-product-button");
        button.setText(product.getName() + "\n" + product.getSalePrice() + " lei");
        button.setPrefSize(140, 80);
        button.setWrapText(true);
        button.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        button.setFont(Font.font(12));
        
        button.setOnAction(event -> addToCart(product));
        
        if (product.getPhysicalStock().compareTo(BigDecimal.ONE) < 0) {
            button.setDisable(true);
            button.setText(product.getName() + "\nStoc epuizat");
        }
        
        return button;
    }
    
    private void filterProducts(String searchText) {
        if (searchText == null || searchText.trim().isEmpty()) {
            displayProducts(availableProducts);
            return;
        }
        
        List<Product> filtered = availableProducts.stream()
            .filter(product -> product.getName().toLowerCase().contains(searchText.toLowerCase()))
            .toList();
        
        displayProducts(filtered);
    }
    
    @FXML
    public void addProductManually() {
        Dialog<ManualEntryResult> dialog = new Dialog<>();
        dialog.setTitle("Introduceți Produs Manual");
        dialog.setHeaderText("Căutați și selectați un produs pentru a-l adăuga în coș");
        
        // Buttons
        ButtonType addButtonType = new ButtonType("Adaugă în Coș", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);
        
        // Create form
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));
        
        // Search field with autocomplete
        TextField searchField = new TextField();
        searchField.setPromptText("Nume produs sau cod bare...");
        
        // Product selection
        ComboBox<Product> productCombo = new ComboBox<>();
        productCombo.setPromptText("Selectați produs");
        productCombo.setPrefWidth(250);
        productCombo.setItems(FXCollections.observableArrayList(availableProducts));
        
        // Custom display for combo box
        productCombo.setConverter(new javafx.util.StringConverter<Product>() {
            @Override
            public String toString(Product product) {
                if (product == null) return "";
                return product.getName() + " - " + product.getSalePrice() + " lei (Stoc: " + product.getPhysicalStock() + ")";
            }
            
            @Override
            public Product fromString(String string) {
                return null;
            }
        });
        
        // Quantity field
        TextField quantityField = new TextField("1");
        quantityField.setPromptText("Cantitate");
        
        // Stock info label
        Label stockInfoLabel = new Label("");
        stockInfoLabel.setStyle("-fx-text-fill: #666;");
        
        // Price info label
        Label priceInfoLabel = new Label("");
        priceInfoLabel.setStyle("-fx-font-weight: bold;");
        
        // Search functionality
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.trim().isEmpty()) {
                productCombo.setItems(FXCollections.observableArrayList(availableProducts));
            } else {
                String searchLower = newVal.toLowerCase();
                List<Product> filtered = availableProducts.stream()
                    .filter(p -> 
                        p.getName().toLowerCase().contains(searchLower) ||
                        (p.getBarcode() != null && p.getBarcode().contains(searchLower))
                    )
                    .toList();
                productCombo.setItems(FXCollections.observableArrayList(filtered));
                
                // Auto-select if only one match
                if (filtered.size() == 1) {
                    productCombo.setValue(filtered.get(0));
                }
            }
        });
        
        // Update info when product selected
        productCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                stockInfoLabel.setText("Stoc disponibil: " + newVal.getPhysicalStock() + " " + 
                                      (newVal.getPhysicalStock().compareTo(BigDecimal.ONE) <= 0 ? "(LIMITAT)" : ""));
                updatePriceInfo(priceInfoLabel, newVal, quantityField.getText());
                
                if (newVal.getPhysicalStock().compareTo(BigDecimal.ZERO) <= 0) {
                    stockInfoLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                } else if (newVal.getPhysicalStock().compareTo(BigDecimal.valueOf(5)) < 0) {
                    stockInfoLabel.setStyle("-fx-text-fill: orange; -fx-font-weight: bold;");
                } else {
                    stockInfoLabel.setStyle("-fx-text-fill: green;");
                }
            } else {
                stockInfoLabel.setText("");
                priceInfoLabel.setText("");
            }
        });
        
        // Update price when quantity changes
        quantityField.textProperty().addListener((obs, oldVal, newVal) -> {
            Product selectedProduct = productCombo.getValue();
            if (selectedProduct != null) {
                updatePriceInfo(priceInfoLabel, selectedProduct, newVal);
            }
        });
        
        // Build grid
        grid.add(new Label("Căutare:"), 0, 0);
        grid.add(searchField, 1, 0);
        
        grid.add(new Label("Produs:"), 0, 1);
        grid.add(productCombo, 1, 1);
        
        grid.add(new Label("Cantitate:"), 0, 2);
        grid.add(quantityField, 1, 2);
        
        grid.add(stockInfoLabel, 1, 3);
        grid.add(priceInfoLabel, 1, 4);
        
        dialog.getDialogPane().setContent(grid);
        
        // Disable add button if no product selected
        javafx.scene.Node addButton = dialog.getDialogPane().lookupButton(addButtonType);
        addButton.setDisable(true);
        
        productCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            addButton.setDisable(newVal == null);
        });
        
        // Convert result
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                Product selectedProduct = productCombo.getValue();
                if (selectedProduct == null) {
                    return null;
                }
                
                try {
                    BigDecimal quantity = new BigDecimal(quantityField.getText().trim());
                    if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
                        showError("Cantitatea trebuie să fie mai mare decât 0!");
                        return null;
                    }
                    
                    if (quantity.compareTo(selectedProduct.getPhysicalStock()) > 0) {
                        showError("Stoc insuficient! Disponibil: " + selectedProduct.getPhysicalStock());
                        return null;
                    }
                    
                    return new ManualEntryResult(selectedProduct, quantity);
                } catch (NumberFormatException e) {
                    showError("Cantitate invalidă!");
                    return null;
                }
            }
            return null;
        });
        
        // Show dialog and process result
        dialog.showAndWait().ifPresent(result -> {
            addToCartWithQuantity(result.product, result.quantity);
            posStatusLabel.setText("✅ Adăugat: " + result.product.getName() + " x " + result.quantity);
        });
    }
    
    private void updatePriceInfo(Label priceLabel, Product product, String quantityText) {
        try {
            BigDecimal quantity = new BigDecimal(quantityText.trim());
            BigDecimal total = product.getSalePrice().multiply(quantity).setScale(2, RoundingMode.HALF_UP);
            priceLabel.setText("Total: " + total + " lei");
        } catch (NumberFormatException e) {
            priceLabel.setText("Cantitate invalidă");
        }
    }
    
    private void addToCartWithQuantity(Product product, BigDecimal quantity) {
        if (product.getPhysicalStock().compareTo(quantity) < 0) {
            showError("Stoc epuizat pentru: " + product.getName());
            return;
        }
        
        Optional<CartItem> existingItem = cartItems.stream()
            .filter(item -> item.getProduct().getId().equals(product.getId()))
            .findFirst();
        
        if (existingItem.isPresent()) {
            existingItem.get().addQuantity(quantity);
        } else {
            cartItems.add(new CartItem(product, quantity));
        }
        
        updateCartSummary();
    }
    
    // Helper class for manual entry dialog result
    private static class ManualEntryResult {
        final Product product;
        final BigDecimal quantity;
        
        ManualEntryResult(Product product, BigDecimal quantity) {
            this.product = product;
            this.quantity = quantity;
        }
    }
    
    @FXML
    public void addToCart(Product product) {
        if (product.getPhysicalStock().compareTo(BigDecimal.ONE) < 0) {
            showError("Stoc epuizat pentru: " + product.getName());
            return;
        }
        
        Optional<CartItem> existingItem = cartItems.stream()
            .filter(item -> item.getProduct().getId().equals(product.getId()))
            .findFirst();
        
        if (existingItem.isPresent()) {
            existingItem.get().addQuantity(BigDecimal.ONE);
        } else {
            cartItems.add(new CartItem(product, BigDecimal.ONE));
        }
        
        updateCartSummary();
        posStatusLabel.setText("Produs adăugat în coș");
    }
    
    private void removeFromCart(CartItem item) {
        cartItems.remove(item);
        updateCartSummary();
        posStatusLabel.setText("Produs eliminat din coș");
    }
    
    @FXML
    public void clearCart() {
        if (cartItems.isEmpty()) {
            return;
        }
        
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmare Golire Coș");
        alert.setHeaderText("Sunteți sigur că doriți să goliți coșul?");
        alert.setContentText("Toate produsele vor fi eliminate din coș.");
        
        if (alert.showAndWait().get() == ButtonType.OK) {
            cartItems.clear();
            updateCartSummary();
            posStatusLabel.setText("Coș golit");
        }
    }
    
    private void updateCartSummary() {
        totalItemsLabel.setText(String.valueOf(cartItems.size()));
        
        cachedCartTotal = cartItems.stream()
            .map(CartItem::getTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        totalAmountLabel.setText(String.format("%.2f lei", cachedCartTotal));
        calculateChange();
    }
    
    private BigDecimal getCartTotal() {
        return cachedCartTotal;
    }
    
    @FXML
    public void processPayment() {
        if (cartItems.isEmpty()) {
            showError("Coșul este gol!");
            return;
        }
        
        BigDecimal total = getCartTotal();
        
        try {
            BigDecimal amountReceived = new BigDecimal(amountReceivedField.getText());
            if (amountReceived.compareTo(total) < 0) {
                showError("Suma primită este insuficientă!");
                return;
            }
            
            // Creare lista de CartItem pentru serviciu
            List<SaleService.CartItem> saleCartItems = cartItems.stream()
                .map(item -> new SaleService.CartItem(
                    item.getProduct().getId(),
                    item.getQuantity(),
                    item.getUnitPrice()
                ))
                .toList();
            
            // Salvare vânzare în baza de date
            String paymentMethod = paymentMethodCombo.getValue();
            String operator = "Operator"; // Poate fi preluat din sistem de login
            
            com.bakerymanager.entity.Sale savedSale = saleService.createSale(
                saleCartItems, 
                paymentMethod, 
                amountReceived, 
                operator
            );
            
            // Actualizare statistici locale
            dailySales = dailySales.add(total);
            dailySalesLabel.setText(String.format("%.2f lei", dailySales));
            
            // Golire coș
            cartItems.clear();
            updateCartSummary();
            amountReceivedField.clear();
            
            posStatusLabel.setText("✅ Vânzare finalizată cu succes! ID: " + savedSale.getId());
            showSuccessMessage("Plată procesată cu succes!\n" +
                "ID Vânzare: " + savedSale.getId() + "\n" +
                "Total: " + total + " lei\n" +
                "Rest: " + amountReceived.subtract(total).setScale(2, RoundingMode.HALF_UP) + " lei");
            
            // Reîncărcare produse pentru a actualiza stocurile afișate
            loadProducts();
            
        } catch (NumberFormatException e) {
            showError("Sumă primită invalidă!");
        } catch (Exception e) {
            logger.error("Error processing payment", e);
            showError("Eroare la procesarea plății: " + e.getMessage());
        }
    }
    
    @FXML
    public void printReceipt() {
        if (cartItems.isEmpty()) {
            showError("Coșul este gol!");
            return;
        }
        
        StringBuilder receipt = new StringBuilder();
        receipt.append("=== BON FISCAL ===\n");
        receipt.append("Data: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))).append("\n");
        receipt.append("==================\n");
        
        BigDecimal total = BigDecimal.ZERO;
        for (CartItem item : cartItems) {
            receipt.append(String.format("%-20s %5s x %8.2f = %8.2f\n",
                item.getProductName(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getTotal()));
            total = total.add(item.getTotal());
        }
        
        receipt.append("==================\n");
        receipt.append(String.format("TOTAL: %25.2f lei\n", total));
        receipt.append("Metodă plată: ").append(paymentMethodCombo.getValue()).append("\n");
        
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Bon Fiscal");
        alert.setHeaderText(null);
        alert.setContentText(receipt.toString());
        alert.getDialogPane().setPrefWidth(400);
        alert.show();
    }
    
    @FXML
    public void showDailyReport() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Raport Zilnic");
        alert.setHeaderText("Raport Vânzări - " + 
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
        
        StringBuilder report = new StringBuilder();
        report.append("Total vânzări azi: ").append(String.format("%.2f lei", dailySales)).append("\n\n");
        
        if (!cartItems.isEmpty()) {
            report.append("Coș curent:\n");
            BigDecimal cartTotal = BigDecimal.ZERO;
            for (CartItem item : cartItems) {
                report.append("• ").append(item.getProductName())
                     .append(" (").append(item.getQuantity()).append(") - ")
                     .append(item.getTotal()).append(" lei\n");
                cartTotal = cartTotal.add(item.getTotal());
            }
            report.append("Total coș: ").append(String.format("%.2f lei", cartTotal));
        } else {
            report.append("Coșul este gol.");
        }
        
        alert.setContentText(report.toString());
        alert.getDialogPane().setPrefWidth(400);
        alert.show();
    }
    
    private void calculateChange() {
        try {
            if (amountReceivedField.getText().trim().isEmpty()) {
                changeLabel.setText("0.00 lei");
                return;
            }
            
            BigDecimal total = getCartTotal();
            
            BigDecimal received = new BigDecimal(amountReceivedField.getText());
            BigDecimal change = received.subtract(total);
            
            changeLabel.setText(String.format("%.2f lei", change));
            
            if (change.compareTo(BigDecimal.ZERO) >= 0) {
                changeLabel.setStyle("-fx-text-fill: green;");
            } else {
                changeLabel.setStyle("-fx-text-fill: red;");
            }
            
        } catch (NumberFormatException e) {
            changeLabel.setText("Sumă invalidă");
            changeLabel.setStyle("-fx-text-fill: red;");
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
