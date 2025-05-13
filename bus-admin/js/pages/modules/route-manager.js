// route-manager.js
import { db, collection, doc, getDoc, getDocs, addDoc, serverTimestamp } from "../../core/firebase.js";
import { getMapState, setMapState, addMarker, removeMarker, clearAllMarkers, setupMapListeners, removeMapListeners, normalizeCoordinates, validateCoordinates, parseStationCoords } from "./map-core.js";


let routePolylines = [];
let routeCreationMode = false;
let currentRoute = null;
let tempRoute = null;
let startRouteStation = null;
let routeStations = {}; // Track stations specific to routes

const displayRouteOnMap = (route) => {
  const { map } = getMapState();
  Object.entries(routeStations).forEach(([id, station]) => {
    // Double-check we have valid coordinates
    if (station.position && station.position.lat && station.position.lng) {
      addMarker(`route_station_${id}`, [station.position.lat, station.position.lng], {
        icon: L.divIcon({
          className: 'route-station-marker',
          html: '<i class="fas fa-circle"></i>'
        }),
        data: station.data
      });
    }
  });
  /*
  // Clear previous route if any
  clearRouteFromMap();
  
  // Create polyline for the route
  const polyline = L.polyline(route.path.map(p=>[p.lat,p.lng]), {
    color: '#3498db',
    weight: 5,
    opacity: 0.7,
    dashArray: '10, 10'
  }).addTo(map);
  
  routePolylines.push(polyline);
  
  route.stations.forEach(stationId => {
    const station = routeStations[stationId];
    // In a real implementation, you would fetch station data
    if(station){
      addMarker(`route_station_${stationId}`, station.position, {
        icon: L.divIcon({
          className: 'route-station-marker',
          html: '<i class="fas fa-circle"></i>'
        })
      });
    }
    
  });*/
};

const displayRouteStations = () => {
  const { map } = getMapState();
  Object.entries(routeStations).forEach(([id, station]) => {
    if (station.position && !isNaN(station.position.lat) && !isNaN(station.position.lng)) {
      addMarker(`route_station_${id}`, [station.position.lat, station.position.lng], {
        icon: L.divIcon({
          className: 'route-station-marker',
          html: '<i class="fas fa-map-marker-alt"></i>'
        }),
        data: station.data
      });
    }    
  });
};

const findNearestRouteStation = (clickPoint, maxDistance = 300) => {
  const { map } = getMapState();
  
  const normalizedClick = normalizeCoordinates(clickPoint);
  if (!normalizedClick || !validateCoordinates(normalizedClick)) {
    console.error('Invalid click coordinates:', clickPoint);
    return null;
  }

  

  let closestStation = null;
  let minDistance = Infinity;

  Object.entries(routeStations).forEach(([id, station]) => {
    try {
      // Skip if station has no valid position
      if (!station?.position) {
        console.warn(`Station ${id} has no position data`);
        return;
      }
      const stationLatLng = L.latLng(
        station.position.lat, 
        station.position.lng
      );
      console.log("stationLatlng");
      //calculate distance
      const distance = map.distance(clickPoint, stationLatLng);
      console.log("distance",distance);
      if (distance < maxDistance && distance < minDistance) {
        minDistance = distance;
        
        closestStation = {
          id,
          position: stationLatLng,
          data: station.data
        };
      }
    } catch (error) {
      console.error(`Error processing station ${id}:`, error);
    }
  });

  return closestStation;
};
//route creation function
const startRouteCreation = () => {
  const { map } = getMapState();
  routeCreationMode = true;
  currentRoute = {
    name: '',
    stations: [],
    path: [],
    createdAt:serverTimestamp()
  };
  document.getElementById('routeStatus').textContent = 'Right-click on a station to start route';
  
  setupMapListeners({
    onRightClick:  (e) => {
      const station = findNearestRouteStation(e.latlng);
      if (station) {
        if (!currentRoute.stations.includes(station.id)) {
          currentRoute.stations.push(station.id);
          currentRoute.path.push({
            lat: station.position.lat,
            lng: station.position.lng
          });
          updateRouteVisualization();
        }
      } else {
        alert('Click directly on a station marker');
      }
    }
    // onStationSelectForRoute
    ,
    onClick: onMapClickAddRoutePoint
  });
};

