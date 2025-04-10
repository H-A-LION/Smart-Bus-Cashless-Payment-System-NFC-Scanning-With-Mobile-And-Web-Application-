import { initAuth } from './modules/auth.js';
import { initAgentPage } from './pages/agent.js';
import { initBusPage } from './pages/bus.js';
import { initNfcPage } from './pages/nfc.js';
import { initMapPage } from './pages/map.js';

// Initialize authentication globally
initAuth();

document.addEventListener('DOMContentLoaded', () => {
  
  switch (true) {
    case !!document.getElementById('agentsTable'):
      initAgentPage();
      break;
      
    case !!document.getElementById('busTable'):
      initBusPage();
      break;
      
    case !!document.getElementById('nfcTable'):
      initNfcPage();
      break;
      
    case !!document.getElementById('map'):
      initMapPage();
      break;
      
    default:
      console.log('No page-specific JS required');
  }
});
