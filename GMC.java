// ============= BACKEND MODELS =============

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

// ============= USER HIERARCHY =============

abstract class User {
    protected String username;
    protected String password;
    protected String role;
    
    public User(String username, String password, String role) {
        this.username = username;
        this.password = password;
        this.role = role;
    }
    
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getRole() { return role; }
    
    public abstract boolean authenticate(String username, String password);
}

class Admin extends User {
    public Admin(String username, String password) {
        super(username, password, "ADMIN");
    }
    
    @Override
    public boolean authenticate(String username, String password) {
        return this.username.equals(username) && this.password.equals(password);
    }
}

class Supplier extends User {
    public Supplier(String username, String password) {
        super(username, password, "SUPPLIER");
    }
    
    @Override
    public boolean authenticate(String username, String password) {
        return this.username.equals(username) && this.password.equals(password);
    }
}

class Customer extends User {
    private Cart cart;
    
    public Customer(String username, String password) {
        super(username, password, "CUSTOMER");
        this.cart = new Cart();
    }
    
    @Override
    public boolean authenticate(String username, String password) {
        return this.username.equals(username) && this.password.equals(password);
    }
    
    public Cart getCart() { return cart; }
}

// ============= PRODUCT MODEL =============

class Product {
    private IntegerProperty id;
    private StringProperty name;
    private DoubleProperty price;
    private IntegerProperty quantity;
    private StringProperty category;
    
    public Product(int id, String name, double price, int quantity, String category) {
        this.id = new SimpleIntegerProperty(id);
        this.name = new SimpleStringProperty(name);
        this.price = new SimpleDoubleProperty(price);
        this.quantity = new SimpleIntegerProperty(quantity);
        this.category = new SimpleStringProperty(category);
    }
    
    // Property getters for JavaFX TableView
    public IntegerProperty idProperty() { return id; }
    public StringProperty nameProperty() { return name; }
    public DoubleProperty priceProperty() { return price; }
    public IntegerProperty quantityProperty() { return quantity; }
    public StringProperty categoryProperty() { return category; }
    
    // Regular getters
    public int getId() { return id.get(); }
    public String getName() { return name.get(); }
    public double getPrice() { return price.get(); }
    public int getQuantity() { return quantity.get(); }
    public String getCategory() { return category.get(); }
    
    // Setters
    public void setId(int id) { this.id.set(id); }
    public void setName(String name) { this.name.set(name); }
    public void setPrice(double price) { this.price.set(price); }
    public void setQuantity(int quantity) { this.quantity.set(quantity); }
    public void setCategory(String category) { this.category.set(category); }
    
    @Override
    public String toString() {
        return String.format("Product{id=%d, name='%s', price=%.2f, quantity=%d, category='%s'}", 
                           getId(), getName(), getPrice(), getQuantity(), getCategory());
    }
}

// ============= CART SYSTEM =============

class CartItem {
    private Product product;
    private int quantity;
    
    public CartItem(Product product, int quantity) {
        this.product = product;
        this.quantity = quantity;
    }
    
    public Product getProduct() { return product; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public double getTotalPrice() { return product.getPrice() * quantity; }
}

class Cart {
    private Map<Integer, CartItem> items;
    
    public Cart() {
        this.items = new HashMap<>();
    }
    
    public void addItem(Product product, int quantity) {
        if (items.containsKey(product.getId())) {
            CartItem existing = items.get(product.getId());
            existing.setQuantity(existing.getQuantity() + quantity);
        } else {
            items.put(product.getId(), new CartItem(product, quantity));
        }
    }
    
    public void removeItem(int productId) {
        items.remove(productId);
    }
    
    public void updateQuantity(int productId, int quantity) {
        if (items.containsKey(productId)) {
            if (quantity <= 0) {
                removeItem(productId);
            } else {
                items.get(productId).setQuantity(quantity);
            }
        }
    }
    
    public Collection<CartItem> getItems() {
        return items.values();
    }
    
    public double getTotalAmount() {
        return items.values().stream()
                   .mapToDouble(CartItem::getTotalPrice)
                   .sum();
    }
    
    public void clear() {
        items.clear();
    }
    
