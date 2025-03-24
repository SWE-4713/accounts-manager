-- MySQL dump 10.13  Distrib 8.0.41, for Win64 (x86_64)
--
-- Host: localhost    Database: usersdb
-- ------------------------------------------------------
-- Server version8.0.41

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

DROP TABLE IF EXISTS accounts;
CREATE TABLE accounts (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    account_name VARCHAR(255) NOT NULL,
    -- Auto-generated 10-digit account_number computed from the id
    account_number AS RIGHT('0000000000' + CAST(id AS VARCHAR(10)), 10) PERSISTED,
    account_description VARCHAR(500) NULL,
    normal_side VARCHAR(50) NULL,
    account_category VARCHAR(100) NULL,
    account_subcategory VARCHAR(100) NULL,
    -- Removed initial_balance column
    debit DECIMAL(15,2) NOT NULL DEFAULT (0.00),
    credit DECIMAL(15,2) NOT NULL DEFAULT (0.00),
    balance DECIMAL(15,2) NOT NULL DEFAULT (0.00),
    date_added DATETIME NOT NULL DEFAULT (GETDATE()),
    user_id BIGINT NULL,
    account_order INT NULL,
    statement VARCHAR(500) NULL,
    comment NVARCHAR(MAX) NULL,
    active BIT NOT NULL DEFAULT (1),
    CONSTRAINT unique_account_name UNIQUE (account_name)
    -- account_number is computed from id so it will be unique as id is unique.
);
