//firebase.js
// Import the functions you need from the SDKs you need
import { initializeApp } from "https://www.gstatic.com/firebasejs/9.6.0/firebase-app.js";
import { getFirestore, collection, doc, getDoc, getDocs,deleteField,
   where, addDoc, updateDoc, deleteDoc, onSnapshot, GeoPoint,
    serverTimestamp , query, setDoc, arrayUnion, increment, runTransaction } from "https://www.gstatic.com/firebasejs/9.6.0/firebase-firestore.js";

import { getDatabase, ref, set, onValue, update } from "https://www.gstatic.com/firebasejs/9.6.0/firebase-database.js";

import { getAuth, signInWithEmailAndPassword, signOut,
   onAuthStateChanged, createUserWithEmailAndPassword,
    sendEmailVerification ,browserSessionPersistence,
     setPersistence, sendPasswordResetEmail, confirmPasswordReset
    } from "https://www.gstatic.com/firebasejs/9.6.0/firebase-auth.js";

import { getAnalytics } from "https://www.gstatic.com/firebasejs/9.6.0/firebase-analytics.js";

// TODO: Add SDKs for Firebase products that you want to use

// https://firebase.google.com/docs/web/setup#available-libraries


// Your web app's Firebase configuration

// For Firebase JS SDK v7.20.0 and later, measurementId is optional

const firebaseConfig = {

  apiKey: "AIzaSyCrCfIqGCkAmBO9UV_enQtIaL8lVRf1MzM",

  authDomain: "smartbus-cashless-payment-nfc.firebaseapp.com",

  databaseURL: "https://smartbus-cashless-payment-nfc-default-rtdb.firebaseio.com",

  projectId: "smartbus-cashless-payment-nfc",

  storageBucket: "smartbus-cashless-payment-nfc.firebasestorage.app",

  messagingSenderId: "577944959510",

  appId: "1:577944959510:web:19b826125fd9f1aff1d337",

  measurementId: "G-F3DM464SPC"

};

// Initialize Firebase
const app = initializeApp(firebaseConfig);
const db = getFirestore(app);
const rtdb = getDatabase(app);
const auth = getAuth(app);
const analytics = getAnalytics(app);


export { 
  db, rtdb, auth, collection, doc, getDoc, getDocs, addDoc, updateDoc, GeoPoint,
  deleteDoc, onSnapshot, serverTimestamp, ref, set, onValue, update, where, deleteField,
  createUserWithEmailAndPassword, sendEmailVerification, signInWithEmailAndPassword,
  signOut, onAuthStateChanged, query, setDoc,browserSessionPersistence, setPersistence,
   sendPasswordResetEmail, confirmPasswordReset, arrayUnion, increment, runTransaction
};