    public boolean isEmpty() {
        return items.isEmpty();
    }
}

// ============= FACTORY PATTERN =============

class UserFactory {
    public static User createUser(String role, String username, String password) {
        switch (role.toUpperCase()) {
            case "ADMIN":
                return new Admin(username, password);
            case "SUPPLIER":
                return new Supplier(username, password);
            case "CUSTOMER":
                return new Customer(username, password);
            default:
                throw new IllegalArgumentException("Invalid user role: " + role);
        }
    }
}

// ============= SINGLETON DATABASE =============

class Database {
    private static Database instance;
    private Map<String, User> users;
    private Map<Integer, Product> products;
    private int nextProductId;
    
    private Database() {
        users = new ConcurrentHashMap<>();
        products = new ConcurrentHashMap<>();
        nextProductId = 1;
        initializeData();
    }
    
    public static Database getInstance() {
        if (instance == null) {
            synchronized (Database.class) {
                if (instance == null) {
                    instance = new Database();
                }
            }
        }
        return instance;
    }
    
    private void initializeData() {
        // Initialize users
        users.put("admin", UserFactory.createUser("ADMIN", "admin", "admin123"));
        users.put("supplier", UserFactory.createUser("SUPPLIER", "supplier", "supplier123"));
        users.put("customer", UserFactory.createUser("CUSTOMER", "customer", "customer123"));
        users.put("john_doe", UserFactory.createUser("CUSTOMER", "john_doe", "password"));
        
        // Initialize products
        addProduct("Apples", 2.99, 50, "Fruits");
        addProduct("Bananas", 1.49, 30, "Fruits");
        addProduct("Milk", 3.99, 20, "Dairy");
        addProduct("Bread", 2.49, 25, "Bakery");
        addProduct("Chicken Breast", 8.99, 15, "Meat");
        addProduct("Rice", 4.99, 40, "Grains");
        addProduct("Tomatoes", 3.49, 35, "Vegetables");
        addProduct("Cheese", 5.99, 18, "Dairy");
        addProduct("Eggs", 2.99, 22, "Dairy");
        addProduct("Orange Juice", 4.49, 12, "Beverages");
    }
    
    public User authenticateUser(String username, String password) {
        User user = users.get(username);
        if (user != null && user.authenticate(username, password)) {
            return user;
        }
        return null;
    }
    
    public void addUser(User user) {
        users.put(user.getUsername(), user);
    }
    
    public Collection<User> getAllUsers() {
        return users.values();
    }
    
    public Product addProduct(String name, double price, int quantity, String category) {
        Product product = new Product(nextProductId++, name, price, quantity, category);
        products.put(product.getId(), product);
        return product;
    }
    
    public void updateProduct(Product product) {
        products.put(product.getId(), product);
    }
    
    public void deleteProduct(int productId) {
        products.remove(productId);
    }
    
    public Product getProduct(int id) {
        return products.get(id);
    }
    
    public Collection<Product> getAllProducts() {
        return products.values();
    }
    
    public List<Product> getAvailableProducts() {
        return products.values().stream()
                      .filter(p -> p.getQuantity() > 0)
                      .collect(Collectors.toList());
    }
}

// ============= SINGLETON INVENTORY =============

class Inventory {
    private static Inventory instance;
    private Database database;
    
    private Inventory() {
        database = Database.getInstance();
    }
    
    public static Inventory getInstance() {
        if (instance == null) {
            synchronized (Inventory.class) {
                if (instance == null) {
                    instance = new Inventory();
                }
            }
        }
        return instance;
    }
    
    public List<Product> getAllProducts() {
        return new ArrayList<>(database.getAllProducts());
    }
    
    public List<Product> getAvailableProducts() {
        return database.getAvailableProducts();
    }
    
    public Product getProduct(int id) {
        return database.getProduct(id);
    }
    
    public void addProduct(String name, double price, int quantity, String category) {
        database.addProduct(name, price, quantity, category);
    }
    
    public void updateProduct(Product product) {
        database.updateProduct(product);
    }
    
    public void deleteProduct(int productId) {
        database.deleteProduct(productId);
    }
    
