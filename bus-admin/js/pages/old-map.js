
import { db, auth, collection, doc, getDoc, getDocs, addDoc, rtdb, query, where, ref, onValue,
   updateDoc, deleteDoc, onSnapshot, serverTimestamp, onAuthStateChanged, GeoPoint } from "../core/firebase.js";

let map;
let markers = {};
let editMode = false;
let currentAction = null;
let clickMarker = null;

// Map Initialization
const initMap = () => {
  // Check if map already exists
  if (map) return;

  map = L.map('map').setView([33.18898, 35.30609], 13);
  
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    maxZoom: 19,
    attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
  }).addTo(map);

  // Add right-click context menu
  map.on('contextmenu', onMapRightClick);
  
  // Add click handler for adding stations
  map.on('click', onMapClick);
};

// Handle map clicks for adding stations
const onMapClick = (e) => {
  if (currentAction !== 'stations' || !editMode) return;
  
  // Clear previous click marker
  if (clickMarker) {
    map.removeLayer(clickMarker);
  }
  
  // Add new marker at click position
  clickMarker = L.marker(e.latlng, {
    draggable: true,
    icon: L.divIcon({className: 'temp-marker', html: '<i class="fas fa-map-marker-alt"></i>'})
  }).addTo(map);
  
  // Update modal coordinates
  document.getElementById('stationLat').value = e.latlng.lat.toFixed(6);
  document.getElementById('stationLng').value = e.latlng.lng.toFixed(6);
  
  // Open modal
  document.getElementById('stationModal').style.display = 'block';
  
  // Update marker position if dragged
  clickMarker.on('dragend', (e) => {
    const newPos = clickMarker.getLatLng();
    document.getElementById('stationLat').value = newPos.lat.toFixed(6);
    document.getElementById('stationLng').value = newPos.lng.toFixed(6);
  });
};

// Handle right-click for address lookup
const onMapRightClick = async (e) => {
  if (currentAction !== 'stations') return;
  
  try {
    const response = await fetch(`https://nominatim.openstreetmap.org/reverse?format=json&lat=${e.latlng.lat}&lon=${e.latlng.lng}`);
    const data = await response.json();
    
    if (data.address) {
      const address = [
        data.address.road,
        data.address.city || data.address.town || data.address.village,
        data.address.country
      ].filter(Boolean).join(', ');
      
      // Show address in a popup
      L.popup()
        .setLatLng(e.latlng)
        .setContent(`<strong>Address:</strong> ${address}`)
        .openOn(map);
    }
  } catch (error) {
    console.error('Error fetching address:', error);
  }
};

const setupStationManagement = () => {
  currentAction = 'stations';
  
  // Show station controls
  document.getElementById('stationControls').style.display = 'block';
  
  // Verify authentication first
  auth.onAuthStateChanged(async (user) => {
    if (!user) {
      console.error('User not authenticated');
      window.location.href = 'index.html';
      return;
    }
    try{
      // Check if user is admin
      const userDoc = await getDoc(doc(db, 'users', user.uid));
      if (!userDoc.exists() || userDoc.data().role !== 'admin') {
        alert('Only admins can manage stations');
        window.location.href = 'admin-dashboard.html';
        return;
      }

      // Setup button event listeners after auth check
      setupStationButtons();
      
      // Load stations
      onSnapshot(collection(db, 'stations'), (snapshot) => {
        snapshot.docChanges().forEach(change => {
          if (change.type === 'removed') {
            removeStationFromMap(change.doc.id);
          } else {
            updateStationOnMap(change.doc.id, change.doc.data());
          }
        });
      });
    }catch(error){
      console.error('Error initializing station management:', error);
      if (error.code === 'permission-denied') {
        alert('You do not have permission to view stations');
      }
    }
    
  });

  // Setup event listeners
  document.getElementById('addStationBtn').addEventListener('click', () => {
    editMode = true;
    document.getElementById('modalTitle').textContent = 'Add New Station';
    document.getElementById('stationForm').reset();
  });
  
  document.getElementById('editStationsBtn').addEventListener('click', function() {
    editMode = !editMode;
    this.classList.toggle('active');
    // Highlight editable stations
    Object.values(markers).forEach(marker => {
      marker.setOpacity(editMode ? 0.7 : 1);
    });
  });
  
  // Handle station form submission
  document.getElementById('stationForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    
    const stationData = {
      name: document.getElementById('stationName').value,
      route: document.getElementById('stationRoute').value,
      lat: parseFloat(document.getElementById('stationLat').value),
      lng: parseFloat(document.getElementById('stationLng').value),
      createdAt: serverTimestamp()
    };
    
    try {
      await addDoc(collection(db, 'stations'), stationData);
      document.getElementById('stationModal').style.display = 'none';
      if (clickMarker) map.removeLayer(clickMarker);
    } catch (error) {
      console.error('Error adding station:', error);
    }
  });
};

