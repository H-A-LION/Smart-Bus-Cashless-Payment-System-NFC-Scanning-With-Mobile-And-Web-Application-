//main.js
import { initAuth } from './modules/auth.js';
import { initAgentPage } from './pages/agent.js';
import { initBusPage } from './pages/bus.js';
import { initNfcPage } from './pages/nfc.js';
import { initMapPage } from './pages/map.js';
import { initRegAgentPage} from './pages/reg-agent.js';
import { initForgotPasswordPage} from './pages/forgot-password.js';


document.addEventListener('DOMContentLoaded', () => {
  // Initialize authentication globally
  initAuth();

  // // Initialize page-specific scripts
  // Use requestIdleCallback for non-critical initialization
  requestIdleCallback(() => {
    // Initialize page-specific scripts with a small delay
    setTimeout(() => {
      const pageInitializers = {
        'agentsTable': initAgentPage,
        'busTable': initBusPage,
        'nfcTable': initNfcPage,
        'map': initMapPage,
        'agentForm': initRegAgentPage,
        'resetPasswordForm': initForgotPasswordPage
      };
      
      for (const [id, initializer] of Object.entries(pageInitializers)) {
        if (document.getElementById(id)) {
          initializer();
          break; // Only initialize one page at a time
        }
      }
    }, 50);
  });
});