    public boolean updateStock(int productId, int newQuantity) {
        Product product = database.getProduct(productId);
        if (product != null) {
            product.setQuantity(newQuantity);
            database.updateProduct(product);
            return true;
        }
        return false;
    }
    
    public boolean decreaseStock(int productId, int quantity) {
        Product product = database.getProduct(productId);
        if (product != null && product.getQuantity() >= quantity) {
            product.setQuantity(product.getQuantity() - quantity);
            database.updateProduct(product);
            return true;
        }
        return false;
    }
    
    public List<Product> getLowStockProducts(int threshold) {
        return database.getAllProducts().stream()
                      .filter(p -> p.getQuantity() <= threshold)
                      .collect(Collectors.toList());
    }
}

// ============= PAYMENT GATEWAY =============

class PaymentGateway {
    public boolean processPayment(double amount, String cardNumber, String expiryDate, String cvv) {
        // Simulate payment processing
        try {
            Thread.sleep(1000); // Simulate processing time
            // Simple validation (for demo purposes)
            return cardNumber.length() >= 13 && !expiryDate.isEmpty() && !cvv.isEmpty() && amount > 0;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
    
    public String generateTransactionId() {
        return "TXN" + System.currentTimeMillis();
    }
}

// ============= REPORT MANAGER =============

class ReportManager {
    private static ReportManager instance;
    
    private ReportManager() {}
    
    public static ReportManager getInstance() {
        if (instance == null) {
            synchronized (ReportManager.class) {
                if (instance == null) {
                    instance = new ReportManager();
                }
            }
        }
        return instance;
    }
    
    public String generateInventoryReport() {
        Inventory inventory = Inventory.getInstance();
        StringBuilder report = new StringBuilder();
        
        report.append("=== INVENTORY REPORT ===\n");
        report.append("Generated: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n\n");
        
        List<Product> products = inventory.getAllProducts();
        report.append("Total Products: ").append(products.size()).append("\n\n");
        
        report.append(String.format("%-5s %-20s %-10s %-10s %-15s%n", "ID", "Name", "Price", "Quantity", "Category"));
        report.append("=".repeat(65)).append("\n");
        
        for (Product product : products) {
            report.append(String.format("%-5d %-20s $%-9.2f %-10d %-15s%n",
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getQuantity(),
                product.getCategory()));
        }
        
        // Low stock alert
        List<Product> lowStockProducts = inventory.getLowStockProducts(10);
        if (!lowStockProducts.isEmpty()) {
            report.append("\n=== LOW STOCK ALERT ===\n");
            for (Product product : lowStockProducts) {
                report.append(String.format("⚠️  %s: %d remaining\n", product.getName(), product.getQuantity()));
            }
        }
        
        return report.toString();
    }
    
    public String generateSalesReport(List<String> transactions) {
        StringBuilder report = new StringBuilder();
        report.append("=== SALES REPORT ===\n");
        report.append("Generated: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n\n");
        
        if (transactions.isEmpty()) {
            report.append("No transactions recorded.\n");
        } else {
            report.append("Recent Transactions:\n");
            for (String transaction : transactions) {
                report.append(transaction).append("\n");
            }
        }
        
        return report.toString();
    }
}

// ============= JAVAFX APPLICATION =============

public class GMC extends Application {
    private Stage primaryStage;
    private User currentUser;
    private List<String> salesTransactions = new ArrayList<>();
    
    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.primaryStage.setTitle("Grocery Management System");
        showLoginScreen();
    }
    
    private void showLoginScreen() {
        VBox loginBox = new VBox(15);
        loginBox.setAlignment(Pos.CENTER);
        loginBox.setPadding(new Insets(30));
        loginBox.setStyle("-fx-background-color: #f0f8ff;");
        
        Label titleLabel = new Label("Grocery Management System");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        
        Label subtitleLabel = new Label("Please login to continue");
        subtitleLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d;");
        
        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        usernameField.setMaxWidth(250);
        
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        passwordField.setMaxWidth(250);
        
        Button loginButton = new Button("Login");
        loginButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold; -fx-pref-width: 100px;");
        
        Label credentialsLabel = new Label("Demo Credentials:\nAdmin: admin/admin123\nSupplier: supplier/supplier123\nCustomer: customer/customer123");
        credentialsLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #95a5a6; -fx-text-alignment: center;");
        
        loginButton.setOnAction(e -> {
            String username = usernameField.getText();
            String password = passwordField.getText();
            
            User user = Database.getInstance().authenticateUser(username, password);
            if (user != null) {
                currentUser = user;
                switch (user.getRole()) {
                    case "ADMIN":
                        showAdminDashboard();
                        break;
                    case "SUPPLIER":
                        showSupplierDashboard();
                        break;
                    case "CUSTOMER":
                        showCustomerDashboard();
                        break;
                }
            } else {
                showAlert("Login Failed", "Invalid username or password!");
            }
        });
        
        passwordField.setOnAction(e -> loginButton.fire());
        
        loginBox.getChildren().addAll(titleLabel, subtitleLabel, usernameField, passwordField, loginButton, credentialsLabel);
        
        Scene scene = new Scene(loginBox, 400, 350);
        primaryStage.setScene(scene);
        primaryStage.show();
    }
    
    private void showAdminDashboard() {
        BorderPane root = new BorderPane();
        
        // Top menu bar
        MenuBar menuBar = createMenuBar();
        root.setTop(menuBar);
        
        // Main content
        TabPane tabPane = new TabPane();
        
        // Inventory Tab
        Tab inventoryTab = new Tab("Inventory Management");
        inventoryTab.setClosable(false);
        inventoryTab.setContent(createInventoryPane());
        
        // Reports Tab
        Tab reportsTab = new Tab("Reports");
        reportsTab.setClosable(false);
        reportsTab.setContent(createReportsPane());
        
        // Users Tab
        Tab usersTab = new Tab("User Management");
        usersTab.setClosable(false);
        usersTab.setContent(createUserManagementPane());
        
        tabPane.getTabs().addAll(inventoryTab, reportsTab, usersTab);
        root.setCenter(tabPane);
        
        Scene scene = new Scene(root, 900, 600);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Admin Dashboard - " + currentUser.getUsername());
    }
    
    private VBox createInventoryPane() {
        VBox inventoryPane = new VBox(10);
        inventoryPane.setPadding(new Insets(15));
        
        // Add product form
        HBox addProductBox = new HBox(10);
        addProductBox.setAlignment(Pos.CENTER_LEFT);
        
        TextField nameField = new TextField();
        nameField.setPromptText("Product Name");
        
        TextField priceField = new TextField();
        priceField.setPromptText("Price");
        
        TextField quantityField = new TextField();
        quantityField.setPromptText("Quantity");
        
        TextField categoryField = new TextField();
        categoryField.setPromptText("Category");
        
        Button addButton = new Button("Add Product");
        addButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
        
        addProductBox.getChildren().addAll(
            new Label("Name:"), nameField,
            new Label("Price:"), priceField,
            new Label("Qty:"), quantityField,
            new Label("Category:"), categoryField,
            addButton
        );
        
        // Products table
        TableView<Product> productsTable = new TableView<>();
        
        TableColumn<Product, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setPrefWidth(50);
        
        TableColumn<Product, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(150);
        
        TableColumn<Product, Double> priceCol = new TableColumn<>("Price");
        priceCol.setCellValueFactory(new PropertyValueFactory<>("price"));
        priceCol.setPrefWidth(80);
        
        TableColumn<Product, Integer> qtyCol = new TableColumn<>("Quantity");
        qtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        qtyCol.setPrefWidth(80);
        
        TableColumn<Product, String> categoryCol = new TableColumn<>("Category");
        categoryCol.setCellValueFactory(new PropertyValueFactory<>("category"));
        categoryCol.setPrefWidth(120);
        
        productsTable.getColumns().addAll(idCol, nameCol, priceCol, qtyCol, categoryCol);
        
        // Load products
        ObservableList<Product> productList = FXCollections.observableArrayList(Inventory.getInstance().getAllProducts());
        productsTable.setItems(productList);
        
        // Control buttons
        HBox controlBox = new HBox(10);
        Button updateButton = new Button("Update Selected");
        updateButton.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white;");
        
        Button deleteButton = new Button("Delete Selected");
        deleteButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
        
        Button refreshButton = new Button("Refresh");
        refreshButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white;");
        
        controlBox.getChildren().addAll(updateButton, deleteButton, refreshButton);
        
        // Event handlers
        addButton.setOnAction(e -> {
            try {
                String name = nameField.getText();
                double price = Double.parseDouble(priceField.getText());
                int quantity = Integer.parseInt(quantityField.getText());
                String category = categoryField.getText();
                
                if (name.isEmpty() || category.isEmpty()) {
                    showAlert("Error", "Please fill in all fields!");
                    return;
                }
                
                Inventory.getInstance().addProduct(name, price, quantity, category);
                productList.setAll(Inventory.getInstance().getAllProducts());
                
                // Clear fields
                nameField.clear();
                priceField.clear();
                quantityField.clear();
                categoryField.clear();
                
                showAlert("Success", "Product added successfully!");
            } catch (NumberFormatException ex) {
                showAlert("Error", "Please enter valid numbers for price and quantity!");
            }
        });
        
        deleteButton.setOnAction(e -> {
            Product selected = productsTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                Inventory.getInstance().deleteProduct(selected.getId());
                productList.remove(selected);
                showAlert("Success", "Product deleted successfully!");
            } else {
                showAlert("Error", "Please select a product to delete!");
            }
        });
        
        refreshButton.setOnAction(e -> {
            productList.setAll(Inventory.getInstance().getAllProducts());
        });
        
        inventoryPane.getChildren().addAll(
            new Label("Add New Product:"),
            addProductBox,
            new Separator(),
            new Label("Current Inventory:"),
            productsTable,
            controlBox
        );
        
        return inventoryPane;
    }
    
