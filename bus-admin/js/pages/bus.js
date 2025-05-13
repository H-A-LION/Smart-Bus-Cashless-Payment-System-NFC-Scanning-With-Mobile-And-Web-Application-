//bus.js
import { db, rtdb, collection, addDoc, ref, set, doc,
   getDocs, setDoc, onValue, serverTimestamp, deleteDoc, query, where } from "../core/firebase.js";
import { initBusTable } from "../modules/datatable.js";

let busTable;
let busMarkers={};
let lines=[];

const loadBuses = async () => {
  try {
    console.log("Loading buses from Firestore...");
    const querySnapshot = await getDocs(collection(db, "buses"));
    const buses = querySnapshot.docs.map(doc => {
      console.log(`Firestore bus ${doc.id}:`, doc.data());
      return { 
        id: doc.id, 
        busId: doc.id,
        ...doc.data() 
      };
    });
    
    console.log("Total buses loaded from Firestore:", buses.length);
    
    const busesRef = ref(rtdb, 'buses');
    console.log("Setting up Realtime Database listener...");
    
    onValue(busesRef, (snapshot) => {
      const realtimeData = snapshot.val() || {};
      console.log("Realtime Database snapshot:", realtimeData);
      
      const mergedData = buses.map(bus => {
        const realtimeBus = realtimeData[bus.id] || {};
        console.log(`Merging data for bus ${bus.id}:`, {
          firestoreData: bus,
          realtimeData: realtimeBus
        });
        
        return {
          ...bus,
          currentLocation: {
            latitude: realtimeBus.location?.lat || null,
            longitude: realtimeBus.location?.lng || null
          },          
          lastUpdated: realtimeBus.timestamp || bus.lastUpdated || 'Never'
        };
      });
      
      console.log("Final merged data before table update:", mergedData);
      busTable.clear().rows.add(mergedData).draw();
    }, (error) => {
      console.error("Realtime Database error:", error);
    });
  } catch (error) {
    console.error("Error loading buses:", error);
    alert("Failed to load buses. Please try again.");
  }
};

const loadLines = async () => {
  try {
    const querySnapshot = await getDocs(collection(db, "lines"));
    lines = querySnapshot.docs.map(doc => ({
      id: doc.id,
      ...doc.data()
    }));
    console.log("Loaded lines:", lines);
  } catch (error) {
    console.error("Error loading lines:", error);
  }
};

const addBus = async (e) => {
  e.preventDefault();
  try {
    const busId = document.getElementById('busId').value.trim();
    
    if (!busId) {
      alert("Bus ID is required");
      return;
    }

    // Add to Firestore
    await setDoc(doc(db, "buses", busId), {
      busId,
      createdAt: serverTimestamp()
    });

    // Initialize in Realtime Database
    await set(ref(rtdb, `buses/${busId}`), {
      location: null,
      timestamp: Date.now()
    });

    document.getElementById('busModal').style.display = 'none';
    document.getElementById('busForm').reset();
    loadBuses();
  } catch (error) {
    console.error("Error adding bus:", error);
    alert("Failed to add bus. Please try again.");
  }
};

const setupEventListeners = () => {
  // Add Bus button
  document.getElementById('addBusBtn').addEventListener('click', () => {
    document.getElementById('busModal').style.display = 'block';
  });

  // Bus Form submission
  document.getElementById('busForm').addEventListener('submit', addBus);

  // Cancel button
  document.getElementById('cancelBusBtn').addEventListener('click', () => {
    document.getElementById('busModal').style.display = 'none';
    document.getElementById('busForm').reset();
  });

  //search buses
  document.getElementById('searchBus').addEventListener('input', (e) => {
    busTable.search(e.target.value).draw();
  });


  // Track/Delete buttons (using event delegation)
  document.getElementById('busTable').addEventListener('click', async (e) => {
    const trackBtn = e.target.closest('.track-btn');
    const deleteBtn = e.target.closest('.delete-btn');
    
    if (trackBtn) {
      const busId = trackBtn.dataset.id;
      // Implement track functionality
      console.log("Track bus:", busId);
      window.location.href = `map-management.html?action=buses&busId=${busId}`;
    }
    
    if (deleteBtn) {
      const busId = deleteBtn.dataset.id;
      if (confirm(`Are you sure you want to delete bus ${busId}?`)) {
        try {
          // Delete from Firestore
          await deleteDoc(doc(db, "buses", busId));
          
          // Delete from Realtime Database
          await set(ref(rtdb, `buses/${busId}`), null);
          
          loadBuses();
        } catch (error) {
          console.error("Error deleting bus:", error);
          alert("Failed to delete bus. Please try again.");
        }
      }
    }
  });
};

export const initBusPage = () => {
  busTable = initBusTable();
  
  // document.getElementById('busForm').addEventListener('submit', async (e) => {
  //   e.preventDefault();
  //   const busId = document.getElementById('busId').value;
    
  //   await addDoc(collection(db, "Buses"), {
  //     busId,
  //     lastUpdated: serverTimestamp()
  //   });
    
  //   await set(ref(rtdb, `buses/${busId}`), {
  //     currentLocation: null,
  //     lastUpdated: Date.now()
  //   });
    
  //   busTable.ajax.reload();
  //   document.getElementById('busModal').style.display = 'none';
  // });
  setupEventListeners();
  loadBuses();
};