const setupStationButtons = () => {
  // Add Station button
  document.getElementById('addStationBtn').addEventListener('click', () => {
    editMode = true;
    document.getElementById('modalTitle').textContent = 'Add New Station';
    document.getElementById('stationForm').reset();
  });
  
  // Edit Stations button
  document.getElementById('editStationsBtn').addEventListener('click', function() {
    editMode = !editMode;
    this.classList.toggle('active');
    
    // // Update all markers
    // Object.values(markers).forEach(marker => {
    //   marker.setOpacity(editMode ? 0.7 : 1);
    //   marker.dragging[editMode ? 'enable' : 'disable']();
    // });
    // Update all markers
    Object.entries(markers).forEach(([id, marker]) => {
      marker.setOpacity(editMode ? 0.7 : 1);
      marker.dragging[editMode ? 'enable' : 'disable']();
      
      // Add/remove click handler for editing
      if (editMode) {
        marker.off('click'); // Remove any existing click handlers
        marker.on('click', (e) => {
          // Get station data from Firestore
          getDoc(doc(db, 'stations', id)).then((doc) => {
            if (doc.exists()) {
              openEditSidebar(id, doc.data());
            }
          });
        });
      } else {
        marker.off('click'); // Remove edit click handler
        // Restore original popup click behavior if needed
      }
    });
  });
 // Station form submission
 document.getElementById('stationForm').addEventListener('submit', async (e) => {
  e.preventDefault();
  
  const stationData = {
    name: document.getElementById('stationName').value,
    route: document.getElementById('stationRoute').value,
    lat: parseFloat(document.getElementById('stationLat').value),
    lng: parseFloat(document.getElementById('stationLng').value),
    location:'su',
    createdAt: serverTimestamp()
  };
  
  try {
    await addDoc(collection(db, 'stations'), stationData);
    document.getElementById('stationModal').style.display = 'none';
    if (clickMarker) map.removeLayer(clickMarker);
  } catch (error) {
    console.error('Error adding station:', error);
    alert('Failed to add station: ' + error.message);
  }
});
//cancel adding station
document.getElementById('cancelStationBtn').addEventListener('click', () => {
  document.getElementById('stationModal').style.display = 'none';
  if (clickMarker) {
    map.removeLayer(clickMarker);
    clickMarker = null;
  }
});
//click x to close station modal
document.querySelector('#stationModal .close').addEventListener('click', () => {
  document.getElementById('stationModal').style.display = 'none';
  if (clickMarker) {
    map.removeLayer(clickMarker);
    clickMarker = null;
  }
});
// sidebar close functionality:
document.getElementById('closeSidebar').addEventListener('click', () => {
  document.getElementById('editSidebar').classList.remove('open');
});
};

