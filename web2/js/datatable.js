export const initAgentTable = () => {
    return $('#agentsTable').DataTable({
      columns: [
        { data: 'id' },
        { data: 'name' },
        { data: 'email' },
        { data: 'phone' },
        { data: 'balance', render: data => `$${data.toFixed(2)}` },
        { 
          data: 'status',
          render: data => `<span class="status-badge ${data}">${data.charAt(0).toUpperCase() + data.slice(1)}</span>`
        },
        { 
          data: 'id',
          render: (data) => `
            <button class="btn-action edit-btn" data-id="${data}"><i class="fas fa-edit"></i></button>
            <button class="btn-action charge-btn" data-id="${data}"><i class="fas fa-coins"></i></button>
            <button class="btn-action delete-btn" data-id="${data}"><i class="fas fa-trash"></i></button>
          `,
          orderable: false
        }
      ],
      pageLength: 10,
      responsive: true
    });
  };
  
  export const initBusTable = () => {
    return $('#busTable').DataTable({
      columns: [
        { data: 'busId' },
        { 
          data: 'currentLocation', 
          render: data => data ? `${data.latitude}, ${data.longitude}` : 'N/A'
        },
        { 
          data: 'lastUpdated', 
          render: data => data ? new Date(data).toLocaleString() : 'Never'
        },
        {
          data: 'busId',
          render: (data) => `
            <button class="btn-action track-btn" data-id="${data}"><i class="fas fa-map-marker-alt"></i></button>
            <button class="btn-action delete-btn" data-id="${data}"><i class="fas fa-trash"></i></button>
          `
        }
      ]
    });
  };
  
  export const initNfcTable = () => {
    return $('#nfcTable').DataTable({
      columns: [
        { data: 'cardId' },
        { data: 'userId', defaultContent: 'N/A' },
        { 
          data: 'balance', 
          render: data => `$${data?.toFixed(2) || '0.00'}`
        },
        { 
          data: 'isActive', 
          render: data => `
            <span class="status-badge ${data ? 'active' : 'inactive'}">
              ${data ? 'Active' : 'Inactive'}
            </span>`
        },
        {
          data: 'cardId',
          render: (data) => `
            <button class="btn-action edit-btn" data-id="${data}">
              <i class="fas fa-edit"></i>
            </button>
            <button class="btn-action delete-btn" data-id="${data}">
              <i class="fas fa-trash"></i>
            </button>
          `
        }
      ]
    });
  };