const updateRouteVisualization = () => {
  const { map } = getMapState();
  if (!map || !currentRoute?.path) return;

  // Validate and convert all path points
  const validLatLngs = currentRoute.path
    .map(point => {
      try {
        // Ensure we have valid numbers
        const lat = parseFloat(point.lat);
        const lng = parseFloat(point.lng);
        if (isNaN(lat) || isNaN(lng)) {
          console.warn('Invalid coordinates in path:', point);
          return null;
        }
        return L.latLng(lat, lng);
      } catch (error) {
        console.error('Coordinate conversion failed:', error);
        return null;
      }
    })
    .filter(Boolean); // Remove any null/invalid points

  if (validLatLngs.length < 2) {
    console.warn('Not enough valid points to draw route');
    return;
  }

  try {
    if (!tempRoute) {
      tempRoute = L.polyline([], {
        color: '#3498db',
        weight: 5,
        dashArray: '5,5'
      }).addTo(map);
    }
    tempRoute.setLatLngs(validLatLngs);
  } catch (error) {
    console.error('Failed to update route visualization:', error);
    // Clean up broken polyline
    if (tempRoute) {
      map.removeLayer(tempRoute);
      tempRoute = null;
    }
  }
};

//Handle station selection for routes
const onStationSelectForRoute = (e) => {
  const clickedStation = findNearestRouteStation(e);
  if (!clickedStation) {
    console.log('error @ onStationSelectForRoute function: clickedStation=null!!');
    alert('Please right-click directly on a station marker');
    return;
  }

  if (!startRouteStation) {
    // First station selected
    startRouteStation = clickedStation;
    currentRoute.stations.push(clickedStation.id);
    currentRoute.path.push({
      lat: clickedStation.position.lat,
      lng: clickedStation.position.lng
    });
    document.getElementById('routeStatus').textContent = 'Left-click to add route points, right-click on another station to finish';
  } else {
    // Finish route segment
    currentRoute.stations.push(clickedStation.id);
    currentRoute.path.push({
      lat: clickedStation.position.lat,
      lng: clickedStation.position.lng
    });
    finishRouteCreation();
  }
};

//Handle adding intermediate points
const onMapClickAddRoutePoint = (e) => {
  if (!routeCreationMode || !startRouteStation) return;
  
  currentRoute.path.push({
    lat: e.latlng.lat,
    lng: e.latlng.lng
  });
  
  // Update temporary route visualization
  if (!tempRoute) {
    tempRoute = L.polyline([], { color: 'red', dashArray: '5,5' }).addTo(map);
  }
  tempRoute.setLatLngs(currentRoute.path.map(p => [p.lat, p.lng]));
};

const getStationCoordinates = (stationData) => {
  if (!stationData) return null;

  // Case 1: Firestore GeoPoint format
  if (
    stationData.location &&
    typeof stationData.location.latitude === 'number' &&
    typeof stationData.location.longitude === 'number'
  ) {
    return {
      lat: stationData.location.latitude,
      lng: stationData.location.longitude
    };
  }

  // Case 2: Separate lat/lng fields
  if (
    typeof stationData.lat === 'number' &&
    typeof stationData.lng === 'number'
  ) {
    return {
      lat: stationData.lat,
      lng: stationData.lng
    };
  }

  // Case 3: Position object
  if (
    stationData.position &&
    typeof stationData.position.lat === 'number' &&
    typeof stationData.position.lng === 'number'
  ) {
    return {
      lat: stationData.position.lat,
      lng: stationData.position.lng
    };
  }

  // Case 4: Coordinates object
  if (
    stationData.coordinates &&
    typeof stationData.coordinates.lat === 'number' &&
    typeof stationData.coordinates.lng === 'number'
  ) {
    return {
      lat: stationData.coordinates.lat,
      lng: stationData.coordinates.lng
    };
  }

  return null;
};

const clearRouteFromMap = () => {
  routePolylines.forEach(polyline => {
    polyline.remove();
  });
  routePolylines = [];
  
  // Remove route station markers
  Object.keys(getMapState().markers).forEach(key => {
    if (key.startsWith('route_station_')) {
      removeMarker(key);
    }
  });
};

