import { 
    db, collection, doc, getDoc, updateDoc, deleteDoc, onSnapshot, serverTimestamp 
  } from "../core/firebase.js";
  import { initAgentTable } from "../modules/datatable.js";
  
    // DOM Elements
    let agentsTable;
    const editModal = document.getElementById('editAgentModal');
    const chargeModal = document.getElementById('chargeAgentModal');
  
    //core functions
  const loadAgents = async () => {
    onSnapshot(collection(db, 'agents'), (snapshot) => {
      const agents = snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() }));
      agentsTable.clear().rows.add(agents).draw();
    });
  };
  const updatePaginationInfo = () => {
    const info = agentsTable.page.info();
    document.getElementById('paginationInfo').textContent = 
      `Showing ${info.start + 1} to ${info.end} of ${info.recordsTotal} agents`;
  };

  //Model Handler
  
  const showEditModal = async (agentId) => {
    const docSnap = await getDoc(doc(db, 'agents', agentId));
    if (!docSnap.exists()) return;
    
    const agent = docSnap.data();
    document.getElementById('editAgentId').value = agentId;
    document.getElementById('editAgentName').value = agent.name || '';
    document.getElementById('editAgentEmail').value = agent.email || '';
    document.getElementById('editAgentPhone').value = agent.phone || '';
    document.getElementById('editAgentBalance').value = agent.balance || 0;
    document.getElementById('editAgentStatus').value = agent.status || 'active';
    editModal.style.display = 'block';
  };

  const saveAgentChanges = async (e) => {
    e.preventDefault();
    const agentId = document.getElementById('editAgentId').value;
    
    try {
      await updateDoc(doc(db, 'agents', agentId), {
        name: document.getElementById('editAgentName').value,
        email: document.getElementById('editAgentEmail').value,
        phone: document.getElementById('editAgentPhone').value,
        balance: parseFloat(document.getElementById('editAgentBalance').value),
        status: document.getElementById('editAgentStatus').value,
        updatedAt: serverTimestamp()
      });
      editModal.style.display = 'none';
    } catch (error) {
      console.error('Error updating agent:', error);
    }
  };

    // Event Handlers
    const setupEventListeners = () => {
    // Table Controls
    document.getElementById('refreshAgentsBtn').addEventListener('click', loadAgents);
    document.getElementById('agentSearch').addEventListener('input', (e) => {
      agentsTable.search(e.target.value).draw();
    });
    
    // Modals
    document.getElementById('editAgentForm').addEventListener('submit', saveAgentChanges);
    document.getElementById('cancelEditBtn').addEventListener('click', () => {
      editModal.style.display = 'none';
    });
    
    // Table Actions (Event Delegation)
    document.getElementById('agentsTable').addEventListener('click', (e) => {
      const btn = e.target.closest('button');
      if (!btn) return;
      
      const agentId = btn.dataset.id;
      if (btn.classList.contains('edit-btn')) showEditModal(agentId);
      if (btn.classList.contains('delete-btn')) deleteAgent(agentId);
    });
     };
  
  export const initAgentPage = () => {
    agentsTable = initAgentTable();
    setupEventListeners();
    loadAgents();
  };