const updateStationOnMap = (id, data) => {
  // Default coordinates
  const DEFAULT_COORDS = [33.18898, 35.30609];
  let coords = DEFAULT_COORDS;
  
  // Check all possible coordinate formats
  if (data.lat && data.lng) {
    // Direct lat/lng properties
    coords = [data.lat, data.lng];
  } else if (data.location && data.location.latitude && data.location.longitude) {
    // Location object with latitude/longitude
    coords = [data.location.latitude, data.location.longitude];
  } else if (data.coordinates) {
    // Coordinates object
    const lat = data.coordinates.latitude || data.coordinates.lat;
    const lng = data.coordinates.longitude || data.coordinates.lng;
    if (lat && lng) coords = [lat, lng];
  } else if (data.geopoint && data.geopoint.latitude && data.geopoint.longitude) {
    // GeoPoint object
    coords = [data.geopoint.latitude, data.geopoint.longitude];
  } else {
    console.warn(`Station ${id} has no valid coordinates`, data);
    return; // Skip stations without valid coordinates
  }

  // Rest of the function remains the same...
  if (markers[id]) {
    markers[id].setLatLng(coords);
    markers[id].setPopupContent(`<b>${data.name || 'Unnamed Station'}</b><br>Route: ${data.route || 'N/A'}`);
  } else {
    markers[id] = L.marker(coords, {
      draggable: editMode,
      icon: L.divIcon({
        className: 'station-marker',
        html: '<i class="fas fa-map-marker-alt"></i>'
      })
    }).addTo(map)
      .bindPopup(`<b>${data.name || 'Unnamed Station'}</b><br>Route: ${data.route || 'N/A'}`);
    
    if (editMode) {
      markers[id].on('dragend', async (e) => {
        const newPos = markers[id].getLatLng();
        await updateDoc(doc(db, 'stations', id), {
          lat: newPos.lat,
          lng: newPos.lng,
          location: 'su' // Ensure location remains 'su' after update
        });
      });
      
      markers[id].on('click', (e) => {
        if (!editMode) return;
        openEditSidebar(id, data);
      });
    }
  }
};

const removeStationFromMap = (id) => {
  if (markers[id]) {
    map.removeLayer(markers[id]);
    delete markers[id];
  }
};

const openEditSidebar = (id, data) => {
  document.getElementById('editStationId').value = id;
  document.getElementById('editStationName').value = data.name || '';
  document.getElementById('editStationRoute').value = data.route || '';
  // Populate route dropdown
  populateRouteDropdown('editStationRoute', data.route);
  document.getElementById('editSidebar').classList.add('open');
  // Setup form submission
  document.getElementById('stationEditForm').onsubmit = async (e) => {
    e.preventDefault();
    await handleStationUpdate(id);
  };
  // Setup delete button
  document.getElementById('deleteStationBtn').onclick = async () => {
    if (confirm('Are you sure you want to delete this station?')) {
      await deleteDoc(doc(db, 'stations', id));
      document.getElementById('editSidebar').classList.remove('open');
    }
  };
};
// Helper function to populate route dropdown
const populateRouteDropdown = async (elementId, selectedValue = '') => {
  const select = document.getElementById(elementId);
  select.innerHTML = '<option value="">Select Route</option>';
  
  try {
    const routesSnapshot = await getDocs(collection(db, 'routes'));
    routesSnapshot.forEach((doc) => {
      const option = document.createElement('option');
      option.value = doc.id;
      option.textContent = doc.data().name || `Route ${doc.id}`;
      option.selected = doc.id === selectedValue;
      select.appendChild(option);
    });
  } catch (error) {
    console.error('Error loading routes:', error);
  }
};
// Handle station updates
const handleStationUpdate = async (id) => {
  try {
    await updateDoc(doc(db, 'stations', id), {
      name: document.getElementById('editStationName').value,
      route: document.getElementById('editStationRoute').value,
      updatedAt: serverTimestamp()
    });
    document.getElementById('editSidebar').classList.remove('open');
  } catch (error) {
    console.error('Error updating station:', error);
    alert('Failed to update station: ' + error.message);
  }
};

