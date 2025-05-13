// station-manager.js
import { db, auth, doc, getDoc, collection, addDoc, updateDoc, deleteDoc, onSnapshot, serverTimestamp } from "../../core/firebase.js";
import { getMapState, setMapState, addMarker, removeMarker, setupMapListeners, clearAllMarkers } from "./map-core.js";

let clickMarker = null;
let markers = {};
let editMode = false;



const onMapClick = (e) => {
  const { currentAction, editMode } = getMapState();
  if (currentAction !== 'stations' || !editMode) return;
  
  if (clickMarker) removeMarker('clickMarker');
  
  clickMarker = addMarker('clickMarker', e.latlng, {
    draggable: true,
    icon: L.divIcon({className: 'temp-marker', html: '<i class="fas fa-map-marker-alt"></i>'}),
    data: { isTemporary: true } 
  });
  
  document.getElementById('stationLat').value = e.latlng.lat.toFixed(6);
  document.getElementById('stationLng').value = e.latlng.lng.toFixed(6);
  document.getElementById('stationModal').style.display = 'block';
  
  clickMarker.on('dragend', (e) => {
    const newPos = clickMarker.getLatLng();
    document.getElementById('stationLat').value = newPos.lat.toFixed(6);
    document.getElementById('stationLng').value = newPos.lng.toFixed(6);
  });
};

const onMapRightClick = async (e) => {
  const { currentAction,map } = getMapState();
  if (currentAction !== 'stations') return;
  // First check if we clicked on a station
  const station = findNearestStation(e.latlng);
  if (station) {
    L.popup()
      .setLatLng(station.position)
      .setContent(`<b>${station.data.name}</b><br>Click to select for line creation`)
      .openOn(map);
    return; // Stop here if we found a station
  }
  try {
    const response = await fetch(`https://nominatim.openstreetmap.org/reverse?format=json&lat=${e.latlng.lat}&lon=${e.latlng.lng}`);
    const data = await response.json();
    
    if (data.address) {
      const address = [
        data.address.road,
        data.address.city || data.address.town || data.address.village,
        data.address.country
      ].filter(Boolean).join(', ');
      
      L.popup()
        .setLatLng(e.latlng)
        .setContent(`<strong>Address:</strong> ${address}`)
        .openOn(map);
    }
  } catch (error) {
    console.error('Error fetching address:', error);
  }
};

const updateStationOnMap = (id, data) => {
  const { markers, editMode } = getMapState();
  // Determine coordinates from various possible formats
  let coords;
  if (data.lat && data.lng) {
    coords = [data.lat, data.lng];
  } else if (data.location && data.location.latitude && data.location.longitude) {
    coords = [data.location.latitude, data.location.longitude];
  } else if (data.coordinates) {
    const lat = data.coordinates.latitude || data.coordinates.lat;
    const lng = data.coordinates.longitude || data.coordinates.lng;
    if (lat && lng) coords = [lat, lng];
  } else if (data.geopoint && data.geopoint.latitude && data.geopoint.longitude) {
    coords = [data.geopoint.latitude, data.geopoint.longitude];
  } else {
    console.warn(`Station ${id} has no valid coordinates`, data);
    return;
  }

  if (markers[id]) {
    markers[id].setLatLng(coords);
    const lineInfo = data.lineId ? `<br>Line: ${data.lineId}` : '';
    markers[id].setPopupContent(`<b>${data.name || 'Unnamed Station'}</b><br>Route: ${data.route || 'N/A'}${lineInfo}`);
    
  } else {
    const marker=addMarker(id, coords, {
      draggable: editMode,
      icon: L.divIcon({
        className: data.lineId ? 'station-marker line-station' : 'station-marker',
        html: `<i class="fas fa-map-marker-alt"></i>${data.lineId ? `<span class="line-badge">${data.lineId}</span>` : ''}`
      }),
      id: id,
      data: data,
      interactive: true, // CRITICAL - makes markers clickable
      bubblingMouseEvents: false // CRITICAL - prevents event propagation
    }).bindPopup(`<b>${data.name || 'Unnamed Station'}</b><br>Route: ${data.route || 'N/A'}`);

    if (editMode) {
      markers[id].on('dragend', async (e) => {
        const newPos = markers[id].getLatLng();
        await updateDoc(doc(db, 'stations', id), {
          lat: newPos.lat,
          lng: newPos.lng,
          location: 'su'
        });
        if (data.lineId) {
          const lineRef = doc(db, 'lines', data.lineId);
          const lineDoc = await getDoc(lineRef);
          if (lineDoc.exists()) {
            const lineData = lineDoc.data();
            const updatedStations = lineData.stations.map(s => 
              s.id === id ? { ...s, lat: newPos.lat, lng: newPos.lng } : s
            );
            await updateDoc(lineRef, { stations: updatedStations });
          }
        }
        await updateDoc(doc(db, 'stations', id), updateData);
      });
      
      markers[id].on('click', (e) => {
        if (!getMapState().editMode) return;
        e.originalEvent.preventDefault();
        e.originalEvent.stopPropagation(); // Prevent map click event
        openEditSidebar(id, data);
      });
      marker.on('contextmenu', (e) => {
        e.originalEvent.preventDefault();
        onMapRightClick(e); // Trigger our right-click handler
      });
    }
  }
};

