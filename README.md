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