    private VBox createReportsPane() {
        VBox reportsPane = new VBox(15);
        reportsPane.setPadding(new Insets(15));
        
        Button inventoryReportBtn = new Button("Generate Inventory Report");
        inventoryReportBtn.setStyle("-fx-background-color: #9b59b6; -fx-text-fill: white; -fx-pref-width: 200px;");
        
        Button salesReportBtn = new Button("Generate Sales Report");
        salesReportBtn.setStyle("-fx-background-color: #e67e22; -fx-text-fill: white; -fx-pref-width: 200px;");
        
        TextArea reportArea = new TextArea();
        reportArea.setEditable(false);
        reportArea.setPrefRowCount(20);
        reportArea.setStyle("-fx-font-family: monospace;");
        
        inventoryReportBtn.setOnAction(e -> {
            String report = ReportManager.getInstance().generateInventoryReport();
            reportArea.setText(report);
        });
        
        salesReportBtn.setOnAction(e -> {
            String report = ReportManager.getInstance().generateSalesReport(salesTransactions);
            reportArea.setText(report);
        });
        
        HBox buttonBox = new HBox(10);
        buttonBox.getChildren().addAll(inventoryReportBtn, salesReportBtn);
        
        reportsPane.getChildren().addAll(
            new Label("Report Generation:"),
            buttonBox,
            new Label("Report Output:"),
            reportArea
        );
        
        return reportsPane;
    }
    
