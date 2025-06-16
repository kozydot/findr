# Findr: Price Comparison Engine

Findr is a powerful price comparison engine that helps you find the best deals on your favorite products across UAE's top e-commerce platforms.

## Features

*   **Real-time Price Tracking:** Monitor prices across major UAE retailers.
*   **Price History Charts:** View historical price data to make informed purchasing decisions.
*   **Bookmarks:** Save your favorite products to a personalized list.

## Tech Stack

*   **Frontend:** React, TypeScript, Vite, Tailwind CSS
*   **Backend:** Java, Spring Boot, Maven
*   **Database:** Firebase Realtime Database
*   **Authentication:** Firebase Authentication

## Prerequisites

*   Node.js (v18 or higher)
*   npm (v8 or higher)
*   Java (v17 or higher)
*   Maven (v3.6 or higher)
*   Firebase Account

## Getting Started

### 1. Clone the Repository

```bash
git clone https://github.com/kozydot/findr.git
cd findr
```

### 2. Set Up Firebase

1.  Go to the [Firebase Console](https://console.firebase.google.com/) and create a new project.
2.  Enable **Firebase Authentication** with the "Email/Password" and "Google" sign-in methods.
3.  Create a **Realtime Database**.
4.  Go to **Project settings** > **Service accounts** and generate a new private key. This will download a JSON file with your Firebase credentials.

### 3. Configure Environment Variables

You will need to set up the following environment variables for the application to run correctly.

#### Backend (`price-comparator/`)

Create a `.env` file in the `price-comparator/` directory and add the following:

```
GOOGLE_APPLICATION_CREDENTIALS=path/to/your/firebase-credentials.json
OXYLABS_USERNAME=your_oxylabs_username
OXYLABS_PASSWORD=your_oxylabs_password
```

#### Frontend (`/`)

Create a `.env` file in the root directory and add the following:

```
VITE_API_BASE_URL=http://localhost:8081
```

### 4. Install Dependencies

#### Backend

```bash
cd price-comparator
mvn install
```

#### Frontend

```bash
npm install
```

### 5. Run the Application

You will need to run both the backend and frontend services in separate terminals.

#### Backend

```bash
cd price-comparator
mvn spring-boot:run
```

The backend will be running at `http://localhost:8081`.

#### Frontend

```bash
npm run dev
```

The frontend will be running at `http://localhost:5174`.

## Deployment

This project is configured for deployment on [Railway](https://railway.app/). Simply connect your GitHub repository to a new Railway project, and the `railway.json` file will handle the rest.
