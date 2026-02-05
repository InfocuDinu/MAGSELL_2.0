package com.bakerymanager.controller;

import com.bakerymanager.entity.Ingredient;
import com.bakerymanager.service.IngredientService;
import com.bakerymanager.service.InvoiceService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.springframework.stereotype.Controller;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

@Controller
public class InventoryController {
    
    private final IngredientService ingredientService;
    private final InvoiceService invoiceService;
    
    public InventoryController(IngredientService ingredientService, InvoiceService invoiceService) {
        this.ingredientService = ingredientService;
        this.invoiceService = invoiceService;
    }
    
    @FXML
    private TextField searchField;
    
    @FXML
    private Label totalIngredientsLabel;
    
    @FXML
    private Label lowStockLabel;
    
    @FXML
    private Label totalValueLabel;
    
    @FXML
    private TableView<Ingredient> ingredientsTable;
    
    @FXML
    private TableColumn<Ingredient, Long> idColumn;
    
    @FXML
    private TableColumn<Ingredient, String> nameColumn;
    
    @FXML
    private TableColumn<Ingredient, Ingredient.UnitOfMeasure> unitColumn;
    
    @FXML
    private TableColumn<Ingredient, BigDecimal> stockColumn;
    
    @FXML
    private TableColumn<Ingredient, BigDecimal> minStockColumn;
    
    @FXML
    private TableColumn<Ingredient, BigDecimal> priceColumn;
    
    @FXML
    private TableColumn<Ingredient, BigDecimal> totalValueColumn;
    
    @FXML
    private TableColumn<Ingredient, Void> actionsColumn;
    
    @FXML
    private Pagination pagination;
    
    private ObservableList<Ingredient> ingredientList = FXCollections.observableArrayList();
    
    @FXML
    public void initialize() {
        setupTable();
        loadIngredients();
        updateStatistics();
        System.out.println("Inventory controller initialized");
    }
    
    private void setupTable() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        unitColumn.setCellValueFactory(new PropertyValueFactory<>("unitOfMeasure"));
        stockColumn.setCellValueFactory(param -> 
            new javafx.beans.property.SimpleObjectProperty<>(param.getValue().getCurrentStock()));
        minStockColumn.setCellValueFactory(new PropertyValueFactory<>("minimumStock"));
        priceColumn.setCellValueFactory(param -> 
            new javafx.beans.property.SimpleObjectProperty<>(param.getValue().getLastPurchasePrice()));
        totalValueColumn.setCellValueFactory(param -> 
            new javafx.beans.property.SimpleObjectProperty<>(calculateTotalValue(param.getValue().getCurrentStock(), param.getValue().getLastPurchasePrice())));
        
        stockColumn.setCellFactory(TextFieldTableCell.forTableColumn(new javafx.util.converter.BigDecimalStringConverter()));
        minStockColumn.setCellFactory(TextFieldTableCell.forTableColumn(new javafx.util.converter.BigDecimalStringConverter()));
        priceColumn.setCellFactory(TextFieldTableCell.forTableColumn(new javafx.util.converter.BigDecimalStringConverter()));
        
        stockColumn.setOnEditCommit(event -> {
            Ingredient ingredient = event.getRowValue();
            ingredient.setCurrentStock(event.getNewValue());
            ingredientService.saveIngredient(ingredient);
            updateStatistics();
        });
        
        minStockColumn.setOnEditCommit(event -> {
            Ingredient ingredient = event.getRowValue();
            ingredient.setMinimumStock(event.getNewValue());
            ingredientService.saveIngredient(ingredient);
            updateStatistics();
        });
        
        priceColumn.setOnEditCommit(event -> {
            Ingredient ingredient = event.getRowValue();
            ingredient.setLastPurchasePrice(event.getNewValue());
            ingredientService.saveIngredient(ingredient);
            updateStatistics();
        });
        
        setupActionsColumn();
        
