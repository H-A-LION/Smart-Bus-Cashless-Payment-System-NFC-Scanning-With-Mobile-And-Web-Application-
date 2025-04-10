import { db, rtdb, collection, doc, getDoc, getDocs, 
    addDoc, updateDoc, deleteDoc, onSnapshot, 
    serverTimestamp, ref, onValue } from "../core/firebase.js";

let map;
let editMode = false;

// Map Initialization
const initMap = () => {
map = L.map('map').setView([9.005401, 38.763611], 13);
L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png').addTo(map);
};

// Station Management
const setupStationManagement = () => {
onSnapshot(collection(db, 'stations'), (snapshot) => {
snapshot.docChanges().forEach(change => {
  if (change.type === 'removed') removeStationFromMap(change.doc.id);
  else updateStationOnMap(change.doc.id, change.doc.data());
});
});
};

export const initMapPage = () => {
initMap();

const action = new URLSearchParams(window.location.search).get('action');
if (action === 'stations') setupStationManagement();
if (action === 'buses') setupBusTracking();
};
