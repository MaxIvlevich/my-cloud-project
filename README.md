Microservices Example: User & Company Management

This project demonstrates a simple microservices application built using Spring Boot and Spring Cloud. It manages basic User and Company entities, showcasing inter-service communication, service discovery, centralized configuration, and an API gateway.

## Features

*   **User Service:** Manages user data (CRUD operations).
*   **Company Service:** Manages company data (CRUD operations) and associations with employees (users).
*   **Data Enrichment:**
    *   User endpoints can optionally return company details.
    *   Company endpoints can optionally return details of associated employees.
*   **Inter-service Communication:** Uses Spring Cloud OpenFeign for type-safe REST calls between services (`Company Service` <-> `User Service`).
*   **Service Discovery:** Uses Spring Cloud Netflix Eureka for service registration and discovery.
*   **Centralized Configuration:** Uses Spring Cloud Config Server to manage externalized configuration for microservices.
*   **API Gateway:** Uses Spring Cloud Gateway as a single entry point for all client requests, routing them to the appropriate microservice.
*   **Containerization:** All services and infrastructure components are containerized using Docker and orchestrated with Docker Compose.

## Architecture Overview

The system consists of the following components:
+---------------------+ +---------------------+ +---------------------+
| |----->| |<-----| |
| API Gateway | | Eureka Server | | Config Server |
| (Spring Cloud Gateway)| | (Service Discovery) | | (Configuration Mgmt)|
| localhost:8080 [] |----->| localhost:8761 | | localhost:8888 |
| | | | | |
+---------^-----------+ +----------^----------+ +----------^----------+
| | |
| routes requests | registers & discovers | provides config
| | |
+---------v-----------+ +----------v----------+ +---------v-----------+
| |----->| |<---->| |
| User Service |<-----| Company Service | | Database(s) |
| (Spring Boot App) | | (Spring Boot App) | | (e.g., PostgreSQL) |
| localhost:8081 [] |----->| localhost:8082 [*] | | |
| | | | | |
+---------------------+ +---------------------+ +---------------------+
| |
+-------------- Feign ------------+
(Inter-service Calls)
*   **Backend:** Java 17+, Spring Boot 3.x
*   **Spring Cloud:** Netflix Eureka (Discovery), Config Server, Spring Cloud Gateway, OpenFeign (Client)
*   **Data:** Spring Data JPA, PostgreSQL (or other configured relational database)
*   **Mapping:** MapStruct
*   **Utils:** Lombok
*   **Build:** Maven 
*   **Containerization:** Docker, Docker Compose

*   All API requests should be made through the **API Gateway**. Assuming the gateway runs on `http://localhost:8080`:

*   **User Service Endpoints:** `http://localhost:8080/api/v1/users`
    *   `GET /api/v1/users` - Get all users
    *   `GET /api/v1/users/{id}` - Get user by ID
    *   `POST /api/v1/users` - Create a new user
    *   `PUT /api/v1/users/{id}` - Update user by ID
    *   `DELETE /api/v1/users/{id}` - Delete user by ID
    *   `GET /api/v1/users/by-ids?ids=1,2,3` - Get multiple users by IDs
    *   `PUT /api/v1/users/{userId}/company` (Body: `companyId` or `null`) - Set/Clear user's company
*   **Company Service Endpoints:** `http://localhost:8080/api/v1/companies`
    *   `GET /api/v1/companies` - Get all companies (with employees)
    *   `GET /api/v1/companies/{id}` - Get company by ID (with employees)
    *   `POST /api/v1/companies` - Create a new company
    *   `PUT /api/v1/companies/{id}` - Update company by ID
    *   `DELETE /api/v1/companies/{id}` - Delete company by ID
    *   `POST /api/v1/companies/{companyId}/employees/{employeeId}` - Add employee to company
    *   `DELETE /api/v1/companies/{companyId}/employees/{employeeId}` - Remove employee from company
    *   `GET /api/v1/companies/by-ids?ids=1,2,3` - Get multiple companies by IDs (simple DTOs)