const setupBusTracking = () => {
  currentAction = 'buses';
  document.getElementById('busControls').style.display = 'block';
  
  // Bus markers storage
  let busMarkers = {};
  // Track if we're already listening to bus updates
  let busListenerUnsubscribe = null;
     
  // Helper function to animate marker
  const bounceMarker = (marker) => {
    marker.setIcon(
      L.divIcon({
        className: 'bus-marker bounce',
        html: '<i class="fas fa-bus"></i>'
      })
    );
    
    setTimeout(() => {
      marker.setIcon(
        L.divIcon({
          className: 'bus-marker',
          html: '<i class="fas fa-bus"></i>'
        })
      );
    }, 1000);
  };
  // Update or create a bus marker
  const updateBusMarker = (busId, busData) => {
    const position = busData.currentLocation;
    
    if (!position || !position.latitude || !position.longitude) {
      console.warn('Bus has invalid location data:', busId, busData);
      return;
    }
    
    const latLng = [position.latitude, position.longitude];
    
    if (busMarkers[busId]) {
      // Update existing marker position
      busMarkers[busId].setLatLng(latLng);
      
      // Update popup content
      busMarkers[busId].setPopupContent(
        `<b>Bus ${busData.busNumber || busId}</b><br>
         Last update: ${new Date(busData.timestamp).toLocaleTimeString()}`
      );
    } else {
      // Create new marker
      busMarkers[busId] = L.marker(latLng, {
        icon: L.divIcon({
          className: 'bus-marker',
          html: '<i class="fas fa-bus"></i>'
        }),
        zIndexOffset: 1000 // Make buses appear above stations
      }).addTo(map);
      
      // Add popup with bus info
      busMarkers[busId].bindPopup(
        `<b>Bus ${busData.busNumber || busId}</b><br>`
      );
      
      // Make marker bounce briefly to draw attention
      bounceMarker(busMarkers[busId]);
    }
  };
  // Clear all bus markers from map
  const clearBusMarkers = () => {
    Object.values(busMarkers).forEach(marker => {
      map.removeLayer(marker);
    });
    busMarkers = {};
    
    // Unsubscribe from previous listener if exists
    if (busListenerUnsubscribe) {
      busListenerUnsubscribe();
      busListenerUnsubscribe = null;
    }
  };
  // Load and track active buses
  const refreshBuses = async () => {
    try {
      console.log('Refreshing bus data...');
      
      // First clear existing bus markers
      clearBusMarkers();
      // Clear existing bus markers
      Object.values(busMarkers).forEach(marker => {
        map.removeLayer(marker);
      });
      busMarkers = {};
      
      // Get all active buses
      const busesRef = ref(rtdb, 'buses');

      onValue(busesRef, (snapshot) => {
        const busesData = snapshot.val();
        
        if (!busesData) {
          console.log('No active buses found');
          return;
        }
        
        // Update markers for each bus
        Object.entries(busesData).forEach(([busId, busData]) => {
          if (busData.location) {
            updateBusMarker(busId, busData);
          }
        });
      }, {
        onlyOnce: false // Keep listening for updates
      });
      
     
      
    } catch (error) {
      console.error('Error refreshing buses:', error);
      alert('Failed to refresh bus data. Please try again.');
    }
  }; 

  // Setup refresh button
  document.getElementById('refreshBusesBtn').addEventListener('click', refreshBuses);
  
  // Initial load
  refreshBuses();
  
  // Clean up when leaving the page
  window.addEventListener('beforeunload', () => {
    if (busListenerUnsubscribe) {
      busListenerUnsubscribe();
    }
  });
};

