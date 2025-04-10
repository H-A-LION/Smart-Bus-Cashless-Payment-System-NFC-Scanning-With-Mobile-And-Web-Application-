import { initializeApp } from "https://www.gstatic.com/firebasejs/9.6.0/firebase-app.js";
import { 
  getFirestore, collection, doc, getDoc, getDocs, where,
  addDoc, updateDoc, deleteDoc, onSnapshot, serverTimestamp ,query
} from "https://www.gstatic.com/firebasejs/9.6.0/firebase-firestore.js";
import { 
  getDatabase, ref, set, onValue, update 
} from "https://www.gstatic.com/firebasejs/9.6.0/firebase-database.js";
import { 
  getAuth, signInWithEmailAndPassword, signOut, onAuthStateChanged 
} from "https://www.gstatic.com/firebasejs/9.6.0/firebase-auth.js";

const firebaseConfig = {
  apiKey: "AIzaSyCrCfIqGCkAmBO9UV_enQtIaL8lVRf1MzM",
  authDomain: "smartbus-cashless-payment-nfc.firebaseapp.com",
  databaseURL: "https://smartbus-cashless-payment-nfc-default-rtdb.firebaseio.com",
  projectId: "smartbus-cashless-payment-nfc",
  storageBucket: "smartbus-cashless-payment-nfc.firebasestorage.app",
  messagingSenderId: "577944959510",
  appId: "1:577944959510:web:96172f84f19cb6d3f1d337",
  measurementId: "G-KM9K4EWPP3"
};

const app = initializeApp(firebaseConfig);
const db = getFirestore(app);
const rtdb = getDatabase(app);
const auth = getAuth(app);

export { 
  db, rtdb, auth,
  collection, doc, getDoc, getDocs, addDoc, updateDoc, deleteDoc, onSnapshot, serverTimestamp,
  ref, set, onValue, update, where,
  signInWithEmailAndPassword, signOut, onAuthStateChanged, query
};