        ingredientsTable.setItems(ingredientList);
        ingredientsTable.setEditable(true);
    }
    
    private void setupActionsColumn() {
        actionsColumn.setCellFactory(param -> new TableCell<>() {
            private final Button editButton = new Button("Editare");
            private final Button deleteButton = new Button("Șterge");
            private final HBox hbox = new HBox(5, editButton, deleteButton);
            
            {
                editButton.getStyleClass().addAll("button", "primary");
                deleteButton.getStyleClass().addAll("button", "danger");
                
                editButton.setOnAction(event -> {
                    Ingredient ingredient = getTableView().getItems().get(getIndex());
                    showEditIngredientDialog(ingredient);
                });
                
                deleteButton.setOnAction(event -> {
                    Ingredient ingredient = getTableView().getItems().get(getIndex());
                    deleteIngredient(ingredient);
                });
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(hbox);
                }
            }
        });
    }
    
    @FXML
    public void searchIngredients() {
        String searchTerm = searchField.getText().trim();
        List<Ingredient> ingredients;
        
        if (searchTerm.isEmpty()) {
            ingredients = ingredientService.getAllIngredients();
        } else {
            ingredients = ingredientService.searchIngredients(searchTerm);
        }
        
        ingredientList.clear();
        ingredientList.addAll(ingredients);
    }
    
    @FXML
    public void showAddIngredientDialog() {
        Dialog<Ingredient> dialog = createIngredientDialog(null);
        dialog.showAndWait().ifPresent(ingredient -> {
            ingredientService.saveIngredient(ingredient);
            loadIngredients();
            updateStatistics();
            showSuccessMessage("Ingredient adăugat cu succes!");
        });
    }
    
    private void showEditIngredientDialog(Ingredient ingredient) {
        Dialog<Ingredient> dialog = createIngredientDialog(ingredient);
        dialog.showAndWait().ifPresent(updatedIngredient -> {
            ingredientService.saveIngredient(updatedIngredient);
            loadIngredients();
            updateStatistics();
            showSuccessMessage("Ingredient actualizat cu succes!");
        });
    }
    
    private Dialog<Ingredient> createIngredientDialog(Ingredient ingredient) {
        Dialog<Ingredient> dialog = new Dialog<>();
        dialog.setTitle(ingredient == null ? "Adaugă Ingredient" : "Editează Ingredient");
        dialog.setHeaderText(null);
        
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));
        
        TextField nameField = new TextField();
        nameField.setPromptText("Nume ingredient");
        
        ComboBox<Ingredient.UnitOfMeasure> unitCombo = new ComboBox<>();
        unitCombo.getItems().addAll(Ingredient.UnitOfMeasure.values());
        
        TextField stockField = new TextField();
        stockField.setPromptText("Stoc curent");
        
        TextField minStockField = new TextField();
        minStockField.setPromptText("Stoc minim");
        
        TextField priceField = new TextField();
        priceField.setPromptText("Preț achiziție");
        
        if (ingredient != null) {
            nameField.setText(ingredient.getName());
            unitCombo.setValue(ingredient.getUnitOfMeasure());
            stockField.setText(ingredient.getCurrentStock().toString());
            minStockField.setText(ingredient.getMinimumStock().toString());
            if (ingredient.getLastPurchasePrice() != null) {
                priceField.setText(ingredient.getLastPurchasePrice().toString());
            }
        }
        
        grid.add(new Label("Nume:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Unitate:"), 0, 1);
        grid.add(unitCombo, 1, 1);
        grid.add(new Label("Stoc curent:"), 0, 2);
        grid.add(stockField, 1, 2);
        grid.add(new Label("Stoc minim:"), 0, 3);
        grid.add(minStockField, 1, 3);
        grid.add(new Label("Preț:"), 0, 4);
        grid.add(priceField, 1, 4);
        
        dialog.getDialogPane().setContent(grid);
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                try {
                    Ingredient newIngredient = ingredient != null ? ingredient : new Ingredient();
                    newIngredient.setName(nameField.getText());
                    newIngredient.setUnitOfMeasure(unitCombo.getValue());
                    newIngredient.setCurrentStock(new BigDecimal(stockField.getText()));
                    newIngredient.setMinimumStock(new BigDecimal(minStockField.getText()));
                    if (!priceField.getText().isEmpty()) {
                        newIngredient.setLastPurchasePrice(new BigDecimal(priceField.getText()));
                    }
                    return newIngredient;
                } catch (NumberFormatException e) {
                    showError("Valori numerice invalide!");
                    return null;
                }
            }
            return null;
        });
        
        return dialog;
    }
    
    private void deleteIngredient(Ingredient ingredient) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmare Ștergere");
        alert.setHeaderText("Sunteți sigur că doriți să ștergeți acest ingredient?");
        alert.setContentText("Ingredient: " + ingredient.getName());
        
        if (alert.showAndWait().get() == ButtonType.OK) {
            ingredientService.deleteIngredient(ingredient.getId());
            loadIngredients();
            updateStatistics();
            showSuccessMessage("Ingredient șters cu succes!");
        }
    }
    
    @FXML
    public void importSPVInvoice() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Selectează fișier XML SPV");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("XML Files", "*.xml")
        );
        
        File file = fileChooser.showOpenDialog(new Stage());
        if (file != null) {
            try {
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
                alert.setTitle("Informații");
                alert.setHeaderText(null);
                alert.setContentText("Import SPV - funcționalitate în dezvoltare");
                alert.show();
            } catch (Exception e) {
                System.err.println("Error importing SPV invoice: " + e.getMessage());
                showError("Eroare la importarea facturii: " + e.getMessage());
            }
        }
    }
    
    private void loadIngredients() {
        List<Ingredient> ingredients = ingredientService.getAllIngredients();
        ingredientList.clear();
        ingredientList.addAll(ingredients);
    }
    
    private void updateStatistics() {
        List<Ingredient> ingredients = ingredientService.getAllIngredients();
        List<Ingredient> lowStock = ingredientService.getLowStockIngredients();
        
        totalIngredientsLabel.setText(String.valueOf(ingredients.size()));
        lowStockLabel.setText(String.valueOf(lowStock.size()));
        
        BigDecimal totalValue = ingredients.stream()
            .filter(ing -> ing.getCurrentStock() != null && ing.getLastPurchasePrice() != null)
            .map(ing -> ing.getCurrentStock().multiply(ing.getLastPurchasePrice()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        totalValueLabel.setText(String.format("%.2f lei", totalValue));
    }
    
    private BigDecimal calculateTotalValue(BigDecimal stock, BigDecimal price) {
        if (stock == null || price == null) {
            return BigDecimal.ZERO;
        }
        return stock.multiply(price).setScale(2, RoundingMode.HALF_UP);
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
