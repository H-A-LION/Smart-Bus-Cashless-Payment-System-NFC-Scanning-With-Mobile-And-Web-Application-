// line-manager.js
import {
  db,
  collection,
  doc,
  getDoc,
  addDoc,
  updateDoc,
  deleteDoc,
  onSnapshot,
  getDocs,
  serverTimestamp,
} from "../../core/firebase.js";

import {
  getMapState,
  setMapState,
  addMarker,
  removeMarker,
  clearAllMarkers,
  setupMapListeners,
  removeMapListeners,
  clearAllLines, 
  normalizeCoordinates,
  validateCoordinates, parseStationCoords
} from "./map-core.js";

let lineCreationMode = false;
let currentLine = null;
let tempLine = null;
let tempMarkers = [];
let startStation = null;
let lineStations = {};
let map;


const findNearestLineStation = (clickPoint, maxDistance = 30) => {
  const { map } = getMapState();
  
  // Validate input coordinates
  const normalizedClick = normalizeCoordinates(clickPoint);
  if (!normalizedClick || !validateCoordinates(normalizedClick)) {
    console.error('Invalid click coordinates:', clickPoint);
    return null;
  }

  let closestStation = null;
  let minDistance = Infinity;

  Object.entries(lineStations).forEach(([id, station]) => {
    try {
      if (!station?.position) {
        console.warn(`Station ${id} has no position data`);
        return;
      }

      // Normalize station coordinates
      const stationCoords = normalizeCoordinates(station.position);
      if (!stationCoords || !validateCoordinates(stationCoords)) {
        console.warn(`Station ${id} has invalid coordinates`, station.position);
        return;
      }

      // Create Leaflet LatLng objects
      const clickLatLng = L.latLng(normalizedClick.lat, normalizedClick.lng);
      const stationLatLng = L.latLng(stationCoords.lat, stationCoords.lng);

      // Calculate distance
      const distance = map.distance(clickLatLng, stationLatLng);

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

const startLineCreation = () => {
  const { map : mapInstance }= getMapState();
  map = mapInstance; // Store the map reference
  lineCreationMode = true;
  currentLine = {
    name: "",
    stations: [],
    routes: [],
    createdAt: serverTimestamp(),
  };
  document.getElementById("lineStatus").textContent =
    "Right-click on a station to start";

  setupMapListeners({
    onRightClick: onStationSelectForLine,
    onMouseMove: updateTempLine,
  });
};

const updateTempLine = (e) => {
  if (!lineCreationMode || !startStation) return;

  const { map } = getMapState();
  const station = lineStations[startStation.id];

  if (!station?.position) {
    console.error('Start station position not found');
    return;
  }

  // Create LatLng objects for the start station and current mouse position
  var startLatLng = L.latLng(station.position.lat, station.position.lng);
  var endLatLng = e.latlng;

  if (!tempLine) {
    // Initialize the temporary line if it doesn't exist
    tempLine = L.polyline([startLatLng, endLatLng], {
      color: "blue",
      weight: 5,
      dashArray: "5,5",
      className: "temp-route-line"
    }).addTo(map);
  } else {
    // Update the existing temporary line
    tempLine.setLatLngs([startLatLng, endLatLng]);
  }
};

const displayLineStations = () => {
  const { map } = getMapState();
  Object.entries(lineStations).forEach(([id, station]) => {
    try {
      if (!station?.position) {
        console.warn(`Station ${id} has no position data`);
        return;
      }

      // Normalize coordinates
      const coords = normalizeCoordinates(station.position);

      if (!coords || !validateCoordinates(coords)) {
        console.warn(`Station ${id} has invalid coordinates`, station.position);
        return;
      }

      addMarker(
        `line_station_${id}`,
        [coords.lat, coords.lng],
        {
          icon: L.divIcon({
            className: "line-station-marker",
            html: '<i class="fas fa-map-marker-alt"></i>'
          }),
          data: station.data
        }
      );
      
    } catch (error) {
      console.error(`Error displaying station ${id}:`, error);
    }
  });
};

// Modify the onStationSelectForLine function to handle intermediate stations
const onStationSelectForLine = (e) => {
  if (!lineCreationMode) return;
  
  // Normalize click coordinates
  const clickCoords = normalizeCoordinates(e.latlng);
  if (!clickCoords || !validateCoordinates(clickCoords)) {
    console.error('Invalid click coordinates:', e.latlng);
    return;
  }

  const clickedStation = findNearestLineStation(clickCoords);
  if (!clickedStation) {
    console.log("No valid station found nearby");
    alert("Please right-click directly on a valid station marker");
    return;
  }

  if (!startStation) {
    // First station selected - start line creation
    startStation = clickedStation;
    currentLine.stations.push(clickedStation.id);
    document.getElementById("lineStatus").textContent =
      "Left-click to add route points, right-click on another station to continue";

    // Setup mouse move listener for temporary line visualization
    // map.on("mousemove", updateTempLine);
  } else {
    // Another station selected - add it to the route
    if (clickedStation.id !== startStation.id) {
      completeRouteSegment(clickedStation);
    } else {
      // Same station clicked - finish line creation
      completeLineCreation();
    }
  }
};

// In line-manager.js, modify the completeRouteSegment function
const completeRouteSegment = (endStation) => {
  const { map } = getMapState();

  // Create the route segment
  const routeSegment = {
    from: startStation.id,
    to: endStation.id,
    path: [
      new GeoPoint(startStation.position.lat, startStation.position.lng),
      new GeoPoint(endStation.position.lat, endStation.position.lng)
    ]
  };
  currentLine.routes.push(routeSegment);

  // Add the end station if not already in the line
  if (!currentLine.stations.includes(endStation.id)) {
    currentLine.stations.push(endStation.id);
  }
  // Draw the permanent line
  const polyline = L.polyline(
    [
      [startStation.position.lat, startStation.position.lng],
      [endStation.position.lat, endStation.position.lng]
    ],
    {
      color: "#3498db",
      weight: 4,
      className: "bus-route"
    }
  ).addTo(map);
  // Clean up temporary visuals
  if (tempLine) {
    map.removeLayer(tempLine);
    tempLine = null;
  }
  // Set the end station as the new start for the next segment
  startStation = endStation;
  document.getElementById("lineStatus").textContent =
    "Continue adding route or right-click to finish";
};

// Modify the completeLineCreation function to save the correct structure
const completeLineCreation = async () => {
  const { map } = getMapState();

  if (tempLine) map.removeLayer(tempLine);
  tempMarkers.forEach((marker) => map.removeLayer(marker));

  // Save the line with entered name
  const lineName = document.getElementById("lineName").value || `Line ${new Date().toLocaleString()}`;
  currentLine.name = lineName;
  // Create the desired structure
  // const lineToSave = {
  //   name: lineName,
  //   stations: currentLine.stations, // This now includes all intermediate stations
  //   route: {
  //     0: currentLine.routes.flatMap(route => 
  //       route.path.map(point => 
  //         new firebase.firestore.GeoPoint(point.lat, point.lng)
  //       )
  //     )
  //   },
  //   createdAt: serverTimestamp()
  // };

  try {
    await addDoc(collection(db, "lines"), lineToSave);
    alert("Line saved successfully!");
    resetLineCreation();
    loadLines(); // Refresh the lines display
  } catch (error) {
    console.error("Error saving line:", error);
    alert("Failed to save line");
  }
};

const renderLine = (lineId, lineData) => {
  const { map } = getMapState();

  // Handle both old and new data formats
  const routes = lineData.routes || (lineData.route ? [{ path: Object.values(lineData.route).flat() }] : []);

  // Render stations
  lineData.stations.forEach((stationId) => {
    const station = lineStations[stationId];
    if (station) {
      let marker = getMapState().markers[`line_station_${stationId}`];
      if (!marker) {
        marker = addMarker(
          `line_station_${stationId}`,
          [station.position.lat, station.position.lng],
          {
            icon: L.divIcon({
              className: "station-marker",
              html: '<i class="fas fa-map-marker-alt"></i>',
            }),
            data: station.data,
          }
        );
      }
      marker.setIcon(
        L.divIcon({
          className: "station-marker line-station",
          html: `<i class="fas fa-map-marker-alt"></i>
          <span class="line-badge">${lineData.name}</span>`,
        })
      );
    }
  });

  // Render routes
  lineData.routes.forEach((route) => {
    try {
      const path = route.path.map(point => {
        // Handle both GeoPoint and regular objects
        const lat = point.latitude || point.lat;
        const lng = point.longitude || point.lng;
        
        if (typeof lat !== 'number' || typeof lng !== 'number') {
          throw new Error(`Invalid coordinates: ${JSON.stringify(point)}`);
        }
        return [lat, lng];
      });

      if (path.length < 2) {
        console.error('Route has insufficient valid points', route);
        return;
      }

      const polyline = L.polyline(path, {
        color: "#3498db",
        weight: 4,
        className: "bus-route",
      }).addTo(map);

      polyline.on("click", (e) => {
        if (confirm("Delete this route segment?")) {
          deleteRouteSegment(lineId, route);
          polyline.remove();
        }
      });
    } catch (error) {
      console.error('Failed to render route polyline:', error);
    }
  });
  
};

const loadLineStations = async () => {
  try {
    const snapshot = await getDocs(collection(db, "stations"));
    lineStations={};
    snapshot.forEach((doc) => {
      const data = doc.data();
      const coords = parseStationCoords(data);

      if (coords) {
        lineStations[doc.id] = {
          position: coords,
          data: data,
        };
      } else {
        console.warn(`Station ${doc.id} has invalid coordinates format`, data);
      }
    });
    displayLineStations();
  } catch (error) {
    console.error("Error loading stations:", error);
  }
};


const loadLines = async () => {
  try {
    await loadLineStations();
    const snapshot = await getDocs(collection(db, "lines"));
    snapshot.forEach((doc) => {
      try {
        const lineData = doc.data();       
        renderLine(doc.id, lineData);
      } catch (error) {
        console.error(`Error rendering line ${doc.id}:`, error);
      }
    });
  } catch (error) {
    console.error("Error loading lines:", error);
    alert("Failed to load lines. Please check console for details.");
  }
};

const resetLineCreation = () => {
  const { map } = getMapState();

  if (tempLine) map.removeLayer(tempLine);
  tempMarkers.forEach((marker) => map.removeLayer(marker));

  tempLine = null;
  tempMarkers = [];
  currentLine = null;
  startStation = null;
  lineCreationMode = false;

  removeMapListeners();
};

const cancelLineCreation = () => {
  resetLineCreation();
  document.getElementById("lineStatus").textContent = "Line creation canceled";
};

const saveCurrentLine = async () => {
  if (!currentLine || currentLine.routes.length === 0) return;

  currentLine.name =
    document.getElementById("lineName").value ||
    `Line ${new Date().toLocaleString()}`;

  try {
    await addDoc(collection(db, "lines"), currentLine);
    alert("Line saved successfully!");
    resetLineCreation();
  } catch (error) {
    console.error("Error saving line:", error);
    alert("Failed to save line");
  }
};

export const setupLineManagement = () => {
  setMapState({ currentAction: "lines" });
  document.getElementById("lineControls").style.display = "block";

  // Initialize UI
  document
    .getElementById("createLineBtn")
    .addEventListener("click", startLineCreation);
  document
    .getElementById("saveLineBtn")
    .addEventListener("click", saveCurrentLine);
  document
    .getElementById("cancelLineBtn")
    .addEventListener("click", cancelLineCreation);

  // Clear existing markers first
  clearAllMarkers();
  // Load existing lines and show all stations
  loadLineStations().then(loadLines);
  // loadLines();

  return {
    cleanup: () => {
      resetLineCreation();
      clearAllLines();
    },
  };
};