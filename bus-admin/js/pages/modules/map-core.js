// map-core.js
import { L } from '../../core/leaflet.js';

// Shared state
let map;
let markers = {};
let editMode = false;
let currentAction = null;
let clickMarker = null;

// Initialize the map
export const initMap = () => {
  if (map) return map; // Return if already initialized
  
  map = L.map('map').setView([33.18898, 35.30609], 13);
  
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    maxZoom: 19,
    attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
  }).addTo(map);

  return map;
};

// Marker management
// export const addMarker = (id, latlng, options = {}) => {
//   if (markers[id]) removeMarker(id);
  
//   markers[id] = L.marker(latlng, options).addTo(map);
//   return markers[id];
// };
export const addMarker = (id, coords, options = {}) => {
  // Normalize and validate coordinates
  const normalized = normalizeCoordinates(coords);
  if (!normalized || !validateCoordinates(normalized)) {
    console.error(`Invalid coordinates for marker ${id}:`, coords);
    return null;
  }

  if (markers[id]) removeMarker(id);
  
  try {
    markers[id] = L.marker([normalized.lat, normalized.lng], options).addTo(map);
    return markers[id];
  } catch (error) {
    console.error(`Failed to add marker ${id}:`, error);
    return null;
  }
};

export const removeMarker = (id) => {
  if (markers[id]) {
    map.removeLayer(markers[id]);
    delete markers[id];
  }
};

export const clearAllMarkers = () => {
  Object.values(markers).forEach(marker => map.removeLayer(marker));
  markers = {};
};

export const clearAllLines = () => {
  Object.keys(markers).forEach(key => {
    if (key.startsWith('line_')) {
      map.removeLayer(markers[key]);
      delete markers[key];
    }
  });
};

// State management
export const getMapState = () => ({
  map,
  markers,
  editMode,
  currentAction,
  clickMarker
});

export const setMapState = (newState) => {
  if (newState.map !== undefined) map = newState.map;
  if (newState.markers !== undefined) markers = newState.markers;
  if (newState.editMode !== undefined) editMode = newState.editMode;
  if (newState.currentAction !== undefined) currentAction = newState.currentAction;
  if (newState.clickMarker !== undefined) clickMarker = newState.clickMarker;
};

// Utility functions
export const setupMapListeners = (handlers) => {
  if (!map) return;
  
  // Clear existing listeners
  map.off('click');
  map.off('contextmenu');
  map.off('mousemove');
  if (handlers.onClick) {
    map.on('click', (e) => {
      try {
        handlers.onClick(e);
      } catch (error) {
        console.error('Error in click handler:', error);
      }
    });
  }
  // Set new listeners
  if (handlers.onClick) map.on('click', handlers.onClick);
  if (handlers.onRightClick) map.on('contextmenu', handlers.onRightClick);
  if (handlers.onMouseMove) map.on('mousemove', handlers.onMouseMove);
};

export const removeMapListeners = () => {
  if (!map) return;
  map.off('click');
  map.off('contextmenu');
  map.off('mousemove');
};

export const ensureLatLng = (latlng) => {
  if (latlng instanceof L.LatLng) return latlng;
  if (validateCoordinates(latlng)) return L.latLng(latlng.lat, latlng.lng);
  return null;
};
// Add these utility functions to map-core.js
export const normalizeCoordinates = (coords) => {
  if (!coords) return null;
  
  // Handle array format [lat, lng]
  if (Array.isArray(coords) ){
    return {
      lat: parseFloat(coords[0]),
      lng: parseFloat(coords[1])
    };
  }
  
  // Handle Leaflet LatLng object
  if (coords.lat !== undefined && coords.lng !== undefined) {
    return {
      lat: parseFloat(coords.lat),
      lng: parseFloat(coords.lng)
    };
  }
  
  // Handle GeoPoint format {latitude, longitude}
  if (coords.latitude !== undefined && coords.longitude !== undefined) {
    return {
      lat: parseFloat(coords.latitude),
      lng: parseFloat(coords.longitude)
    };
  }
  
  return null;
};

// map-core.js (add these functions)

/**
 * Universal station coordinate parser - handles all Firestore format variations
 * @param {Object} stationData - Station document from Firestore
 * @returns {Object|null} Normalized {lat, lng} or null if invalid
 */
export const parseStationCoords = (stationData) => {
  if (!stationData) return null;

  // Case 1: Firestore GeoPoint
  if (stationData.location?.latitude && stationData.location?.longitude) {
    return {
      lat: parseFloat(stationData.location.latitude),
      lng: parseFloat(stationData.location.longitude)
    };
  }

  // Case 2: Separate lat/lng fields
  if (stationData.lat != null && stationData.lng != null) {
    return {
      lat: parseFloat(stationData.lat),
      lng: parseFloat(stationData.lng)
    };
  }

  // Case 3: position object
  if (stationData.position?.lat && stationData.position?.lng) {
    return {
      lat: parseFloat(stationData.position.lat),
      lng: parseFloat(stationData.position.lng)
    };
  }

  // Case 4: coordinates object
  if (stationData.coordinates?.lat && stationData.coordinates?.lng) {
    return {
      lat: parseFloat(stationData.coordinates.lat),
      lng: parseFloat(stationData.coordinates.lng)
    };
  }

  return null;
};

export const validateCoordinates = (coords) => {
  if (!coords) return false;
  const normalized = normalizeCoordinates(coords);
  if (!normalized) return false;
  
  return !isNaN(normalized.lat) && 
         !isNaN(normalized.lng) &&
         Math.abs(normalized.lat) <= 90 &&
         Math.abs(normalized.lng) <= 180;
};