// Import the functions you need from the SDKs you need
  import { initializeApp } from "https://www.gstatic.com/firebasejs/11.4.0/firebase-app.js";
  import { getAnalytics } from "https://www.gstatic.com/firebasejs/11.4.0/firebase-analytics.js";
  import { getFirestore, getDatabase, ref, onValue, collection, query, where, getDocs, addDoc, doc, getDoc,updateDoc, deleteDoc, serverTimestamp, orderBy, limit } from "https://www.gstatic.com/firebasejs/11.4.0/firebase-firestore.js";
  import { firebaseConfig } from "./firebaseConfig";

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
  const db=getFirestore(app);// Get the Firestore Database instance
  const dbrealtime=getDatabase(app);// Get the Realtime Database instance
  dbd.settings({timestampsInSnapshots: true});
  

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



  //////////////////////read data/////////////////////
  //getting alldocs
async function getAllBuses() {
  const busesCol = collection(db, "buses");
  const querySnapshot = await getDocs(busesCol);
  const buses = [];
  querySnapshot.forEach((doc) => {
    buses.push({ id: doc.id, ...doc.data() }); //Include the document ID
  });
  console.log(buses);
  return buses;
}

getAllBuses();


//getting specific doc

async function getBus(busId) {
    const busDocRef = doc(db, "buses", busId);
    const busDoc = await getDoc(busDocRef);
    if(busDoc.exists()){
        return { id: busDoc.id, ...busDoc.data() };
    } else {
        return null;
    }
}

getBus("bus123");


//query data filter and sort
async function getRouteBuses(route) {
  const q = query(collection(db, "buses"), where("route", "==", route), orderBy("busNumber"), limit(10));
  const querySnapshot = await getDocs(q);
  const buses = [];
  querySnapshot.forEach((doc) => {
    buses.push({ id: doc.id, ...doc.data() });
  });
  console.log(buses);
    return buses;
}

getRouteBuses("Route A");


 ///////////////////////WriteData/////////////////////////
 //Adding new doc

 async function addBus(busData) {
   const busesCol = collection(db, "buses");
   const newBusRef = await addDoc(busesCol, { ...busData, timestamp: serverTimestamp() });
   console.log("Added bus with ID: ", newBusRef.id);
 }
 
 const newBus = {
   busNumber: "456",
   route: "Route B",
   location: "Depot"
 };
 
 addBus(newBus);


 //Updating existing doc
async function updateBus(busId, updates) {
  const busDocRef = doc(db, "buses", busId);
  await updateDoc(busDocRef, { ...updates, lastUpdated: serverTimestamp() });
  console.log("Bus updated");
}

updateBus("bus123", { location: "Main Street" });

//Deleting a doc
async function deleteBus(busId) {
  const busDocRef = doc(db, "buses", busId);
  await deleteDoc(busDocRef);
  console.log("Bus deleted");
}

deleteBus("bus123");
