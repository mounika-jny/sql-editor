# DS Editor

A full-stack database SQL editor application built with Spring Boot and React.

## 📋 Overview

DS Editor is a web-based SQL editor that allows you to execute SQL queries against various databases. The application features a modern React frontend and a robust Spring Boot backend with support for multiple database systems.

## 🛠️ Technology Stack

### Backend
- **Java 21**
- **Spring Boot 3.5.7**
- **Spring Data JPA**
- **Spring MVC**
- **Lombok**
- **SpringDoc OpenAPI 2.6.0** (Swagger UI)
- **Maven** (Build tool)

### Frontend
- **React 18.2.0**
- **React DOM 18.2.0**
- **Axios 1.6.0**
- **React Scripts 5.0.1**
- **npm** (Package manager)

### Supported Databases
- **H2 Database** (In-memory, for development)
- **PostgreSQL** (Production)

## 📦 Prerequisites

Before you begin, ensure you have the following installed:

- **Java Development Kit (JDK) 21** or higher
  ```bash
  java -version
  ```

- **Maven 3.6+** (or use the included Maven wrapper)
  ```bash
  mvn -version
  ```

- **Node.js 20.11.0** and **npm 10.2.4** (or compatible versions)
  ```bash
  node -v
  npm -v
  ```
  > Note: The frontend-maven-plugin will automatically install Node.js and npm locally if not found

- **PostgreSQL** (optional, only if using PostgreSQL profile)
  ```bash
  psql --version
  ```

## 🚀 Quick Start

Get DS Editor up and running in 3 simple steps:

### 1. Clone the Repository