const setupRouteManagement = () => {
  currentAction = 'routes';
  document.getElementById('routeControls').style.display = 'block';
  
  // Initialize route selector dropdown
  const routeSelector = document.getElementById('routeSelector');
  
  // Clear existing options
  routeSelector.innerHTML = '<option value="">Select a route</option>';
  
  // Load available routes from Firestore
  const loadRoutes = async () => {
    try {
      const querySnapshot = await getDocs(collection(db, 'routes'));
      
      querySnapshot.forEach((doc) => {
        const route = doc.data();
        const option = document.createElement('option');
        option.value = doc.id;
        option.textContent = route.name || `Route ${doc.id}`;
        routeSelector.appendChild(option);
      });
      
      // Add event listener for route selection
      routeSelector.addEventListener('change', (e) => {
        const routeId = e.target.value;
        if (routeId) {
          displayRouteOnMap(routeId);
        } else {
          clearRouteFromMap();
        }
      });
      
    } catch (error) {
      console.error('Error loading routes:', error);
      alert('Failed to load routes. Please try again.');
    }
  };
  
  // Display selected route on map
  const displayRouteOnMap = async (routeId) => {
    try {
      // Clear any existing route from map
      clearRouteFromMap();
      
      // Get route details
      const routeDoc = await getDoc(doc(db, 'routes', routeId));
      if (!routeDoc.exists()) {
        throw new Error('Route not found');
      }
      
      const route = routeDoc.data();
      
      // Get all stations for this route
      const stationsQuery = query(
        collection(db, 'stations'),
        where('route', '==', routeId)
      );
      const stationsSnapshot = await getDocs(stationsQuery);
      
      const stations = [];
      stationsSnapshot.forEach((doc) => {
        stations.push({
          id: doc.id,
          ...doc.data()
        });
      });
      
      // Sort stations by sequence if available
      stations.sort((a, b) => (a.sequence || 0) - (b.sequence || 0));
      
      // Create polyline for the route
      const routeCoordinates = stations.map(station => [station.lat, station.lng]);
      
      if (routeCoordinates.length > 1) {
        // Create the route line
        markers.routeLine = L.polyline(routeCoordinates, {
          color: '#3498db',
          weight: 5,
          opacity: 0.7,
          className: 'route-line'
        }).addTo(map);
        
        // Zoom to fit the route
        map.fitBounds(markers.routeLine.getBounds());
      }
      
      // Highlight stations on this route
      stations.forEach(station => {
        if (markers[station.id]) {
          markers[station.id].setIcon(
            L.divIcon({
              className: 'station-marker highlight',
              html: '<i class="fas fa-map-marker-alt"></i>'
            })
          );
        }
      });
      
    } catch (error) {
      console.error('Error displaying route:', error);
      alert('Failed to display route. Please try again.');
    }
  };
  
  // Clear route visualization from map
  const clearRouteFromMap = () => {
    if (markers.routeLine) {
      map.removeLayer(markers.routeLine);
      delete markers.routeLine;
    }
    
    // Reset all station markers to default style
    Object.values(markers).forEach(marker => {
      if (marker instanceof L.Marker) {
        marker.setIcon(
          L.divIcon({
            className: 'station-marker',
            html: '<i class="fas fa-map-marker-alt"></i>'
          })
        );
      }
    });
  };
  
  // Initial load of routes
  loadRoutes();
};


export const initMapPage = () => {
  try {
    // Initialize map first
    initMap();

  // Get action parameter after map is ready
  const action = new URLSearchParams(window.location.search).get('action');

  // Default to stations if no action specified
  const validActions = ['stations', 'buses', 'routes'];
  const defaultAction = 'stations';
  if (!action || !validActions.includes(action)) {
    console.log(`No valid action parameter found, defaulting to ${defaultAction}`);
    // setupStationManagement();
    return;
  }
  
  // Use requestAnimationFrame for better performance
  requestAnimationFrame(() => {
    switch(action) {
      case 'stations':
        setupStationManagement();
        break;
      case 'buses':
        setupBusTracking();
        break;
      case 'routes':
        setupRouteManagement();
        break;
      default:
        console.warn('Unknown action:', action);
    }
  });

  } catch (error) {
    console.error('Error initializing map page:', error);
    alert('Failed to initialize map. Please try again.');
  }
  
};