    private VBox createUserManagementPane() {
        VBox userPane = new VBox(15);
        userPane.setPadding(new Insets(15));
        
        ListView<String> usersList = new ListView<>();
        Collection<User> users = Database.getInstance().getAllUsers();
        ObservableList<String> userStrings = FXCollections.observableArrayList();
        
        for (User user : users) {
            userStrings.add(String.format("%s (%s)", user.getUsername(), user.getRole()));
        }
        usersList.setItems(userStrings);
        
        userPane.getChildren().addAll(
            new Label("System Users:"),
            usersList
        );
        
        return userPane;
    }
    
    private void showSupplierDashboard() {
        BorderPane root = new BorderPane();
        root.setTop(createMenuBar());
        
        VBox supplierPane = new VBox(15);
        supplierPane.setPadding(new Insets(15));
        
        Label welcomeLabel = new Label("Welcome, " + currentUser.getUsername() + "!");
        welcomeLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        
        // Low stock products
        Label lowStockLabel = new Label("Products Requiring Restock:");
        lowStockLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        
        ListView<String> lowStockList = new ListView<>();
        List<Product> lowStockProducts = Inventory.getInstance().getLowStockProducts(15);
        ObservableList<String> lowStockStrings = FXCollections.observableArrayList();
        
        for (Product product : lowStockProducts) {
            lowStockStrings.add(String.format("%s - Current Stock: %d", product.getName(), product.getQuantity()));
        }
        lowStockList.setItems(lowStockStrings);
        lowStockList.setPrefHeight(200);
        
        // Update stock form
        HBox updateStockBox = new HBox(10);
        updateStockBox.setAlignment(Pos.CENTER_LEFT);
        
        TextField productIdField = new TextField();
        productIdField.setPromptText("Product ID");
        
        TextField newQuantityField = new TextField();
        newQuantityField.setPromptText("New Quantity");
        
        Button updateStockBtn = new Button("Update Stock");
        updateStockBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
        
        updateStockBox.getChildren().addAll(
            new Label("Product ID:"), productIdField,
            new Label("New Quantity:"), newQuantityField,
            updateStockBtn
        );
        
        updateStockBtn.setOnAction(e -> {
            try {
                int productId = Integer.parseInt(productIdField.getText());
                int newQuantity = Integer.parseInt(newQuantityField.getText());
                
                if (Inventory.getInstance().updateStock(productId, newQuantity)) {
                    showAlert("Success", "Stock updated successfully!");
                    productIdField.clear();
                    newQuantityField.clear();
                    
                    // Refresh low stock list
                    List<Product> updatedLowStock = Inventory.getInstance().getLowStockProducts(15);
                    lowStockStrings.clear();
                    for (Product product : updatedLowStock) {
                        lowStockStrings.add(String.format("%s - Current Stock: %d", product.getName(), product.getQuantity()));
                    }
                } else {
                    showAlert("Error", "Product not found!");
                }
            } catch (NumberFormatException ex) {
                showAlert("Error", "Please enter valid numbers!");
            }
        });
        
        supplierPane.getChildren().addAll(
            welcomeLabel,
            new Separator(),
            lowStockLabel,
            lowStockList,
            new Separator(),
            new Label("Update Stock:"),
            updateStockBox
        );
        
        root.setCenter(supplierPane);
        
        Scene scene = new Scene(root, 800, 500);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Supplier Dashboard - " + currentUser.getUsername());
    }
    
