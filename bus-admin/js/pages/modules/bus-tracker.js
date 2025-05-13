// bus-tracker.js
import { rtdb, ref, onValue } from "../../core/firebase.js";
import { getMapState, setMapState, addMarker, removeMarker, clearAllMarkers } from "./map-core.js";

let busMarkers = {};
let busListenerUnsubscribe = null;

const updateBusMarker = (busId, busData) => {
  const { markers } = getMapState();
  
  if (!busData.location) return;
  
  const icon = L.divIcon({
    className: 'bus-marker',
    html: `<div class="bus-icon" style="color:#3399ff;">
    <i class="fa-solid fa-van-shuttle"></i>
    <span><p style="font-size:12px;">${busId}</p></span>
    </div>`,
    iconSize: [10, 10]
  });
  
  if (markers[`bus_${busId}`]) {
    markers[`bus_${busId}`].setLatLng([busData.location.lat, busData.location.lng]);
  } else {
    busMarkers[busId] = addMarker(`bus_${busId}`, [busData.location.lat, busData.location.lng], {
      icon,
      zIndexOffset: 1000
    }).bindPopup(`<b>Bus ${busId}</b><br>Last update: ${new Date(busData.timestamp).toLocaleTimeString()}`);
  }
};

const clearBusMarkers = () => {
  Object.keys(busMarkers).forEach(busId => {
    removeMarker(`bus_${busId}`);
  });
  busMarkers = {};
};

export const refreshBuses = async () => {
  const { map } = getMapState();
  
  try {
    clearBusMarkers();
    
    const busesRef = ref(rtdb, 'buses');
    busListenerUnsubscribe = onValue(busesRef, (snapshot) => {
      const busesData = snapshot.val();
      if (!busesData) return;
      
      Object.entries(busesData).forEach(([busId, busData]) => {
        if (busData.location) {
          updateBusMarker(busId, busData);
        }
      });
    }, { onlyOnce: false });
  } catch (error) {
    console.error('Error refreshing buses:', error);
  }
};

export const setupBusTracking = (busId = null) => {
  const { map } = getMapState();
  setMapState({ currentAction: 'buses' });
  
  // Show bus controls UI
  document.getElementById('busControls').style.display = 'block';
  
  // Setup refresh button
  document.getElementById('refreshBusesBtn').addEventListener('click', refreshBuses);
  
  // If tracking specific bus, highlight it
  if (busId) {
    // Custom implementation for highlighting a specific bus
  }
  
  // Initial load
  refreshBuses();
  
  return {
    cleanup: () => {
      if (busListenerUnsubscribe) busListenerUnsubscribe();
      clearBusMarkers();
    }
  };
};