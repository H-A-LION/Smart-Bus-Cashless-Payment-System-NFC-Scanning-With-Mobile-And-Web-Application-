// modules/map-utils.js
export const createStationMarker = (coords, options = {}) => {
    return L.marker(coords, {
      draggable: options.editable,
      icon: L.divIcon({ className: 'station-marker', html: '<i class="fas fa-map-marker-alt"></i>' })
    });
  };