    private void showCustomerDashboard() {
        BorderPane root = new BorderPane();
        root.setTop(createMenuBar());
        
        // Left side - Products
        VBox leftPane = new VBox(10);
        leftPane.setPadding(new Insets(15));
        leftPane.setPrefWidth(500);
        
        Label productsLabel = new Label("Available Products:");
        productsLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        
        TableView<Product> productsTable = new TableView<>();
        
        TableColumn<Product, String> nameCol = new TableColumn<>("Product");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(150);
        
        TableColumn<Product, Double> priceCol = new TableColumn<>("Price");
        priceCol.setCellValueFactory(new PropertyValueFactory<>("price"));
        priceCol.setPrefWidth(80);
        
        TableColumn<Product, Integer> qtyCol = new TableColumn<>("Stock");
        qtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        qtyCol.setPrefWidth(60);
        
        TableColumn<Product, String> categoryCol = new TableColumn<>("Category");
        categoryCol.setCellValueFactory(new PropertyValueFactory<>("category"));
        categoryCol.setPrefWidth(100);
        
        productsTable.getColumns().addAll(nameCol, priceCol, qtyCol, categoryCol);
        
        ObservableList<Product> availableProducts = FXCollections.observableArrayList(
            Inventory.getInstance().getAvailableProducts()
        );
        productsTable.setItems(availableProducts);
        
        // Add to cart controls
        HBox addToCartBox = new HBox(10);
        addToCartBox.setAlignment(Pos.CENTER_LEFT);
        
        TextField quantityField = new TextField("1");
        quantityField.setPrefWidth(60);
        quantityField.setPromptText("Qty");
        
        Button addToCartBtn = new Button("Add to Cart");
        addToCartBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
        
        addToCartBox.getChildren().addAll(
            new Label("Quantity:"), quantityField, addToCartBtn
        );
        
        leftPane.getChildren().addAll(productsLabel, productsTable, addToCartBox);
        
        // Right side - Cart
        VBox rightPane = new VBox(10);
        rightPane.setPadding(new Insets(15));
        rightPane.setPrefWidth(300);
        rightPane.setStyle("-fx-background-color: #ecf0f1;");
        
        Label cartLabel = new Label("Shopping Cart:");
        cartLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        
        ListView<String> cartList = new ListView<>();
        cartList.setPrefHeight(200);
        
        Label totalLabel = new Label("Total: $0.00");
        totalLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        
        Button clearCartBtn = new Button("Clear Cart");
        clearCartBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
        
        Button checkoutBtn = new Button("Checkout");
        checkoutBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold;");
        
        HBox cartButtonsBox = new HBox(10);
        cartButtonsBox.getChildren().addAll(clearCartBtn, checkoutBtn);
        
        rightPane.getChildren().addAll(cartLabel, cartList, totalLabel, cartButtonsBox);
        
        // Event handlers
        Customer customer = (Customer) currentUser;
        
        addToCartBtn.setOnAction(e -> {
            Product selectedProduct = productsTable.getSelectionModel().getSelectedItem();
            if (selectedProduct != null) {
                try {
                    int quantity = Integer.parseInt(quantityField.getText());
                    if (quantity <= 0) {
                        showAlert("Error", "Quantity must be greater than 0!");
                        return;
                    }
                    
                    if (quantity > selectedProduct.getQuantity()) {
                        showAlert("Error", "Not enough stock available!");
                        return;
                    }
                    
                    customer.getCart().addItem(selectedProduct, quantity);
                    updateCartDisplay(cartList, totalLabel, customer.getCart());
                    quantityField.setText("1");
                    showAlert("Success", "Item added to cart!");
                    
                } catch (NumberFormatException ex) {
                    showAlert("Error", "Please enter a valid quantity!");
                }
            } else {
                showAlert("Error", "Please select a product!");
            }
        });
        
        clearCartBtn.setOnAction(e -> {
            customer.getCart().clear();
            updateCartDisplay(cartList, totalLabel, customer.getCart());
        });
        
        checkoutBtn.setOnAction(e -> {
            if (customer.getCart().isEmpty()) {
                showAlert("Error", "Cart is empty!");
            } else {
                showCheckoutDialog(customer.getCart());
            }
        });
        
        // Initial cart display update
        updateCartDisplay(cartList, totalLabel, customer.getCart());
        
        root.setLeft(leftPane);
        root.setRight(rightPane);
        
        Scene scene = new Scene(root, 850, 600);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Customer Dashboard - " + currentUser.getUsername());
    }
    
