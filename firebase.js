// Import the functions you need from the SDKs you need
import { initializeApp } from "firebase/app";
import { getAnalytics } from "firebase/analytics";
// TODO: Add SDKs for Firebase products that you want to use
// https://firebase.google.com/docs/web/setup#available-libraries

// Your web app's Firebase configuration
// For Firebase JS SDK v7.20.0 and later, measurementId is optional
const firebaseConfig = {
  apiKey: "AIzaSyAVDTtbp_6lziNr2VzGV8Gsm3p4uVS1EPs",
  authDomain: "price-comparator-23ac4.firebaseapp.com",
  projectId: "price-comparator-23ac4",
  storageBucket: "price-comparator-23ac4.firebasestorage.app",
  messagingSenderId: "447166386996",
  appId: "1:447166386996:web:600b8a94604cad8f94cb61",
  measurementId: "G-XS51242Q3J"
};

// Initialize Firebase
const app = initializeApp(firebaseConfig);
const analytics = getAnalytics(app);