const openEditSidebar = (id, data) => {
  console.log('Sidebar elements:', {
    sidebar: document.getElementById('editSidebar'),
    idField: document.getElementById('editStationId'),
    nameField: document.getElementById('editStationName')
  });
  // CHANGE: Fixed route select initialization
  const routeSelect = document.getElementById('editStationRoute');
  if (routeSelect) {
    routeSelect.innerHTML = '<option value="">Select Route</option>';
    const lineSelect = document.createElement('select');
    lineSelect.id = 'editStationLine';
    if (data.route) {
      routeSelect.value = data.route;
    }
  }
  console.log('Opening sidebar for station:', id); // DEBUG
  document.getElementById('editStationId').value = id;
  document.getElementById('editStationName').value = data.name || '';

  const sidebar=document.getElementById('editSidebar');
  if(sidebar){
  sidebar.classList.add('open');
  console.log('Sidebar should be visible now');
  }
  else{
  console.error('Sidebar element not found');
  }
};

const setupStationButtons = () => {
  // In your setupStationManagement function:
console.log('Station controls initialized');
document.querySelectorAll('.control-group button').forEach(btn => {
    btn.addEventListener('click', () => {
        console.log('Button clicked:', btn.id);
    });
});
  // Check if elements exist before adding listeners
  const addStationBtn = document.getElementById('addStationBtn');
  const editStationsBtn = document.getElementById('editStationsBtn');
  const stationForm = document.getElementById('stationForm');
  const cancelStationBtn = document.getElementById('cancelStationBtn');
  const modalCloseBtn = document.querySelector('#stationModal .close');
  const closeSidebarBtn = document.getElementById('closeSidebar');
  const updateStationBtn = document.getElementById('updateStationBtn');
  const deleteStationBtn = document.getElementById('deleteStationBtn');
  if (!addStationBtn || !editStationsBtn || !stationForm || !cancelStationBtn || 
    !modalCloseBtn || !closeSidebarBtn || !updateStationBtn || !deleteStationBtn) {
  console.error('Required DOM elements not found');
  return;
}
  // Add Station button
  addStationBtn.addEventListener('click', () => {
    setMapState({ editMode: true });
    document.getElementById('modalTitle').textContent = 'Add New Station';
    document.getElementById('stationForm').reset();
  });
  
  // Edit Stations button
  editStationsBtn.addEventListener('click', function() {
    const { editMode } = getMapState();
    const newEditMode = !editMode;
    setMapState({ editMode: newEditMode });
    
    this.classList.toggle('active');

    const {markers}=getMapState();
    Object.values(markers).forEach(marker => {
      marker.setOpacity(newEditMode ? 0.7 : 1);
      marker.dragging[newEditMode ? 'enable' : 'disable']();
      marker.off('click');

      if (newEditMode) {
        
        marker.on('click', (e) => {
            e.originalEvent.preventDefault();
            e.originalEvent.stopPropagation();
            const stationId = marker.options.id; // Assuming marker has station ID
            const stationData = markers[stationId].options.data; // Assuming we store data with marker
            openEditSidebar(stationId, stationData);
        });
    } else {
        marker.off('click'); // Remove edit click handlers when exiting edit mode
    }
    });
    if (newEditMode) {
      alert('Edit mode activated. Click on a station to edit it.');
    } else {
      alert('Edit mode deactivated.');
    }
  });
  
  // Station form submission
  stationForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    
    const stationData = {
      name: document.getElementById('stationName').value,
      route: document.getElementById('stationRoute').value,
      lat: parseFloat(document.getElementById('stationLat').value),
      lng: parseFloat(document.getElementById('stationLng').value),
      location: 'su',
      createdAt: serverTimestamp()
    };
    
    try {
      await addDoc(collection(db, 'stations'), stationData);
      document.getElementById('stationModal').style.display = 'none';
      if (clickMarker) removeMarker('clickMarker');
    } catch (error) {
      console.error('Error adding station:', error);
      alert('Failed to add station: ' + error.message);
    }
  });
  
  // Cancel adding station
  cancelStationBtn.addEventListener('click', () => {
    document.getElementById('stationModal').style.display = 'none';
    if (clickMarker) removeMarker('clickMarker');
  });
  
  // Close modal
  modalCloseBtn.addEventListener('click', () => {
    document.getElementById('stationModal').style.display = 'none';
    if (clickMarker) removeMarker('clickMarker');
  });
  
  // Sidebar close
  closeSidebarBtn.addEventListener('click', () => {
    document.getElementById('editSidebar').classList.remove('open');
  });
  
  // Update station form
  updateStationBtn.addEventListener('click', async (e) => {
    e.preventDefault();
    const id = document.getElementById('editStationId').value;
    const name = document.getElementById('editStationName').value;
    const route = document.getElementById('editStationRoute').value;
    const lineId = document.getElementById('editStationLine')?.value;
    
    try {
      await updateDoc(doc(db, 'stations', id), {
        name,
        route,
        ...(lineId && { lineId })
      });
      document.getElementById('editSidebar').classList.remove('open');
    } catch (error) {
      console.error('Error updating station:', error);
      alert('Failed to update station');
    }
  });
  
  // Delete station
  deleteStationBtn.addEventListener('click', async (e) => {
    e.preventDefault();
    const id = document.getElementById('editStationId').value;
    if (confirm('Are you sure you want to delete this station?')) {
      try {
        await deleteDoc(doc(db, 'stations', id));
        document.getElementById('editSidebar').classList.remove('open');
      } catch (error) {
        console.error('Error deleting station:', error);
        alert('Failed to delete station');
      }
    }
  });
};


