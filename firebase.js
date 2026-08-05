// firebase.js

import { initializeApp } from "https://www.gstatic.com/firebasejs/11.0.2/firebase-app.js";
import {
    getDatabase,
    ref,
    onValue,
    update
} from "https://www.gstatic.com/firebasejs/11.0.2/firebase-database.js";

const firebaseConfig = {
    apiKey: "AIzaSyAbShgOnyg4dz2kmwh3Bdh2dEalCKrPQqQ",
    authDomain: "smart-home-monitoring-sy-95ea0.firebaseapp.com",
    databaseURL: "https://smart-home-monitoring-sy-95ea0-default-rtdb.asia-southeast1.firebasedatabase.app",
    projectId: "smart-home-monitoring-sy-95ea0",
    storageBucket: "smart-home-monitoring-sy-95ea0.firebasestorage.app",
    messagingSenderId: "2537742724",
    appId: "1:2537742724:web:b85ed4f099f3272d3e3dd1"
};

// Initialize Firebase
const app = initializeApp(firebaseConfig);

// Realtime Database
const db = getDatabase(app);

// Make available to script.js
window.db = db;
window.ref = ref;
window.onValue = onValue;
window.update = update;

console.log("✅ Firebase Connected");