const finishRouteCreation = () => {
  const { map } = getMapState();
  
  if (tempRoute) {
    map.removeLayer(tempRoute);
    tempRoute = null;
  }
  
  // Prompt for route name and save
  const routeName = prompt('Enter route name:');
  if (routeName) {
    currentRoute.name = routeName;
    saveRoute(currentRoute);
  }
  
  routeCreationMode = false;
  startRouteStation = null;
  currentRoute = null;
  removeMapListeners();
};

const saveRoute = async (route) => {
  try {
    await addDoc(collection(db, 'routes'), {
      name: route.name,
      stations: route.stations,
      path: route.path,
      createdAt: serverTimestamp()
    });
    alert('Route saved successfully!');
    loadRoutes(); // Refresh the route list
  } catch (error) {
    console.error('Error saving route:', error);
    alert('Failed to save route');
  }
};

// const loadRouteStations = async () => {
//   try {
//     const snapshot = await getDocs(collection(db, 'stations'));
//     snapshot.forEach(doc => {
//       const data = doc.data();
//       let position = null;
      
//       // Handle all possible coordinate formats
//       if (data.location && data.location.latitude && data.location.longitude) {
//         // Geopoint format
//         position = {
//           lat: parseFloat(data.location.latitude),
//           lng: parseFloat(data.location.longitude)
//         };
//       } else if (data.lat && data.lng) {
//         // Separate number fields
//         position = {
//           lat: parseFloat(data.lat),
//           lng: parseFloat(data.lng)
//         };
//       } else if (data.position && data.position.lat && data.position.lng) {
//         // Position object format
//         position = {
//           lat: parseFloat(data.position.lat),
//           lng: parseFloat(data.position.lng)
//         };
//       }

//       if (position && !isNaN(position.lat) && !isNaN(position.lng)) {
//         routeStations[doc.id] = {
//           position: position,
//           data: data
//         };
//       } else {
//         console.warn(`Station ${doc.id} has invalid coordinates`, data);
//       }
//     });
//     displayRouteStations();
//   } catch (error) {
//     console.error("Error loading stations:", error);
//   }
// };

const loadRouteStations = async () => {
  try {
    const snapshot = await getDocs(collection(db, 'stations'));
    routeStations={};
    snapshot.forEach((doc) => {
      const data = doc.data();
      // const coords = getStationCoordinates(data);
      const coords=parseStationCoords(data);

      if (coords) {
        routeStations[doc.id] = {
          position: coords,
          data: data
        };
      } else {
        console.warn(`Station ${doc.id} has invalid coordinates format`, data);
      }
    });
    displayRouteStations();
  } catch (error) {
    console.error("Error loading stations:", error);
  }
};

export const loadRoutes = async () => {
  try {
    await loadRouteStations();
    const querySnapshot = await getDocs(collection(db, "routes"));
    const routes = querySnapshot.docs.map(doc => ({ id: doc.id, ...doc.data() }));
    
    // Populate route dropdown
    const routeSelect = document.getElementById('routeSelect');
    routeSelect.innerHTML = '<option value="">Select a route</option>';
    
    routes.forEach(route => {
      const option = document.createElement('option');
      option.value = route.id;
      option.textContent = route.name || `Route ${route.id}`;
      routeSelect.appendChild(option);
    });
    
    // Setup route selection handler
    routeSelect.addEventListener('change', (e) => {
      const routeId = e.target.value;
      if (!routeId) {
        clearRouteFromMap();
        return;
      }
      
      const selectedRoute = routes.find(r => r.id === routeId);
      if (selectedRoute) {
        displayRouteOnMap(selectedRoute);
      }
    });
  } catch (error) {
    console.error("Error loading routes:", error);
    alert("Failed to load routes. Please try again.");
  }
};

export const setupRouteManagement = () => {
  setMapState({ currentAction: 'routes' });
  // Clear existing markers first
  clearAllMarkers();
  routePolylines=[];
  document.getElementById('routeControls').style.display = 'block';
  
  // Initialize UI
  document.getElementById('createRouteBtn').addEventListener('click', startRouteCreation);
  

  // Load initial routes and show all stations
  loadRouteStations().then(loadRoutes);
  
  return {
    loadRoutes,
    cleanup: () => {
      clearRouteFromMap();
      if (tempRoute) tempRoute.remove();
      removeMapListeners();
    }
  };
};