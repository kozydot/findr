// Import the functions you need from the SDKs you need
import { initializeApp } from "firebase/app";
import { getDatabase } from "firebase/database";
import { getAuth } from "firebase/auth";

// Your web app's Firebase configuration
const firebaseConfig = {
  apiKey: "AIzaSyAVDTtbp_6lziNr2VzGV8Gsm3p4uVS1EPs",
  authDomain: "price-comparator-23ac4.firebaseapp.com",
  projectId: "price-comparator-23ac4",
  databaseURL: "https://price-comparator-23ac4-default-rtdb.asia-southeast1.firebasedatabase.app",
  storageBucket: "price-comparator-23ac4.appspot.com",
  messagingSenderId: "447166386996",
  appId: "1:447166386996:web:600b8a94604cad8f94cb61",
  measurementId: "G-XS51242Q3J"
};

// Initialize Firebase
const app = initializeApp(firebaseConfig);
// const analytics = getAnalytics(app);
export const db = getDatabase(app);
export const auth = getAuth(app);