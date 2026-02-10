# 🏛️ Online Bidding System - Backend

A comprehensive Spring Boot backend application for managing college stall bidding events. This system enables students to participate in real-time bidding for college stalls, with robust authentication, authorization, and administrative controls.

## 📋 Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Technology Stack](#technology-stack)
- [Architecture](#architecture)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Configuration](#configuration)
- [Database Setup](#database-setup)
- [Running the Application](#running-the-application)
- [API Documentation](#api-documentation)
- [WebSocket Configuration](#websocket-configuration)
- [Security](#security)
- [Project Structure](#project-structure)
- [Testing](#testing)
- [Troubleshooting](#troubleshooting)
- [Contributing](#contributing)
- [License](#license)

## 🎯 Overview

The Online Bidding System is a full-featured backend application designed for educational institutions to manage stall bidding events. It provides:

- **User Management**: Student registration, authentication, and profile management
- **Bidder Applications**: Students can apply to become bidders with admin approval workflow
- **Stall Management**: Admin-controlled creation and management of auction items
- **Real-time Bidding**: WebSocket-powered live bidding with instant updates
- **Admin Dashboard**: Comprehensive statistics and management interface
- **Email Notifications**: Automated emails for verification, approvals, and bidding updates

## ✨ Features

### 👤 User Management
- ✅ Local registration with email verification (OTP-based)
- ✅ Google OAuth2 authentication integration
- ✅ JWT-based stateless authentication
- ✅ Role-based access control (USER, BIDDER, ADMIN)
- ✅ Profile management with image upload
- ✅ Password reset functionality

### 🎫 Bidder Application System
- ✅ Students can apply to become bidders
- ✅ Admin approval workflow (PENDING → APPROVED/REJECTED)
- ✅ Email notifications on status changes
- ✅ Application history tracking

### 🏪 Stall Management
- ✅ CRUD operations for stalls (Admin only)
- ✅ Multiple stall statuses: AVAILABLE, ACTIVE, CLOSED, SOLD
- ✅ Image upload for stalls
- ✅ Base price and original price configuration
- ✅ Bidding time window management
- ✅ Auto-close when original price is reached

### 💰 Bidding System
- ✅ Real-time bidding via REST API and WebSocket
- ✅ Live bid notifications to all participants
- ✅ Bid history and tracking
- ✅ Highest bid tracking per stall
- ✅ Winner declaration by admin
- ✅ Viewer count tracking
- ✅ Bid validation (minimum increments, timing, permissions)

### 💬 Comments System
- ✅ Comment on stalls
- ✅ Threaded replies (parent-child comments)
- ✅ Edit and delete own comments
- ✅ Real-time comment updates

### 📊 Admin Dashboard
- ✅ Real-time statistics (users, bidders, stalls, bids)
- ✅ Application management interface
- ✅ User management and role assignment
- ✅ Stall lifecycle control (start/stop bidding)
- ✅ Winner declaration and results management

### 📧 Email Notifications
- ✅ Email verification with OTP
- ✅ Bidder application status updates
- ✅ Bidding notifications
- ✅ Winner announcements
- ✅ Integrated with Brevo (SendGrid alternative)

## 🛠️ Technology Stack

### Core Framework
- **Spring Boot**: 3.5.3
- **Java**: 17 (LTS)
- **Maven**: Build and dependency management

### Database & ORM
- **MySQL**: Relational database
- **Spring Data JPA**: Data persistence
- **Hibernate**: ORM implementation

### Security
- **Spring Security**: 6.5.4
- **JWT (JSON Web Tokens)**: Stateless authentication
- **OAuth2**: Google authentication
- **BCrypt**: Password encryption

### Real-time Communication
- **Spring WebSocket**: Real-time bidding
- **STOMP Protocol**: Message broker
- **SockJS**: WebSocket fallback

### Email Service
- **Spring Mail**: Email framework
- **Brevo API**: Email delivery service

### Additional Libraries
- **Lombok**: Reduce boilerplate code
- **Jakarta Validation**: Request validation
- **Nimbus JOSE JWT**: JWT processing
- **JJWT**: JWT implementation

## 🏗️ Architecture

The application follows a **layered architecture** pattern:

```
┌─────────────────────────────────────────┐
│          REST API Layer                  │
│     (Controllers - @RestController)      │
└─────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────┐
│         Service Layer                    │
│    (Business Logic - @Service)           │
└─────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────┐
│       Repository Layer                   │
│   (Data Access - @Repository)            │
└─────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────┐
│          Database Layer                  │
│            (MySQL)                       │
└─────────────────────────────────────────┘
```

### Key Components:
- **Controllers**: Handle HTTP requests and responses
- **Services**: Implement business logic and orchestration
- **Repositories**: Data access and persistence
- **Entities**: JPA entities representing database tables
- **DTOs**: Data Transfer Objects for API communication
- **Security**: JWT filters, OAuth2 handlers, security configuration
- **Config**: Application configuration and beans

## 📦 Prerequisites

Before running this application, ensure you have:

- **Java 17** or higher ([Download](https://adoptium.net/))
- **Maven 3.6+** (or use included Maven wrapper)
- **MySQL 8.0+** ([Download](https://dev.mysql.com/downloads/mysql/))
- **Git** for version control
- **IDE** (IntelliJ IDEA, Eclipse, or VS Code recommended)

### Optional
- **Postman** or **Insomnia** for API testing
- **MySQL Workbench** for database management

## 🚀 Installation

### 1. Clone the Repository

```bash
git clone https://github.com/Saivardhan190/Online-Bidding-System.git
cd Online-Bidding-System
```

### 2. Install Dependencies

Using Maven wrapper (recommended):
```bash
./mvnw clean install
```

Or using Maven:
```bash
mvn clean install
```

## ⚙️ Configuration

### Environment Variables

Create environment variables or update `application.properties`:

#### Database Configuration
```bash
export DB_USERNAME=root
export DB_PASSWORD=your_mysql_password
```

#### JWT Configuration
```bash
export JWT_SECRET=your-secret-key-min-256-bits-long
```

#### Google OAuth2 Configuration
```bash
export GOOGLE_CLIENT_ID=your-google-client-id
export GOOGLE_CLIENT_SECRET=your-google-client-secret
export GOOGLE_REDIRECT_URI=http://localhost:8080/login/oauth2/code/google
```

#### Email Configuration (Brevo)
```bash
export BREVO_API_KEY=your-brevo-api-key
export BREVO_FROM_EMAIL=noreply@yourdomain.com
```

#### Admin Configuration
```bash
export ADMIN_DEFAULT_EMAIL=admin@example.com
export ADMIN_DEFAULT_PASSWORD=Admin@123
```

### Application Properties

The `src/main/resources/application.properties` file contains:

```properties
# Application Name
spring.application.name=online_bidding_system

# Database Configuration
spring.datasource.url=jdbc:mysql://localhost:3306/online_bidding_system
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JPA/Hibernate Configuration
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.properties.hibernate.jdbc.time_zone=Asia/Kolkata

# JWT Configuration
jwt.secret=${JWT_SECRET}
jwt.expiration=86400000  # 24 hours

# OAuth2 Configuration
spring.security.oauth2.client.registration.google.client-id=${GOOGLE_CLIENT_ID}
spring.security.oauth2.client.registration.google.client-secret=${GOOGLE_CLIENT_SECRET}
spring.security.oauth2.client.registration.google.scope=email,profile
spring.security.oauth2.client.registration.google.redirect-uri=${GOOGLE_REDIRECT_URI}

# Email Configuration
brevo.api.key=${BREVO_API_KEY}
brevo.from.email=${BREVO_FROM_EMAIL}
brevo.from.name=College Bidding System

# Admin Configuration
admin.default.email=${ADMIN_DEFAULT_EMAIL}
admin.default.password=${ADMIN_DEFAULT_PASSWORD}
admin.default.name=System Administrator

# Frontend URL (CORS)
app.frontend.url=http://localhost:4200

# Logging
logging.level.org.springframework=INFO
logging.level.com.application.example=DEBUG
```

## 🗄️ Database Setup

### 1. Create Database

Connect to MySQL and create the database:

```sql
mysql -u root -p

CREATE DATABASE online_bidding_system;

USE online_bidding_system;
```

### 2. Database Schema

The application uses Hibernate's `ddl-auto=update` to automatically create tables. The following entities will be created:

#### Core Tables:
- **users**: User accounts with authentication details
- **stalls**: Auction items/stalls
- **bid**: Bidding history
- **bidder_application**: Bidder approval requests
- **bidding_result**: Auction results and winners
- **comment**: User comments on stalls
- **email_otp**: Email verification codes
- **application_settings**: System configuration

### 3. Initial Data

On first run, the application automatically creates:
- Default admin account (from environment variables)
- System settings

## 🏃 Running the Application

### Development Mode

Using Maven wrapper:
```bash
./mvnw spring-boot:run
```

Or using Maven:
```bash
mvn spring-boot:run
```

Using Java:
```bash
./mvnw clean package
java -jar target/online_bidding_system-0.0.1-SNAPSHOT.jar
```

### Production Mode

```bash
./mvnw clean package -DskipTests
java -jar target/online_bidding_system-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

### Server Information

Once started, the application will be available at:
- **API Base URL**: `http://localhost:8080`
- **WebSocket Endpoint**: `http://localhost:8080/ws-bidding`

### Verify Application

Check if the application is running:
```bash
curl http://localhost:8080/api/auth/
```

## 📚 API Documentation

### Base URL
```
http://localhost:8080/api
```

### Authentication Endpoints

#### Register User
```http
POST /api/auth/signup
Content-Type: application/json

{
  "studentName": "John Doe",
  "studentEmail": "john@example.com",
  "password": "Password123!",
  "collageId": "STU001",
  "department": "Computer Science",
  "year": 2
}
```

#### Login
```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "john@example.com",
  "password": "Password123!"
}

Response:
{
  "success": true,
  "message": "Login successful",
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "user": { ... }
}
```

#### Verify OTP
```http
POST /api/auth/verify-otp?email=john@example.com&otp=123456
```

#### Get Current User
```http
GET /api/auth/me
Authorization: Bearer <token>
```

### Stall Endpoints

#### Get All Stalls
```http
GET /api/stalls
```

#### Get Stall by ID
```http
GET /api/stalls/{stallId}
```

#### Get Active Stalls
```http
GET /api/stalls/active
```

#### Create Stall (Admin Only)
```http
POST /api/stalls
Authorization: Bearer <admin-token>
Content-Type: application/json

{
  "stallNo": 101,
  "stallName": "Food Stall A",
  "description": "Prime location food stall",
  "category": "Food",
  "location": "Main Building",
  "basePrice": 5000,
  "originalPrice": 15000,
  "maxBidders": 10,
  "biddingStart": "2024-03-01T10:00:00",
  "biddingEnd": "2024-03-01T18:00:00"
}
```

#### Update Stall (Admin Only)
```http
PUT /api/stalls/{stallId}
Authorization: Bearer <admin-token>
Content-Type: application/json
```

#### Delete Stall (Admin Only)
```http
DELETE /api/stalls/{stallId}
Authorization: Bearer <admin-token>
```

#### Start Bidding (Admin Only)
```http
POST /api/stalls/{stallId}/start-bidding
Authorization: Bearer <admin-token>
```

#### Stop Bidding (Admin Only)
```http
POST /api/stalls/{stallId}/stop-bidding
Authorization: Bearer <admin-token>
```

### Bidding Endpoints

#### Place Bid (Bidder Only)
```http
POST /api/bids/place
Authorization: Bearer <bidder-token>
Content-Type: application/json

{
  "stallId": 1,
  "biddedPrice": 10000
}
```

#### Get Bids for Stall
```http
GET /api/bids/stall/{stallId}
```

#### Get Bid History
```http
GET /api/bids/stall/{stallId}/history
```

#### Get Highest Bid
```http
GET /api/bids/stall/{stallId}/highest
```

#### Get My Bids
```http
GET /api/bids/my-bids
Authorization: Bearer <token>
```

#### Declare Winner (Admin Only)
```http
POST /api/bids/stall/{stallId}/declare-winner
Authorization: Bearer <admin-token>
```

### Admin Endpoints

#### Get Dashboard Statistics
```http
GET /api/admin/stats
Authorization: Bearer <admin-token>

Response:
{
  "totalUsers": 150,
  "totalBidders": 45,
  "totalStalls": 20,
  "activeStalls": 8,
  "pendingApplications": 12,
  "approvedApplications": 45,
  "rejectedApplications": 8,
  "totalBids": 234
}
```

#### Get Pending Applications
```http
GET /api/admin/applications/pending
Authorization: Bearer <admin-token>
```

#### Approve Application
```http
POST /api/admin/applications/{applicationId}/approve
Authorization: Bearer <admin-token>
```

#### Reject Application
```http
POST /api/admin/applications/{applicationId}/reject
Authorization: Bearer <admin-token>
Content-Type: application/json

{
  "reason": "Incomplete information"
}
```

#### Get All Users
```http
GET /api/admin/users
Authorization: Bearer <admin-token>
```

### Profile Endpoints

#### Get Profile
```http
GET /api/profile
Authorization: Bearer <token>
```

#### Update Profile
```http
PUT /api/profile
Authorization: Bearer <token>
Content-Type: application/json

{
  "studentName": "John Doe",
  "phone": "1234567890",
  "address": "123 Main St",
  "department": "Computer Science",
  "year": 3
}
```

#### Upload Profile Picture
```http
POST /api/profile/picture
Authorization: Bearer <token>
Content-Type: multipart/form-data

file: <image-file>
```

### Bidder Application Endpoints

#### Apply as Bidder
```http
POST /api/bidder-applications/apply
Authorization: Bearer <token>
Content-Type: application/json

{
  "phoneNumber": "1234567890",
  "reason": "I want to start a food business",
  "preferredStallCategory": "Food"
}
```

#### Get Application Status
```http
GET /api/bidder-applications/my-application
Authorization: Bearer <token>
```

### Comment Endpoints

#### Get Comments for Stall
```http
GET /api/comments/stall/{stallId}
```

#### Add Comment
```http
POST /api/comments
Authorization: Bearer <token>
Content-Type: application/json

{
  "stallId": 1,
  "content": "Great location!",
  "parentCommentId": null
}
```

#### Update Comment
```http
PUT /api/comments/{commentId}
Authorization: Bearer <token>
Content-Type: application/json

{
  "content": "Updated comment text"
}
```

#### Delete Comment
```http
DELETE /api/comments/{commentId}
Authorization: Bearer <token>
```

## 🔌 WebSocket Configuration

### Connection Setup

#### Endpoint
```
ws://localhost:8080/ws-bidding
```

#### Using SockJS (Recommended)
```javascript
// JavaScript/TypeScript example
const socket = new SockJS('http://localhost:8080/ws-bidding');
const stompClient = Stomp.over(socket);

stompClient.connect({}, (frame) => {
  console.log('Connected: ' + frame);
  
  // Subscribe to stall updates
  stompClient.subscribe('/topic/stall/1/bids', (message) => {
    const bid = JSON.parse(message.body);
    console.log('New bid:', bid);
  });
});
```

### WebSocket Topics

#### Subscribe to Stall Bid Updates
```
Topic: /topic/stall/{stallId}/bids
Message Format:
{
  "type": "NEW_BID",
  "stallId": 1,
  "stallName": "Food Stall A",
  "currentHighestBid": 10000,
  "highestBidderName": "John Doe",
  "highestBidderId": 123,
  "timestamp": "2024-03-01T10:30:00",
  "message": "New bid of ₹10000 by John Doe"
}
```

#### Subscribe to Viewer Count
```
Topic: /topic/stall/{stallId}/viewers
Message Format: Integer (viewer count)
```

#### Subscribe to User Activity
```
Topic: /topic/stall/{stallId}/users
Message Format: String (activity message)
```

### Sending Messages

#### Place Bid via WebSocket
```
Destination: /app/bid/place
Message:
{
  "stallId": 1,
  "bidderId": 123,
  "biddedPrice": 10000
}
```

#### Join Stall Room
```
Destination: /app/stall/{stallId}/join
Message: "username"
```

#### Leave Stall Room
```
Destination: /app/stall/{stallId}/leave
Message: "username"
```

## 🔐 Security

### Authentication Flow

1. **User Registration**: User signs up with email and password
2. **Email Verification**: OTP sent to email, user verifies
3. **Login**: User authenticates with credentials
4. **Token Generation**: Server generates JWT token
5. **Token Usage**: Client includes token in Authorization header
6. **Token Validation**: Server validates token on each request

### JWT Token Structure

```
Header:
{
  "alg": "HS256",
  "typ": "JWT"
}

Payload:
{
  "sub": "user@example.com",
  "role": "BIDDER",
  "iat": 1234567890,
  "exp": 1234654290
}
```

### Authorization Levels

| Role | Permissions |
|------|------------|
| **USER** | View stalls, apply for bidder, manage profile |
| **BIDDER** | All USER permissions + place bids, view bid history |
| **ADMIN** | All permissions + manage stalls, approve applications, declare winners |

### CORS Configuration

Configured to allow requests from:
- `http://localhost:4200` (Angular default)

### Security Best Practices

✅ Passwords hashed with BCrypt  
✅ JWT tokens with expiration  
✅ Role-based access control  
✅ HTTPS recommended for production  
✅ SQL injection prevention via JPA  
✅ XSS protection with Spring Security  
✅ CSRF disabled for stateless API  

## 📁 Project Structure

```
src/
├── main/
│   ├── java/
│   │   └── com/application/example/online_bidding_system/
│   │       ├── config/                    # Configuration classes
│   │       │   ├── AdminDataLoader.java   # Initial admin setup
│   │       │   ├── SecurityConfig.java    # Security configuration
│   │       │   ├── WebConfig.java         # Web configuration
│   │       │   └── WebSocketConfig.java   # WebSocket configuration
│   │       │
│   │       ├── controller/                # REST Controllers
│   │       │   ├── AdminController.java
│   │       │   ├── AuthController.java
│   │       │   ├── BidController.java
│   │       │   ├── BidWebSocketController.java
│   │       │   ├── BidderApplicationController.java
│   │       │   ├── BiddingResultController.java
│   │       │   ├── CommentController.java
│   │       │   ├── EmailVerificationController.java
│   │       │   ├── ProfileController.java
│   │       │   └── StallController.java
│   │       │
│   │       ├── dto/                       # Data Transfer Objects
│   │       │   ├── email/                 # Email DTOs
│   │       │   ├── request/               # Request DTOs
│   │       │   ├── response/              # Response DTOs
│   │       │   └── websocket/             # WebSocket DTOs
│   │       │
│   │       ├── entity/                    # JPA Entities
│   │       │   ├── ApplicationSettings.java
│   │       │   ├── ApplicationStatus.java
│   │       │   ├── AuthProvider.java
│   │       │   ├── Bid.java
│   │       │   ├── BidderApplication.java
│   │       │   ├── BiddingResult.java
│   │       │   ├── Comment.java
│   │       │   ├── EmailOtp.java
│   │       │   ├── Role.java
│   │       │   ├── Stall.java
│   │       │   ├── StallStatus.java
│   │       │   ├── Status.java
│   │       │   └── User.java
│   │       │
│   │       ├── exception/                 # Exception handling
│   │       │   ├── BadRequestException.java
│   │       │   ├── GlobalExceptionHandler.java
│   │       │   ├── ResourceNotFoundException.java
│   │       │   └── UnauthorizedException.java
│   │       │
│   │       ├── repository/                # Spring Data Repositories
│   │       │   ├── ApplicationSettingsRepository.java
│   │       │   ├── BidRepository.java
│   │       │   ├── BidderApplicationRepository.java
│   │       │   ├── BiddingResultRepository.java
│   │       │   ├── CommentRepository.java
│   │       │   ├── EmailOtpRepository.java
│   │       │   ├── StallRepository.java
│   │       │   └── UserRepository.java
│   │       │
│   │       ├── security/                  # Security components
│   │       │   ├── JwtAuthenticationFilter.java
│   │       │   ├── JwtUtils.java
│   │       │   └── OAuth2SuccessHandler.java
│   │       │
│   │       ├── service/                   # Business logic
│   │       │   ├── AuthService.java
│   │       │   ├── BidService.java
│   │       │   ├── BidderApplicationService.java
│   │       │   ├── BidderApplicationServiceImpl.java
│   │       │   ├── BiddingResultService.java
│   │       │   ├── CustomOAuth2UserService.java
│   │       │   ├── Emailservice.java
│   │       │   ├── NotificationService.java
│   │       │   └── StallService.java
│   │       │
│   │       └── OnlineBiddingSystemApplication.java
│   │
│   └── resources/
│       ├── application.properties         # Application configuration
│       └── static/                        # Static resources
│
└── test/
    └── java/
        └── com/application/example/online_bidding_system/
            └── OnlineBiddingSystemApplicationTests.java
```

## 🧪 Testing

### Running Tests

Run all tests:
```bash
./mvnw test
```

Run specific test class:
```bash
./mvnw test -Dtest=OnlineBiddingSystemApplicationTests
```

### Test Coverage

Run tests with coverage:
```bash
./mvnw test jacoco:report
```

### Manual Testing with Postman

1. Import the API collection (if available)
2. Set up environment variables:
   - `baseUrl`: `http://localhost:8080`
   - `token`: JWT token from login
3. Test authentication flow:
   - Register → Verify OTP → Login
4. Test bidding flow:
   - Apply as bidder → Admin approves → Place bid

### Testing WebSocket

Use a WebSocket client like:
- **Postman** (WebSocket tab)
- **websocat** (CLI tool)
- Browser console with SockJS/STOMP

```javascript
// Browser console example
const socket = new SockJS('http://localhost:8080/ws-bidding');
const stompClient = Stomp.over(socket);
stompClient.connect({}, () => {
  stompClient.subscribe('/topic/stall/1/bids', (msg) => {
    console.log(JSON.parse(msg.body));
  });
});
```

## 🐛 Troubleshooting

### Common Issues

#### 1. Database Connection Error
```
Error: Unable to connect to MySQL
```
**Solution**: 
- Verify MySQL is running: `sudo service mysql status`
- Check credentials in environment variables
- Ensure database exists: `CREATE DATABASE online_bidding_system;`

#### 2. Port Already in Use
```
Error: Port 8080 is already in use
```
**Solution**:
- Change port in `application.properties`: `server.port=8081`
- Or kill process using port 8080:
  ```bash
  lsof -ti:8080 | xargs kill -9
  ```

#### 3. JWT Token Invalid
```
Error: 401 Unauthorized
```
**Solution**:
- Check token expiration (24 hours by default)
- Verify token format: `Bearer <token>`
- Ensure JWT_SECRET is set correctly

#### 4. Email Not Sending
```
Error: Failed to send email
```
**Solution**:
- Verify Brevo API key is valid
- Check API rate limits
- Verify sender email is verified in Brevo

#### 5. WebSocket Connection Failed
```
Error: WebSocket connection failed
```
**Solution**:
- Verify WebSocket endpoint: `/ws-bidding`
- Check CORS configuration
- Ensure SockJS fallback is enabled

### Debug Mode

Enable debug logging:
```properties
logging.level.com.application.example=DEBUG
logging.level.org.springframework.security=DEBUG
logging.level.org.hibernate.SQL=DEBUG
```

### Health Check

Create a simple health endpoint:
```bash
curl http://localhost:8080/actuator/health
```

## 🤝 Contributing

### Development Workflow

1. **Fork the repository**
2. **Create a feature branch**
   ```bash
   git checkout -b feature/your-feature-name
   ```
3. **Make your changes**
4. **Test your changes**
   ```bash
   ./mvnw test
   ```
5. **Commit your changes**
   ```bash
   git commit -m "Add: your feature description"
   ```
6. **Push to your fork**
   ```bash
   git push origin feature/your-feature-name
   ```
7. **Create a Pull Request**

### Code Style Guidelines

- Follow Java naming conventions
- Use Lombok annotations to reduce boilerplate
- Write meaningful commit messages
- Add JavaDoc for public methods
- Keep methods small and focused
- Use dependency injection

### Commit Message Format

```
Type: Short description

Detailed description (optional)

Types: Add, Update, Fix, Remove, Refactor, Docs, Test
```

## 📄 License

This project is developed for educational purposes.

## 📞 Contact & Support

For issues, questions, or contributions:
- **Repository**: [Saivardhan190/Online-Bidding-System](https://github.com/Saivardhan190/Online-Bidding-System)
- **Issues**: [GitHub Issues](https://github.com/Saivardhan190/Online-Bidding-System/issues)

---

## 📊 Quick Reference

### Default Credentials
- **Admin Email**: As configured in `ADMIN_DEFAULT_EMAIL`
- **Admin Password**: As configured in `ADMIN_DEFAULT_PASSWORD`

### Port Configuration
- **Application**: 8080
- **MySQL**: 3306

### File Upload Locations
- **Profile Pictures**: `/uploads/profiles/`
- **Stall Images**: `/uploads/stalls/`

### Important URLs
- **API Documentation**: `http://localhost:8080/api`
- **WebSocket**: `ws://localhost:8080/ws-bidding`
- **Health Check**: `http://localhost:8080/actuator/health` (if actuator enabled)

---
Thank you
