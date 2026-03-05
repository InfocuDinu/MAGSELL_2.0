package com.bakerymanager.controller;

import com.bakerymanager.entity.Ingredient;
import com.bakerymanager.entity.User;
import com.bakerymanager.entity.Waste;
import com.bakerymanager.exception.AuthorizationException;
import com.bakerymanager.smartbill.inventory.api.InventoryFacade;
import com.bakerymanager.service.AuthorizationService;
import com.bakerymanager.service.IngredientService;
import com.bakerymanager.service.MultiLocationInventoryService;
import com.bakerymanager.service.ReplenishmentService;
import com.bakerymanager.service.StockService;
import com.bakerymanager.service.UserService;
import com.bakerymanager.service.WasteService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
public class InventoryController {
    
    private static final Logger logger = LoggerFactory.getLogger(InventoryController.class);
    
    private final InventoryFacade inventoryFacade;
    private final AuthorizationService authorizationService;
    private final IngredientService ingredientService;
    private final MultiLocationInventoryService multiLocationInventoryService;
    private final StockService stockService;
    private final WasteService wasteService;
    private final UserService userService;
    private final ReplenishmentService replenishmentService;
    
    public InventoryController(InventoryFacade inventoryFacade,
                               AuthorizationService authorizationService,
                               IngredientService ingredientService,
                               MultiLocationInventoryService multiLocationInventoryService,
                               StockService stockService,
                               WasteService wasteService,
                               UserService userService,
                               ReplenishmentService replenishmentService) {
        this.inventoryFacade = inventoryFacade;
        this.authorizationService = authorizationService;
        this.ingredientService = ingredientService;
        this.multiLocationInventoryService = multiLocationInventoryService;
        this.stockService = stockService;
        this.wasteService = wasteService;
        this.userService = userService;
        this.replenishmentService = replenishmentService;
    }
    
    // Form fields
    @FXML
    private TextField nameField;
    
    @FXML
    private ComboBox<Ingredient.ProductType> productTypeCombo;
    
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
    private TextField warehouseField;