export const findStationAtPosition = (latlng) => { // Helper for line manager
  const { map } = getMapState();
  for (const id in markers) {
    if (markers[id] instanceof L.Marker && 
        map.distance(latlng, markers[id].getLatLng()) < 30) {
      return { id, marker: markers[id], data: markers[id].options.data };
    }
  }
  return null;
};

export const getStationMarkers = () => markers; 

export const getAllStationMarkers = () => {
  return Object.values(markers).filter(m => !m.options.data?.isTemporary);
};

// export const findNearestStation = (latlng, maxDistance = 50) => {
//   const { map } = getMapState();
//   let closestStation = null;
//   let minDistance = Infinity;

//   Object.entries(markers).forEach(([id, marker]) => {
//     if (marker instanceof L.Marker && !marker.options.data?.isTemporary) {
//       const distance = map.distance(latlng, marker.getLatLng());
//       if (distance < maxDistance && distance < minDistance) {
//         minDistance = distance;
//         closestStation = { 
//           id,
//           position: marker.getLatLng(),
//           data: marker.options.data 
//         };
//         console.log('closestStation3= ',String(closestStation));
//       }
//       console.log('closestStation2= ',String(closestStation));
//     }
//   });
//   if(!closestStation)
//     console.log('closestStation1= ',String(closestStation));
//   return closestStation;
// };
export const findNearestStation = (e, maxDistance = 30) => {
  const { map, markers } = getMapState();
  
  // Prevent default map behaviors if it's a click event
  if (e && e.originalEvent) {
    e.originalEvent.preventDefault();
    e.originalEvent.stopPropagation();
  }

  const latlng = e.latlng || e;
  let closestStation = null;
  let minDistance = Infinity;

  Object.entries(markers).forEach(([id, marker]) => {
    if (!(marker instanceof L.Marker)) return;

    try {
      const markerPos = marker.getLatLng();
      const distance = map.distance(latlng, markerPos);
      
      if (distance < maxDistance && distance < minDistance) {
        minDistance = distance;
        closestStation = {
          id,
          position: markerPos,
          data: marker.options.data,
          marker: marker,
          element: e.target // The actual clicked DOM element
        };
      }
    } catch (error) {
      console.error(`Error processing marker ${id}:`, error);
    }
  });

  return closestStation;
};



