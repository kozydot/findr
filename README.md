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

1.  Place your Firebase service account JSON file in the `price-comparator/src/main/resources/` directory.
2.  **Important:** Rename the file to `price-comparator-23ac4-firebase-adminsdk-fbsvc-a9eb8ddc35.json`.
3.  Open the `price-comparator/src/main/resources/application.properties` file and update the following values with your Oxylabs credentials:

```properties
oxylabs.username=your_oxylabs_username
oxylabs.password=your_oxylabs_password
```

#### Frontend (`/`)

The frontend is configured to use a proxy for API requests, so no environment variables are needed for it to communicate with the backend in a local development environment.

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