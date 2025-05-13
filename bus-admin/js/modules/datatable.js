//datatable.js
  export const initAgentTable = () => {
    return $('#agentsTable').DataTable({
      columns: [
        { data: 'id' },
        { data: 'name',  // Changed from 'name' to 'username'
          render: data => data || 'N/A'
        },
        { data: 'email' },
        { data: 'phone' },
        { data: 'balance',
           render: data => {
            const amount=typeof data === 'number' ? data:0;
            return `$${amount.toFixed(2)}`
           } 
        },
        { data: 'status',
          render: data => {
            const status=typeof data === 'string' ?data:'';
            return `<span class="status-badge ${data}">${status.charAt(0).toUpperCase() + status.slice(1)}</span>`
          }
        },
        { data: 'id',
          render: (data, type, row) => `
            <button class="btn-action edit-btn" data-id="${data}"><i class="fas fa-edit" title="Edit"></i></button>
            <button class="btn-action charge-btn" data-id="${data}"><i class="fas fa-coins" title="Charge"></i></button>
            <button class="btn-action verify-email-btn" data-id="${data}" data-email="${row.email}" ${row.emailVerified ? 'disabled' : ''}><i class="fas fa-envelope" title="${row.emailVerified ? 'Email Verified' : 'Verify Email'}"></i></button>
            <button class="btn-action verify-phone-btn" data-id="${data}" data-phone="${row.phone}" ${row.phoneVerified ? 'disabled' : ''}><i class="fas fa-phone" title="${row.phoneVerified ? 'Phone Verified' : 'Verify Phone'}"></i></button>
            <button class="btn-action delete-btn" data-id="${data}"><i class="fas fa-trash" title="Delete"></i></button>
           `,
            orderable: false
        }
      ],
      pageLength: 10,
      responsive: true,
      destroy:true,
      ajax: {             // Add this for dynamic reloading
        url: '',          // Leave empty if using Firestore listeners
        dataSrc: ''
      }
    });
  };
  
  
  export const initBusTable = () => {
    return $('#busTable').DataTable({
      columns: [
        { data: 'busId' },
        { 
          data: 'currentLocation',
          render: function(data, type, row) {
            console.log("Rendering location for bus:", row.busId, "Data:", data);
            if (!data) return 'No location';
            if (data.latitude === null || data.longitude === null) {
              if (data.lat !== null || data.lng !== null) {
                return `${data.lat}, ${data.lng}`;
              } else {
                return 'No coordinates';
              }
            }
            return `${data.latitude}, ${data.longitude}`;
          }
          
        },
        { 
          data: 'lastUpdated',
          render: function(data) {
            return data ? new Date(data).toLocaleString() : 'Never updated';
          }
        },
        {
          data: 'busId',
          render: (data) => `
            <button class="btn-action track-btn" data-id="${data}">
              <i class="fas fa-map-marker-alt"></i>
            </button>
            <button>
              <i class="fas fa-edit"></i>
            </button>
            <button class="btn-action delete-btn" data-id="${data}">
              <i class="fas fa-trash"></i>
            </button>
          `
        }
      ],
      createdRow: function(row, data, dataIndex) {
        console.log("Created row for bus:", data.busId, "with data:", data);
      }
    });
  };
  
  export const initNfcTable = () => {
    return $('#nfcTable').DataTable({
      columns: [
        { data: 'cardId' },
        { data: 'userId',
          render: data=> data ||'Unassigned'
        },
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
          `,
          orderable: false
        }
      ],
      pageLength: 10,
    responsive: true
    });
  };