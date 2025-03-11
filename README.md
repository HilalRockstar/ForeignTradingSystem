# ForeignTradingSystem
# MY SQL DATABASE CREATION

CREATE DATABASE foreign_trading;
USE foreign_trading;

CREATE TABLE users (
    id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    balance DOUBLE DEFAULT 1000.0
);

CREATE TABLE transactions (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT,
    currency VARCHAR(10),
    amount DOUBLE,
    price DOUBLE,
    transaction_type ENUM('BUY', 'SELL'),
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);