    @FXML
    private TextField zoneField;
    
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
        setupProductTypeComboBox();
        setupUnitComboBox();
        setupTable();
        loadIngredients();
        updateStatistics();
        logger.info("Inventory controller initialized");
    }
    
    private void setupProductTypeComboBox() {
        productTypeCombo.setItems(FXCollections.observableArrayList(Ingredient.ProductType.values()));
        productTypeCombo.setValue(Ingredient.ProductType.MATERIE_PRIMA); // Default value
        
        // Custom display using StringConverter
        productTypeCombo.setConverter(new javafx.util.StringConverter<Ingredient.ProductType>() {
            @Override
            public String toString(Ingredient.ProductType type) {
                return type != null ? type.getDisplayName() : "";
            }
            
            @Override
            public Ingredient.ProductType fromString(String string) {
                return productTypeCombo.getItems().stream()
                    .filter(type -> type.getDisplayName().equals(string))
                    .findFirst()
                    .orElse(null);
            }
        });
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
            List<Ingredient> ingredients = inventoryFacade.getAllIngredients();
            ingredients.forEach(multiLocationInventoryService::syncIngredientLocationStock);
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
        productTypeCombo.setValue(ingredient.getProductType() != null ? 
            ingredient.getProductType() : Ingredient.ProductType.MATERIE_PRIMA);
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
        warehouseField.setText(ingredient.getWarehouse() != null ? ingredient.getWarehouse() : "");
        zoneField.setText(ingredient.getZone() != null ? ingredient.getZone() : "");
        setStatus("Selectat: " + ingredient.getName());
    }
    
    @FXML
    public void addIngredient() {
        clearForm();
        nameField.requestFocus();
        setStatus("Gata pentru adăugare produs nou");
    }
    
    @FXML
    public void saveIngredient() {
        if (!requireManagerOrAdmin("INVENTORY_SAVE", "ingredient")) {
            return;
        }
        try {
            // Validate required fields
            if (nameField.getText() == null || nameField.getText().trim().isEmpty()) {
                showError("Numele produsului este obligatoriu!");
                return;
            }
            
            if (unitCombo.getValue() == null) {
                showError("Unitatea de măsură este obligatorie!");
                return;
            }
            
            if (productTypeCombo.getValue() == null) {
                showError("Tipul produsului este obligatoriu!");
                return;
            }
            
            // Determine if this is an update or create operation
            boolean isUpdate = selectedIngredient != null;
            
            // Create or update ingredient
            Ingredient ingredient = selectedIngredient != null ? selectedIngredient : new Ingredient();
            ingredient.setName(nameField.getText().trim());
            ingredient.setUnitOfMeasure(unitCombo.getValue());
            ingredient.setProductType(productTypeCombo.getValue());
            
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

            ingredient.setWarehouse(normalizeOptionalField(warehouseField.getText()));
            ingredient.setZone(normalizeOptionalField(zoneField.getText()));
            
            // Save ingredient
            inventoryFacade.saveIngredient(ingredient);
            multiLocationInventoryService.syncIngredientLocationStock(ingredient);
            
            // Reload and update
            loadIngredients();
            updateStatistics();
            clearForm();
            
            String message = isUpdate ? "Produs actualizat cu succes!" : "Produs adăugat cu succes!";
            showSuccessMessage(message);
            setStatus(message);
            
        } catch (Exception e) {
            logger.error("Error saving ingredient", e);
            showError("Eroare la salvarea produsului: " + e.getMessage());
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
        if (!requireManagerOrAdmin("INVENTORY_DELETE", "ingredient")) {
            return;
        }
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
                    inventoryFacade.deleteIngredient(selectedIngredient.getId());
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
    public void transferInternal() {
        if (!requireManagerOrAdmin("INVENTORY_TRANSFER", "ingredient.transfer")) {
            return;
        }
        Ingredient ingredient = requireSelectedIngredient();
        if (ingredient == null) {
            return;
        }

        BigDecimal quantity = promptDecimal("Transfer intern", "Cantitate transferată:");
        if (quantity == null) {
            return;
        }

        String fromWarehouse = normalizeOptionalField(promptText("Transfer intern", "Depozit sursă:"));
        if (fromWarehouse == null) {
            return;
        }

        String fromZone = normalizeOptionalField(promptText("Transfer intern", "Zonă sursă (opțional):"));

        String toWarehouse = normalizeOptionalField(promptText("Transfer intern", "Depozit destinație:"));
        if (toWarehouse == null) {
            return;
        }

        String toZone = normalizeOptionalField(promptText("Transfer intern", "Zonă destinație (opțional):"));

        String reason = promptText("Transfer intern", "Motiv transfer:");
        if (reason == null || reason.isBlank()) {
            showError("Motivul este obligatoriu pentru transfer intern.");
            return;
        }

        try {
            String unit = ingredient.getUnitOfMeasure() != null ? ingredient.getUnitOfMeasure().name() : null;
            multiLocationInventoryService.transferBetweenLocations(
                ingredient,
                quantity,
                unit,
                fromWarehouse,
                fromZone,
                toWarehouse,
                toZone,
                reason
            );
            setStatus("Transfer intern înregistrat pentru " + ingredient.getName());
            showSuccessMessage("Transfer intern înregistrat cu succes.");
        } catch (Exception e) {
            logger.error("Error during internal transfer", e);
            showError("Eroare la transfer intern: " + e.getMessage());
        }
    }

    @FXML
    public void adjustStock() {
        if (!requireManagerOrAdmin("INVENTORY_ADJUST", "ingredient.adjust")) {
            return;
        }
        Ingredient ingredient = requireSelectedIngredient();
        if (ingredient == null) {
            return;
        }

        String mode = promptChoice("Ajustare stoc", "Tip ajustare:", List.of("Pozitivă (+)", "Negativă (-)"));
        if (mode == null) {
            return;
        }

        BigDecimal quantity = promptDecimal("Ajustare stoc", "Cantitate ajustare:");
        if (quantity == null) {
            return;
        }

        String reason = promptText("Ajustare stoc", "Motiv ajustare:");
        if (reason == null || reason.isBlank()) {
            showError("Motivul este obligatoriu pentru ajustare.");
            return;
        }

        try {
            String unit = ingredient.getUnitOfMeasure() != null ? ingredient.getUnitOfMeasure().name() : null;
            BigDecimal delta = "Pozitivă (+)".equals(mode) ? quantity : quantity.negate();
            stockService.adjustStock(ingredient, delta, unit, reason);

            ingredient.setCurrentStock(ingredient.getCurrentStock().add(delta));
            inventoryFacade.saveIngredient(ingredient);
            multiLocationInventoryService.applyDeltaToPrimaryLocation(ingredient, delta);

            loadIngredients();
            setStatus("Ajustare stoc înregistrată pentru " + ingredient.getName());
            showSuccessMessage("Ajustare de stoc realizată cu succes.");
        } catch (Exception e) {
            logger.error("Error during stock adjustment", e);
            showError("Eroare la ajustare stoc: " + e.getMessage());
        }
    }

    @FXML
    public void returnToSupplier() {
        if (!requireManagerOrAdmin("INVENTORY_RETURN", "ingredient.return")) {
            return;
        }
        Ingredient ingredient = requireSelectedIngredient();
        if (ingredient == null) {
            return;
        }

        BigDecimal quantity = promptDecimal("Retur furnizor", "Cantitate returnată:");
        if (quantity == null) {
            return;
        }

        String reason = promptText("Retur furnizor", "Motiv retur:");
        if (reason == null || reason.isBlank()) {
            showError("Motivul este obligatoriu pentru retur furnizor.");
            return;
        }

        try {
            String unit = ingredient.getUnitOfMeasure() != null ? ingredient.getUnitOfMeasure().name() : null;
            stockService.returnToSupplier(ingredient, quantity, unit, reason);

            ingredient.setCurrentStock(ingredient.getCurrentStock().subtract(quantity));
            inventoryFacade.saveIngredient(ingredient);
            multiLocationInventoryService.applyDeltaToPrimaryLocation(ingredient, quantity.negate());

            loadIngredients();
            setStatus("Retur furnizor înregistrat pentru " + ingredient.getName());
            showSuccessMessage("Retur către furnizor înregistrat cu succes.");
        } catch (Exception e) {
            logger.error("Error during supplier return", e);
            showError("Eroare la retur furnizor: " + e.getMessage());
        }
    }

    @FXML
    public void registerWaste() {
        if (!requireManagerOrAdmin("INVENTORY_WASTE", "ingredient.waste")) {
            return;
        }
        Ingredient ingredient = requireSelectedIngredient();
        if (ingredient == null) {
            return;
        }

        String wasteType = promptChoice(
            "Pierderi/Casare",
            "Tip pierdere:",
            List.of("Rebut", "Expirat", "Donație")
        );
        if (wasteType == null) {
            return;
        }

        BigDecimal quantity = promptDecimal("Pierderi/Casare", "Cantitate pierdută/casată:");
        if (quantity == null) {
            return;
        }

        String reason = promptText("Pierderi/Casare", "Motiv pierdere/casare:");
        if (reason == null || reason.isBlank()) {
            showError("Motivul este obligatoriu pentru pierdere/casare.");
            return;
        }

        try {
            String unit = ingredient.getUnitOfMeasure() != null ? ingredient.getUnitOfMeasure().name() : null;
            stockService.registerWaste(ingredient, quantity, unit, reason);

            Waste.WasteReason wasteReason = mapWasteReason(wasteType);
            String recordedBy = userService.getCurrentUser()
                .map(user -> user.getUsername() != null ? user.getUsername() : user.getFullName())
                .orElse("SYSTEM");
            String notes = "Tip: " + wasteType + ". Motiv: " + reason;
            wasteService.recordIngredientWaste(ingredient, quantity, wasteReason, recordedBy, notes);

            ingredient.setCurrentStock(ingredient.getCurrentStock().subtract(quantity));
            inventoryFacade.saveIngredient(ingredient);
            multiLocationInventoryService.applyDeltaToPrimaryLocation(ingredient, quantity.negate());

            loadIngredients();
            setStatus("Pierdere/Casare înregistrată pentru " + ingredient.getName());
            showSuccessMessage("Document de pierdere/casare înregistrat cu succes.");
        } catch (Exception e) {
            logger.error("Error during waste registration", e);
            showError("Eroare la înregistrare pierdere/casare: " + e.getMessage());
        }
    }

    @FXML
    public void barcodeReception() {
        if (!requireOperatorOrAbove("INVENTORY_BARCODE_RECEIPT", "ingredient.barcode.receipt")) {
            return;
        }
        String barcode = promptText("Recepție prin scanare", "Scanați/introduceți codul de bare:");
        if (barcode == null || barcode.isBlank()) {
            return;
        }

        Ingredient ingredient = findIngredientByBarcode(barcode);
        if (ingredient == null) {
            return;
        }

        BigDecimal quantity = promptDecimal("Recepție prin scanare", "Cantitate recepționată:");
        if (quantity == null) {
            return;
        }

        String batchCodeInput = promptText("Recepție prin scanare", "Cod lot (opțional):");
        String batchCode = normalizeOptionalField(batchCodeInput);
        if (batchCode == null) {
            batchCode = "BC-" + barcode.trim() + "-" + System.currentTimeMillis();
        }

        String expiryText = promptText("Recepție prin scanare", "Data expirare (YYYY-MM-DD, opțional):");
        LocalDate expiryDate;
        try {
            expiryDate = parseOptionalDate(expiryText);
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
            return;
        }

        String reasonInput = promptText("Recepție prin scanare", "Motiv/observații (opțional):");
        String reason = normalizeOptionalField(reasonInput);
        if (reason == null) {
            reason = "Recepție scanare cod bare: " + barcode.trim();
        }

        try {
            String unit = ingredient.getUnitOfMeasure() != null ? ingredient.getUnitOfMeasure().name() : null;
            stockService.receiveBatch(
                ingredient,
                quantity,
                unit,
                expiryDate,
                LocalDateTime.now(),
                batchCode,
                "BARCODE_SCANNER_INTAKE",
                ingredient.getId(),
                reason
            );

            ingredient.setCurrentStock(ingredient.getCurrentStock().add(quantity));
            inventoryFacade.saveIngredient(ingredient);
            multiLocationInventoryService.applyDeltaToPrimaryLocation(ingredient, quantity);

            loadIngredients();
            setStatus("Recepție scanată: " + ingredient.getName() + " (lot " + batchCode + ")");
            showSuccessMessage("Recepție înregistrată cu succes pentru " + ingredient.getName());
        } catch (Exception e) {
            logger.error("Error during barcode intake", e);
            showError("Eroare la recepția prin scanare: " + e.getMessage());
        }
    }

    @FXML
    public void barcodeConsumption() {
        if (!requireOperatorOrAbove("INVENTORY_BARCODE_CONSUMPTION", "ingredient.barcode.consumption")) {
            return;
        }
        String barcode = promptText("Consum prin scanare", "Scanați/introduceți codul de bare:");
        if (barcode == null || barcode.isBlank()) {
            return;
        }

        Ingredient ingredient = findIngredientByBarcode(barcode);
        if (ingredient == null) {
            return;
        }

        BigDecimal quantity = promptDecimal("Consum prin scanare", "Cantitate consumată:");
        if (quantity == null) {
            return;
        }

        String lotCode = normalizeOptionalField(
            promptText("Consum prin scanare", "Cod lot (opțional; gol = FEFO automat):")
        );
        String reasonInput = promptText("Consum prin scanare", "Motiv/observații (opțional):");
        String reason = normalizeOptionalField(reasonInput);
        if (reason == null) {
            reason = "Consum scanare cod bare: " + barcode.trim();
        }

        try {
            String unit = ingredient.getUnitOfMeasure() != null ? ingredient.getUnitOfMeasure().name() : null;

            if (lotCode != null) {
                stockService.consumeFromSpecificBatch(
                    ingredient,
                    lotCode,
                    quantity,
                    unit,
                    "BARCODE_SCANNER_OUTTAKE",
                    ingredient.getId(),
                    reason
                );
            } else {
                stockService.consumeFefo(
                    ingredient,
                    quantity,
                    unit,
                    "BARCODE_SCANNER_OUTTAKE",
                    ingredient.getId(),
                    reason
                );
            }

            ingredient.setCurrentStock(ingredient.getCurrentStock().subtract(quantity));
            inventoryFacade.saveIngredient(ingredient);
            multiLocationInventoryService.applyDeltaToPrimaryLocation(ingredient, quantity.negate());

            loadIngredients();
            setStatus("Consum scanat: " + ingredient.getName() + (lotCode != null ? " (lot " + lotCode + ")" : ""));
            showSuccessMessage("Consum înregistrat cu succes pentru " + ingredient.getName());
        } catch (Exception e) {
            logger.error("Error during barcode consumption", e);
            showError("Eroare la consumul prin scanare: " + e.getMessage());
        }
    }
    
    @FXML
    public void clearForm() {
        selectedIngredient = null;
        nameField.clear();
        productTypeCombo.setValue(Ingredient.ProductType.MATERIE_PRIMA); // Reset to default
        quantityField.clear();
        unitCombo.setValue(null);
        priceField.clear();
        minStockField.clear();
        barcodeField.clear();
        warehouseField.clear();
        zoneField.clear();
        ingredientsTable.getSelectionModel().clearSelection();
        setStatus("Formular golit");
    }
    
    private void updateStatistics() {
        List<Ingredient> ingredients = inventoryFacade.getAllIngredients();
        totalIngredientsLabel.setText("Total produse: " + ingredients.size());
    }

    private Ingredient requireSelectedIngredient() {
        if (selectedIngredient == null) {
            showError("Selectați un ingredient din tabel.");
            return null;
        }
        return selectedIngredient;
    }

    private BigDecimal promptDecimal(String title, String header) {
        String value = promptText(title, header);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            BigDecimal parsed = new BigDecimal(value.trim());
            if (parsed.compareTo(BigDecimal.ZERO) <= 0) {
                showError("Cantitatea trebuie să fie mai mare decât zero.");
                return null;
            }
            return parsed;
        } catch (NumberFormatException ex) {
            showError("Cantitatea introdusă nu este validă.");
            return null;
        }
    }

    private String promptText(String title, String header) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle(title);
        dialog.setHeaderText(header);
        dialog.setContentText("Valoare:");
        Optional<String> result = dialog.showAndWait();
        return result.orElse(null);
    }

    private String promptChoice(String title, String header, List<String> options) {
        if (options == null || options.isEmpty()) {
            return null;
        }
        ChoiceDialog<String> dialog = new ChoiceDialog<>(options.get(0), options);
        dialog.setTitle(title);
        dialog.setHeaderText(header);
        dialog.setContentText("Selectați:");
        Optional<String> result = dialog.showAndWait();
        return result.orElse(null);
    }

    @FXML
    public void runAssistedInventory() {
        if (!requireManagerOrAdmin("INVENTORY_ASSISTED_COUNT", "inventory.assisted")) {
            return;
        }
        try {
            Dialog<ButtonType> filterDialog = new Dialog<>();
            filterDialog.setTitle("Inventariere asistată");
            filterDialog.setHeaderText("Filtrare pe depozit / zonă (opțional)");
            filterDialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);

            TextField warehouseFilterField = new TextField();
            warehouseFilterField.setPromptText("Ex: Depozit Principal");
            TextField zoneFilterField = new TextField();
            zoneFilterField.setPromptText("Ex: Raft A");

            grid.add(new Label("Depozit:"), 0, 0);
            grid.add(warehouseFilterField, 1, 0);
            grid.add(new Label("Zonă:"), 0, 1);
            grid.add(zoneFilterField, 1, 1);

            filterDialog.getDialogPane().setContent(grid);

            Optional<ButtonType> filterResult = filterDialog.showAndWait();
            if (filterResult.isEmpty() || filterResult.get() != ButtonType.OK) {
                return;
            }

            String warehouseFilter = normalizeOptionalField(warehouseFilterField.getText());
            String zoneFilter = normalizeOptionalField(zoneFilterField.getText());

            List<Ingredient> candidates = inventoryFacade.getAllIngredients().stream()
                .filter(ing -> matchesFilter(ing.getWarehouse(), warehouseFilter))
                .filter(ing -> matchesFilter(ing.getZone(), zoneFilter))
                .sorted(Comparator.comparing(Ingredient::getName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());

            if (candidates.isEmpty()) {
                showError("Nu există produse/ingrediente pentru filtrul selectat.");
                return;
            }

            Dialog<ButtonType> countDialog = new Dialog<>();
            countDialog.setTitle("Inventariere asistată");
            countDialog.setHeaderText("Introduceți cantitățile faptice (format: nume|cantitate)");
            countDialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            TextArea countsArea = new TextArea();
            countsArea.setPrefRowCount(18);
            countsArea.setPrefColumnCount(80);
            countsArea.setWrapText(false);
            countsArea.setText(buildCountingTemplate(candidates));
            countDialog.getDialogPane().setContent(countsArea);

            Node okButton = countDialog.getDialogPane().lookupButton(ButtonType.OK);
            okButton.setDisable(false);

            Optional<ButtonType> countResult = countDialog.showAndWait();
            if (countResult.isEmpty() || countResult.get() != ButtonType.OK) {
                return;
            }

            Map<String, BigDecimal> factualByName = parseFactualInput(countsArea.getText());
            List<InventoryDiff> diffs = computeDiffs(candidates, factualByName, warehouseFilter, zoneFilter);

            String summary = buildInventorySummary(diffs, warehouseFilter, zoneFilter);
            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
            confirmation.setTitle("Inventariere asistată - Propuneri ajustare");
            confirmation.setHeaderText("Scriptic vs Faptic + propuneri");
            TextArea summaryArea = new TextArea(summary);
            summaryArea.setEditable(false);
            summaryArea.setWrapText(false);
            summaryArea.setPrefRowCount(18);
            summaryArea.setPrefColumnCount(95);
            confirmation.getDialogPane().setContent(summaryArea);
            confirmation.getDialogPane().setPrefWidth(1000);
            confirmation.setContentText(null);

            Optional<ButtonType> confirmResult = confirmation.showAndWait();
            if (confirmResult.isEmpty() || confirmResult.get() != ButtonType.OK) {
                setStatus("Inventariere asistată anulată (fără ajustări aplicate)");
                return;
            }

            int adjustedCount = 0;
            for (InventoryDiff diff : diffs) {
                if (diff.delta.compareTo(BigDecimal.ZERO) == 0) {
                    continue;
                }

                String unit = diff.ingredient.getUnitOfMeasure() != null ? diff.ingredient.getUnitOfMeasure().name() : null;
                String reason = "Inventariere asistată " + LocalDateTime.now() +
                    " | depozit=" + (warehouseFilter != null ? warehouseFilter : "TOATE") +
                    " | zona=" + (zoneFilter != null ? zoneFilter : "TOATE");
                stockService.adjustStock(diff.ingredient, diff.delta, unit, reason);

                diff.ingredient.setCurrentStock(diff.factualQty);
                inventoryFacade.saveIngredient(diff.ingredient);
                adjustedCount++;
            }

            loadIngredients();
            setStatus("Inventariere finalizată. Ajustări aplicate: " + adjustedCount);
            showSuccessMessage("Inventariere asistată finalizată cu succes.\nAjustări aplicate: " + adjustedCount);

        } catch (Exception e) {
            logger.error("Error running assisted inventory", e);
            showError("Eroare la inventarierea asistată: " + e.getMessage());
        }
    }

    @FXML
    public void runSmartReplenishment() {
        if (!requireManagerOrAdmin("INVENTORY_REPLENISHMENT", "inventory.replenishment")) {
            return;
        }
        try {
            List<ReplenishmentService.ReplenishmentSuggestion> suggestions = replenishmentService.generateTodaySuggestions();
            if (suggestions.isEmpty()) {
                showInfoDialog("Reaprovizionare inteligentă", "Nu există articole de comandat azi.");
                setStatus("Reaprovizionare inteligentă: nimic de comandat azi");
                return;
            }

            StringBuilder sb = new StringBuilder();
            sb.append("=== LISTĂ AUTOMATĂ ‘DE COMANDAT AZI’ ===\n");
            sb.append(String.format("%-22s | %8s | %8s | %8s | %10s | %8s | %s\n",
                "Articol", "Stoc", "Siguranță", "Avg/zi", "Punct cmd", "Comandă", "Prioritate"));
            sb.append("-----------------------------------------------------------------------------------------------------\n");

            for (ReplenishmentService.ReplenishmentSuggestion s : suggestions) {
                sb.append(String.format("%-22s | %8.3f | %8.3f | %8.3f | %10.3f | %8.3f | %s\n",
                    trimForReport(s.ingredientName(), 22),
                    s.currentStock(),
                    s.safetyStock(),
                    s.avgDailyConsumption(),
                    s.reorderPoint(),
                    s.recommendedOrderQty(),
                    s.priority()));
            }

            sb.append("\nNotă: punct comandă = stoc siguranță + consum mediu/zi × lead-time (3 zile).\n");
            sb.append("Consum mediu/zi calculat pe ultimele 30 zile (consum + waste + retur).\n");

            TextArea area = new TextArea(sb.toString());
            area.setEditable(false);
            area.setWrapText(false);
            area.setPrefRowCount(20);
            area.setPrefColumnCount(110);

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Reaprovizionare inteligentă");
            alert.setHeaderText("Listă automată ‘de comandat azi’");
            alert.getDialogPane().setContent(area);
            alert.getDialogPane().setPrefWidth(1100);
            alert.setContentText(null);
            alert.show();

            setStatus("Reaprovizionare inteligentă generată: " + suggestions.size() + " articole");
        } catch (Exception e) {
            logger.error("Error generating smart replenishment list", e);
            showError("Eroare la reaprovizionarea inteligentă: " + e.getMessage());
        }
    }

    @FXML
    public void exportSmartReplenishmentCsv() {
        if (!requireManagerOrAdmin("INVENTORY_EXPORT_REPLENISHMENT", "inventory.replenishment.export")) {
            return;
        }
        try {
            List<ReplenishmentService.ReplenishmentSuggestion> suggestions = replenishmentService.generateTodaySuggestions();
            if (suggestions.isEmpty()) {
                showInfoDialog("Reaprovizionare inteligentă", "Nu există articole de exportat azi.");
                return;
            }

            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Salvează lista de reaprovizionare (CSV)");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
            fileChooser.setInitialFileName("de_comandat_azi_" + LocalDateTime.now().toLocalDate() + ".csv");

            File selectedFile = fileChooser.showSaveDialog(ingredientsTable.getScene().getWindow());
            if (selectedFile == null) {
                return;
            }

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(selectedFile))) {
                writer.write("ingredient,unit,depozit,zona,stoc_curent,stoc_siguranta,consum_mediu_zi,punct_comanda,cantitate_de_comandat,prioritate");
                writer.newLine();

                for (ReplenishmentService.ReplenishmentSuggestion s : suggestions) {
                    writer.write(String.join(",",
                        csv(s.ingredientName()),
                        csv(s.unit()),
                        csv(s.warehouse()),
                        csv(s.zone()),
                        s.currentStock().toPlainString(),
                        s.safetyStock().toPlainString(),
                        s.avgDailyConsumption().toPlainString(),
                        s.reorderPoint().toPlainString(),
                        s.recommendedOrderQty().toPlainString(),
                        csv(s.priority())
                    ));
                    writer.newLine();
                }
            }

            setStatus("CSV reaprovizionare exportat: " + selectedFile.getName());
            showSuccessMessage("Export realizat cu succes:\n" + selectedFile.getAbsolutePath());
        } catch (IOException e) {
            logger.error("Error exporting replenishment CSV", e);
            showError("Eroare la export CSV reaprovizionare: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error exporting replenishment CSV", e);
            showError("Eroare neașteptată la export reaprovizionare: " + e.getMessage());
        }
    }

    @FXML
    public void showMultiLocationReport() {
        if (!requireManagerOrAdmin("INVENTORY_MULTI_LOCATION_REPORT", "inventory.multi-location.report")) {
            return;
        }
        try {
            List<MultiLocationInventoryService.LocationStockRow> rows = multiLocationInventoryService.buildLocationStockRows();
            if (rows.isEmpty()) {
                showInfoDialog("Raport multi-location", "Nu există stocuri distribuite pe locații.");
                return;
            }

            StringBuilder sb = new StringBuilder();
            sb.append("=== RAPORT STOCURI PE LOCAȚII ===\n");
            sb.append(String.format("%-25s | %-20s | %-15s | %10s | %s\n", "Articol", "Depozit", "Zonă", "Cantitate", "UM"));
            sb.append("--------------------------------------------------------------------------------------------------------\n");

            for (MultiLocationInventoryService.LocationStockRow row : rows) {
                sb.append(String.format("%-25s | %-20s | %-15s | %10.3f | %s\n",
                    trimForReport(row.ingredientName(), 25),
                    trimForReport(row.warehouse(), 20),
                    trimForReport(row.zone(), 15),
                    row.quantity(),
                    row.unit()
                ));
            }

            TextArea area = new TextArea(sb.toString());
            area.setEditable(false);
            area.setWrapText(false);
            area.setPrefRowCount(20);
            area.setPrefColumnCount(110);

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Raport Multi-location");
            alert.setHeaderText("Stocuri separate per depozit/zonă");
            alert.getDialogPane().setContent(area);
            alert.getDialogPane().setPrefWidth(1100);
            alert.setContentText(null);
            alert.show();

            setStatus("Raport multi-location generat: " + rows.size() + " poziții");
        } catch (Exception e) {
            logger.error("Error generating multi-location report", e);
            showError("Eroare la generarea raportului multi-location: " + e.getMessage());
        }
    }

    private String buildCountingTemplate(List<Ingredient> ingredients) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Format: nume|cantitate_faptica\n");
        sb.append("# Exemplu: Faina alba|120.500\n");
        for (Ingredient ing : ingredients) {
            BigDecimal scriptic = ing.getCurrentStock() != null ? ing.getCurrentStock() : BigDecimal.ZERO;
            sb.append(ing.getName()).append("|").append(scriptic).append("\n");
        }
        return sb.toString();
    }

    private Map<String, BigDecimal> parseFactualInput(String input) {
        Map<String, BigDecimal> result = new HashMap<>();
        if (input == null || input.isBlank()) {
            return result;
        }

        String[] lines = input.split("\\r?\\n");
        for (String rawLine : lines) {
            String line = rawLine != null ? rawLine.trim() : "";
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            String[] parts = line.split("\\|", 2);
            if (parts.length != 2) {
                continue;
            }

            String name = parts[0].trim();
            String qtyText = parts[1].trim().replace(',', '.');
            if (name.isEmpty() || qtyText.isEmpty()) {
                continue;
            }

            try {
                BigDecimal qty = new BigDecimal(qtyText);
                result.put(name.toLowerCase(Locale.ROOT), qty);
            } catch (NumberFormatException ex) {
                logger.warn("Skipping invalid factual quantity line: {}", line);
            }
        }

        return result;
    }

    private List<InventoryDiff> computeDiffs(List<Ingredient> ingredients,
                                             Map<String, BigDecimal> factualByName,
                                             String warehouseFilter,
                                             String zoneFilter) {
        List<InventoryDiff> diffs = new ArrayList<>();
        for (Ingredient ing : ingredients) {
            BigDecimal scriptic = ing.getCurrentStock() != null ? ing.getCurrentStock() : BigDecimal.ZERO;
            BigDecimal factual = factualByName.getOrDefault(ing.getName().toLowerCase(Locale.ROOT), scriptic);
            BigDecimal delta = factual.subtract(scriptic);
            diffs.add(new InventoryDiff(ing, scriptic, factual, delta, warehouseFilter, zoneFilter));
        }
        return diffs;
    }

    private String buildInventorySummary(List<InventoryDiff> diffs,
                                         String warehouseFilter,
                                         String zoneFilter) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== INVENTARIERE ASISTATĂ ===\n");
        sb.append("Depozit: ").append(warehouseFilter != null ? warehouseFilter : "TOATE").append("\n");
        sb.append("Zonă: ").append(zoneFilter != null ? zoneFilter : "TOATE").append("\n\n");
        sb.append(String.format("%-25s | %10s | %10s | %10s | %s\n", "Articol", "Scriptic", "Faptic", "Diferență", "Propunere"));
        sb.append("--------------------------------------------------------------------------------------------------------\n");

        BigDecimal totalPlus = BigDecimal.ZERO;
        BigDecimal totalMinus = BigDecimal.ZERO;

        for (InventoryDiff diff : diffs) {
            String proposal;
            if (diff.delta.compareTo(BigDecimal.ZERO) > 0) {
                proposal = "Ajustare +" + diff.delta.setScale(3, RoundingMode.HALF_UP);
                totalPlus = totalPlus.add(diff.delta);
            } else if (diff.delta.compareTo(BigDecimal.ZERO) < 0) {
                proposal = "Ajustare " + diff.delta.setScale(3, RoundingMode.HALF_UP);
                totalMinus = totalMinus.add(diff.delta.abs());
            } else {
                proposal = "Fără ajustare";
            }

            sb.append(String.format("%-25s | %10.3f | %10.3f | %10.3f | %s\n",
                diff.ingredient.getName(),
                diff.scripticQty,
                diff.factualQty,
                diff.delta,
                proposal
            ));
        }

        sb.append("\nTOTAL ajustări + : ").append(totalPlus.setScale(3, RoundingMode.HALF_UP)).append("\n");
        sb.append("TOTAL ajustări - : ").append(totalMinus.setScale(3, RoundingMode.HALF_UP)).append("\n");
        sb.append("\nApăsați OK pentru a aplica propunerile de ajustare.");
        return sb.toString();
    }

    private boolean matchesFilter(String value, String filter) {
        if (filter == null || filter.isBlank()) {
            return true;
        }
        if (value == null || value.isBlank()) {
            return false;
        }
        return value.trim().equalsIgnoreCase(filter.trim());
    }

    private String normalizeOptionalField(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private Ingredient findIngredientByBarcode(String barcode) {
        String normalizedBarcode = normalizeOptionalField(barcode);
        if (normalizedBarcode == null) {
            showError("Codul de bare este obligatoriu.");
            return null;
        }

        List<Ingredient> matches = ingredientService.findByBarcode(normalizedBarcode);
        if (matches.isEmpty()) {
            showError("Nu există articol mapat pentru codul de bare: " + normalizedBarcode);
            return null;
        }

        if (matches.size() == 1) {
            return matches.get(0);
        }

        Map<String, Ingredient> optionsByLabel = new HashMap<>();
        List<String> labels = new ArrayList<>();
        for (Ingredient match : matches) {
            String label = String.format(
                "%s [ID:%d | Depozit:%s | Zonă:%s]",
                match.getName(),
                match.getId(),
                match.getWarehouse() != null ? match.getWarehouse() : "N/A",
                match.getZone() != null ? match.getZone() : "N/A"
            );
            optionsByLabel.put(label, match);
            labels.add(label);
        }

        String selected = promptChoice(
            "Selecție articol",
            "Codul de bare este asociat cu mai multe articole. Alegeți articolul:",
            labels
        );
        return selected != null ? optionsByLabel.get(selected) : null;
    }

    private LocalDate parseOptionalDate(String value) {
        String normalized = normalizeOptionalField(value);
        if (normalized == null) {
            return null;
        }

        try {
            return LocalDate.parse(normalized);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Data expirării trebuie să fie în formatul YYYY-MM-DD.");
        }
    }

    private String trimForReport(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, Math.max(0, maxLength - 1)) + "…";
    }

    private String csv(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n") || escaped.contains("\r")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }

    private static class InventoryDiff {
        private final Ingredient ingredient;
        private final BigDecimal scripticQty;
        private final BigDecimal factualQty;
        private final BigDecimal delta;

        private InventoryDiff(Ingredient ingredient,
                              BigDecimal scripticQty,
                              BigDecimal factualQty,
                              BigDecimal delta,
                              String warehouse,
                              String zone) {
            this.ingredient = ingredient;
            this.scripticQty = scripticQty;
            this.factualQty = factualQty;
            this.delta = delta;
        }
    }

    private Waste.WasteReason mapWasteReason(String typeLabel) {
        if (typeLabel == null) {
            return Waste.WasteReason.OTHER;
        }
        return switch (typeLabel.trim().toLowerCase()) {
            case "rebut" -> Waste.WasteReason.REBUT;
            case "expirat" -> Waste.WasteReason.EXPIRED;
            case "donație", "donatie" -> Waste.WasteReason.DONATION;
            default -> Waste.WasteReason.OTHER;
        };
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

    private void showInfoDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.show();
    }

    private boolean requireManagerOrAdmin(String action, String resource) {
        try {
            authorizationService.requireAnyRole(action, resource, User.Role.ADMIN, User.Role.MANAGER);
            return true;
        } catch (AuthorizationException ex) {
            showError(ex.getMessage());
            return false;
        }
    }

    private boolean requireOperatorOrAbove(String action, String resource) {
        try {
            authorizationService.requireOperatorOrAbove(action, resource);
            return true;
        } catch (AuthorizationException ex) {
            showError(ex.getMessage());
            return false;
        }
    }
}