    private void updateCartDisplay(ListView<String> cartList, Label totalLabel, Cart cart) {
        ObservableList<String> cartItems = FXCollections.observableArrayList();
        
        for (CartItem item : cart.getItems()) {
            cartItems.add(String.format("%s x%d - $%.2f", 
                item.getProduct().getName(), 
                item.getQuantity(), 
                item.getTotalPrice()));
        }
        
        cartList.setItems(cartItems);
        totalLabel.setText(String.format("Total: $%.2f", cart.getTotalAmount()));
    }
    
    private void showCheckoutDialog(Cart cart) {
        Stage checkoutStage = new Stage();
        checkoutStage.setTitle("Checkout");
        checkoutStage.initOwner(primaryStage);
        
        VBox checkoutPane = new VBox(15);
        checkoutPane.setPadding(new Insets(20));
        
        Label titleLabel = new Label("Checkout");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        
        // Order summary
        Label summaryLabel = new Label("Order Summary:");
        summaryLabel.setStyle("-fx-font-weight: bold;");
        
        TextArea summaryArea = new TextArea();
        summaryArea.setEditable(false);
        summaryArea.setPrefRowCount(5);
        
        StringBuilder summary = new StringBuilder();
        for (CartItem item : cart.getItems()) {
            summary.append(String.format("%s x%d - $%.2f\n", 
                item.getProduct().getName(), 
                item.getQuantity(), 
                item.getTotalPrice()));
        }
        summary.append(String.format("\nTotal Amount: $%.2f", cart.getTotalAmount()));
        summaryArea.setText(summary.toString());
        
        // Payment form
        Label paymentLabel = new Label("Payment Information:");
        paymentLabel.setStyle("-fx-font-weight: bold;");
        
        GridPane paymentGrid = new GridPane();
        paymentGrid.setHgap(10);
        paymentGrid.setVgap(10);
        
        TextField cardNumberField = new TextField();
        cardNumberField.setPromptText("Card Number");
        
        TextField expiryField = new TextField();
        expiryField.setPromptText("MM/YY");
        
        TextField cvvField = new TextField();
        cvvField.setPromptText("CVV");
        
        paymentGrid.add(new Label("Card Number:"), 0, 0);
        paymentGrid.add(cardNumberField, 1, 0);
        paymentGrid.add(new Label("Expiry Date:"), 0, 1);
        paymentGrid.add(expiryField, 1, 1);
        paymentGrid.add(new Label("CVV:"), 0, 2);
        paymentGrid.add(cvvField, 1, 2);
        
        // Buttons
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);
        
