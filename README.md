# YolmaFoodApp

A full-stack food ordering web application built with modern technologies, featuring secure authentication, online payment processing, and cloud-based image storage.

## Features

- **User Authentication & Authorization**: Secure login and registration system with Spring Security
- **Food Ordering System**: Browse menu items, add to cart, and place orders
- **Online Payment Processing**: Integrated Stripe payment gateway for secure transactions
- **Image Management**: AWS S3 integration for efficient image storage and retrieval
- **Responsive Design**: Modern React-based frontend for seamless user experience across devices
- **RESTful API**: Well-structured backend API for all operations

## Tech Stack

### Backend
- **Java** with **Spring Boot** framework
- **Spring Data JPA** for database operations
- **Spring Security** for authentication and authorization
- **MySQL** database
- **AWS S3** for image storage
- **Stripe API** for payment processing

### Frontend
- **React** for building user interface
- **JavaScript (ES6+)**
- **Axios** for HTTP requests

### Deployment
- **AWS EC2** for hosting

## Prerequisites

Before running this application, ensure you have the following installed:

- Java JDK 11 or higher
- Node.js and npm
- MySQL Server
- Maven
- AWS Account (for S3)
- Stripe Account (for payment processing)

## Getting Started

### Backend Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/InfiniteGosi/YolmaFoodApp.git
   cd YolmaFoodApp
   ```

2. **Configure MySQL Database**
   
   Create a MySQL database and update the `application.properties` file:
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/yolma_food_db
   spring.datasource.username=your_username
   spring.datasource.password=your_password
   ```

3. **Configure AWS S3**
   
   Add your AWS credentials to `application.properties`:
   ```properties
   aws.access.key.id=your_access_key
   aws.secret.access.key=your_secret_key
   aws.s3.bucket.name=your_bucket_name
   aws.region=your_region
   ```

4. **Configure Stripe**
   
   Add your Stripe API keys:
   ```properties
   stripe.api.key=your_stripe_secret_key
   ```

5. **Build and run the backend**
   ```bash
   mvn clean install
   mvn spring-boot:run
   ```

   The backend server will start on `http://localhost:8080`

### Frontend Setup

1. **Navigate to frontend directory**
   ```bash
   cd frontend
   ```

2. **Install dependencies**
   ```bash
   npm install
   ```

3. **Configure environment variables**
   
   Create a `.env` file in the frontend directory:
   ```env
   REACT_APP_API_URL=http://localhost:8080/api
   REACT_APP_STRIPE_PUBLIC_KEY=your_stripe_public_key
   ```

4. **Start the development server**
   ```bash
   npm start
   ```

   The frontend will start on `http://localhost:3000`

## Project Structure

```
YolmaFoodApp/
├── backend/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── com/yolma/foodapp/
│   │   │   │       ├── config/
│   │   │   │       ├── controller/
│   │   │   │       ├── model/
│   │   │   │       ├── repository/
│   │   │   │       ├── service/
│   │   │   │       └── security/
│   │   │   └── resources/
│   │   │       └── application.properties
│   │   └── test/
│   └── pom.xml
├── frontend/
│   ├── public/
│   ├── src/
│   │   ├── components/
│   │   ├── pages/
│   │   ├── services/
│   │   ├── utils/
│   │   └── App.js
│   ├── package.json
│   └── .env
└── README.md
```

## Environment Variables

### Backend (`application.properties`)
- `spring.datasource.url` - MySQL database URL
- `spring.datasource.username` - Database username
- `spring.datasource.password` - Database password
- `aws.access.key.id` - AWS access key
- `aws.secret.access.key` - AWS secret key
- `aws.s3.bucket.name` - S3 bucket name
- `aws.region` - AWS region
- `stripe.api.key` - Stripe secret key

### Frontend (`.env`)
- `REACT_APP_API_URL` - Backend API base URL
- `REACT_APP_STRIPE_PUBLIC_KEY` - Stripe publishable key

## API Endpoints

### Authentication
- `POST /api/auth/register` - Register new user
- `POST /api/auth/login` - User login

### Food Items
- `GET /api/foods` - Get all food items
- `GET /api/foods/{id}` - Get food item by ID
- `POST /api/foods` - Create new food item (Admin)
- `PUT /api/foods/{id}` - Update food item (Admin)
- `DELETE /api/foods/{id}` - Delete food item (Admin)

### Orders
- `POST /api/orders` - Create new order
- `GET /api/orders` - Get user orders
- `GET /api/orders/{id}` - Get order by ID

### Payments
- `POST /api/payments/create-checkout-session` - Create Stripe checkout session
- `POST /api/payments/webhook` - Handle Stripe webhooks
