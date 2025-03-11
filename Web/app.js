// Import the functions you need from the SDKs you need
  import { initializeApp } from "https://www.gstatic.com/firebasejs/11.4.0/firebase-app.js";
  import { getAnalytics } from "https://www.gstatic.com/firebasejs/11.4.0/firebase-analytics.js";
  import { getFirestore, collection, query, where, getDocs } from "https://www.gstatic.com/firebasejs/11.4.0/firebase-firestore.js";



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
    appId: "1:577944959510:web:96172f84f19cb6d3f1d337",
    measurementId: "G-KM9K4EWPP3"
  };

  // Initialize Firebase
  const app = initializeApp(firebaseConfig);
  const analytics = getAnalytics(app);
  const dbd=firebase.firestore();
  const dbr=firebase.realtime();
  dbd.settings({timestampsInSnapshots: true});
  // Initialize Firestore
  const db = getFirestore();

  // Function to handle login
   function handleLogin(event){
    event.preventDefault(); // Prevent the default form submission

    const username = document.querySelector('input[name="uname"]').value;
    const password = document.querySelector('input[name="pswd"]').value;

    signInWithEmailAndPassword(auth, username, password)
      .then((userCredential) => {
        // Login successful, redirect to the admin page
        window.location.href = 'Admin_page.html'; 
      })
      .catch((error) => {
        // Handle Errors
        const errorCode = error.code;
        const errorMessage = error.message;
        alert("Login failed: " + errorMessage);
      });
  }
 // Add event listener to the form submit
  document.querySelector('form').addEventListener('submit', handleLogin);