export const setupStationManagement = async () => {
  console.log('Initializing station management', {
    map: !!getMapState().map,
    editMode: getMapState().editMode,
    markers: Object.keys(getMapState().markers).length
  });
  // Wait for DOM to be fully loaded
  if (document.readyState !== 'complete') {
    await new Promise(resolve => {
      window.addEventListener('load', resolve);
    });
  } 
  const { map } = getMapState();
  setMapState({ currentAction: 'stations' });
  
  // Show station controls UI
  const stationControls = document.getElementById('stationControls');
  if (stationControls) {
    stationControls.style.display = 'block';
  } else {
    console.error('Station controls element not found');
    return;
  }
  
  // Verify authentication
  auth.onAuthStateChanged(async (user) => {
    if (!user) {
      window.location.href = 'index.html';
      return;
    }
    
    try {
      const userDoc = await getDoc(doc(db, 'users', user.uid));
      if (!userDoc.exists() || userDoc.data().role !== 'admin') {
        alert('Only admins can manage stations');
        window.location.href = 'admin-dashboard.html';
        return;
      }

      setupStationButtons();
      loadStations();
    } catch(error) {
      console.error('Error initializing station management:', error);
    }
  });

  // Setup map event listeners
  setupMapListeners({
    onClick: onMapClick,
    onRightClick: onMapRightClick
  });
  
  return {
    cleanup: () => {
      removeMapListeners();
      if (clickMarker) removeMarker('clickMarker');
    },
    getStationMarkers: () => markers,
    findStationAtPosition
  };
};

// Replace the existing loadStations function with this single version:

export const loadStations = () => {
  return new Promise((resolve) => {
    const unsubscribe = onSnapshot(collection(db, 'stations'), (snapshot) => {
      console.log('Received stations snapshot:', snapshot.size, 'stations'); // Debug
      snapshot.docChanges().forEach(change => {
        // console.log('Station change:', change.type, change.doc.id, change.doc.data()); // Debug
        if (change.type === 'removed') {
          removeMarker(change.doc.id);
        } else {
          const data = change.doc.data();
          // Ensure line reference exists
          if (data.lineId) {
            getDoc(doc(db, 'lines', data.lineId)).then(lineDoc => {
              if (!lineDoc.exists()) {
                updateDoc(doc(db, 'stations', change.doc.id), { lineId: null });
              }
            });
          }
          updateStationOnMap(change.doc.id, data);
        }
      });
      resolve();
    });
  });
};

