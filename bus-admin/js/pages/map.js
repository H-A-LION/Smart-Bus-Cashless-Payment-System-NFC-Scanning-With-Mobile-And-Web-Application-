// map.js
import { L } from '../core/leaflet.js';
import { initMap,getMapState } from './modules/map-core.js';
import { setupStationManagement } from './modules/station-manager.js';
import { setupBusTracking } from './modules/bus-tracker.js';
import { setupRouteManagement } from './modules/route-manager.js';
import { setupLineManagement } from './modules/line-manager.js';

export const initMapPage = () => {
  console.log('Initializing map page');
  
  try {
    // Initialize map
    const map = initMap();
    
    
    // Handle different map views
    const params = new URLSearchParams(window.location.search);
    const action = params.get('action');
    const busId = params.get('busId');
    
    switch(action) {
      case 'stations':
        setupStationManagement();
        break;
      case 'buses':
        setupBusTracking(busId);
        break;
      case 'routes':
        setupRouteManagement();
        break;
      case 'lines':
        setupLineManagement();
        break;
      default:
        // Default map view
        console.log('No specific map action specified');
    }
  } catch (error) {
    console.error('Map initialization error:', error);
    alert('Failed to initialize map. Please try again.');
  }
};