        Button payButton = new Button("Pay Now");
        payButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold;");
        
        Button cancelButton = new Button("Cancel");
        cancelButton.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white;");
        
        buttonBox.getChildren().addAll(payButton, cancelButton);
        
        // Event handlers
        payButton.setOnAction(e -> {
            String cardNumber = cardNumberField.getText();
            String expiry = expiryField.getText();
            String cvv = cvvField.getText();
            
            if (cardNumber.isEmpty() || expiry.isEmpty() || cvv.isEmpty()) {
                showAlert("Error", "Please fill in all payment fields!");
                return;
            }
            
            PaymentGateway paymentGateway = new PaymentGateway();
            boolean paymentSuccess = paymentGateway.processPayment(cart.getTotalAmount(), cardNumber, expiry, cvv);
            
            if (paymentSuccess) {
                // Process the order
                processOrder(cart);
                String transactionId = paymentGateway.generateTransactionId();
                
                // Record transaction
                String transaction = String.format("Transaction %s - Customer: %s - Amount: $%.2f - Date: %s",
                    transactionId,
                    currentUser.getUsername(),
                    cart.getTotalAmount(),
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
                salesTransactions.add(transaction);
                
                showAlert("Success", "Payment successful!\nTransaction ID: " + transactionId);
                cart.clear();
                checkoutStage.close();
                
                // Refresh customer dashboard
                showCustomerDashboard();
            } else {
                showAlert("Error", "Payment failed! Please check your payment details.");
            }
        });
        
        cancelButton.setOnAction(e -> checkoutStage.close());
        
        checkoutPane.getChildren().addAll(
            titleLabel,
            summaryLabel,
            summaryArea,
            paymentLabel,
            paymentGrid,
            buttonBox
        );
        
        Scene checkoutScene = new Scene(checkoutPane, 400, 500);
        checkoutStage.setScene(checkoutScene);
        checkoutStage.showAndWait();
    }
    
    private void processOrder(Cart cart) {
        // Decrease inventory for each item in the cart
        for (CartItem item : cart.getItems()) {
            Inventory.getInstance().decreaseStock(item.getProduct().getId(), item.getQuantity());
        }
    }
    
    private MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar();
        
        Menu fileMenu = new Menu("File");
        MenuItem logoutItem = new MenuItem("Logout");
        MenuItem exitItem = new MenuItem("Exit");
        
        logoutItem.setOnAction(e -> showLoginScreen());
        exitItem.setOnAction(e -> Platform.exit());
        
        fileMenu.getItems().addAll(logoutItem, new SeparatorMenuItem(), exitItem);
        
        Menu helpMenu = new Menu("Help");
        MenuItem aboutItem = new MenuItem("About");
        aboutItem.setOnAction(e -> showAlert("About", "Grocery Management System v1.0\nBuilt with JavaFX and OOAD principles"));
        
        helpMenu.getItems().add(aboutItem);
        
        menuBar.getMenus().addAll(fileMenu, helpMenu);
        return menuBar;
    }
    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
