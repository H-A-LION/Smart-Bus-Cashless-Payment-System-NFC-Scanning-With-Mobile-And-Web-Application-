//agent.js
import { 
    db, collection, doc, getDoc, updateDoc, deleteDoc, onSnapshot, serverTimestamp, 
    auth, arrayUnion, increment, runTransaction
  } from "../core/firebase.js";
import { initAgentTable } from "../modules/datatable.js";
  
  // DOM Elements
  let agentsTable;
  let editModal;
  let chargeModal;
  const initModals = () => {
    chargeModal = document.getElementById('chargeUserModal');
    editModal = document.getElementById('editUserModal');
    // Add click outside to close
    window.addEventListener('click', (e) => {
      if (e.target === chargeModal) {
        chargeModal.style.display = 'none';
      }
    });
  };


  
  //core functions
  const loadAgents = async () => {
    if (agentsTable) {
      agentsTable.destroy(); // Clean up old instance
    }
    
    agentsTable = initAgentTable(); // Reinitialize
    
    onSnapshot(collection(db, 'users'), (snapshot) => {
    const users = snapshot.docs.map(doc => ({ 
      id: doc.id,
       ...doc.data(),
        name: doc.data().name || doc.data().username || 'N/A' 
      }));
    agentsTable.clear().rows.add(users).draw();
    });
      
      
    
    
  };


  const updatePaginationInfo = () => {
    const info = agentsTable.page.info();
    document.getElementById('paginationInfo').textContent = 
      `Showing ${info.start + 1} to ${info.end} of ${info.recordsTotal} agents`;
  };

  //Model Handler
  
  const showEditModal = async (agentId) => {
    const docSnap = await getDoc(doc(db, 'users', agentId));
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
    const userId = document.getElementById('editAgentId').value;
    
    try {
      await updateDoc(doc(db, 'users', userId), {
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

  const deleteAgent = async (userId) => {
    if (!confirm('Are you sure you want to delete this user?')) return;
    
    try {
      // Delete from Firestore
      await deleteDoc(doc(db, 'users', userId));
      
      // Optional: Delete from Firebase Auth (requires admin privileges)
      // await auth.deleteUser(userId); 
      
      alert('User deleted successfully');
    } catch (error) {
      console.error('Error deleting user:', error);
      alert(`Error: ${error.message}`);
    }
  };

  const showChargeModal = async (userId) => {
    try {
      const docSnap = await getDoc(doc(db, 'users', userId));
    if (!docSnap.exists()){
      alert('User not found!');
      return;
    } 
    const user=docSnap.data();
    document.getElementById('chargeAgentId').value = userId;
    const balanceDisplay=document.getElementById('currentBalance');
    if (balanceDisplay) {
      balanceDisplay.textContent = `Current Balance: $${user.balance?.toFixed(2) || '0.00'}`;
    }

    //Reset
    document.getElementById('chargeAmount').value = '';
    document.getElementById('chargeNotes').value = '';

    //show Modal
    chargeModal.style.display = 'block';      

    document.getElementById('chargeAmount').addEventListener('input', (e) => {
      if (e.target.value < 0) e.target.value = 0;
    });

    } catch (error) {
      console.error('Error loading user:', error);
    alert('Failed to load user data');
    }
    
    
    
  };

  const processCharge = async (e) => {
    e.preventDefault();
    const submitBtn=e.target.querySelector('button[type="submit"]');
    const userId = document.getElementById('chargeAgentId').value;

    try{
      submitBtn.disabled=true;
      submitBtn.innerHTML='<i class="fas fa-spinner fa-spin"></i> Processing...';

      const amount = parseFloat(document.getElementById('chargeAmount').value);
      const notes = document.getElementById('chargeNotes').value;

      if (amount > 10000) { // Example limit
        throw new Error('Maximum charge amount is $10000');
      }
      if (amount > 500) {
        await sendChargeNotificationEmail(userId, amount);
      }
      
      //Validate Input 
      if (isNaN(amount) || amount <= 0) {
        alert('Please enter a valid amount');
        return;
      }
  
    
      
      const userRef = doc(db, 'users', userId);
      // Process charge using Firestore transaction
    
    await runTransaction(db, async (transaction) => {
      const userDoc = await transaction.get(userRef);
      if (!userDoc.exists()) {
        throw new Error("User document does not exist!");
      }
      
      const currentBalance = userDoc.data().balance || 0;
      const newBalance = currentBalance + amount;
      
      transaction.update(userRef, {
        balance: newBalance,
        transactions: arrayUnion({
          type: 'credit',
          amount: amount,
          notes: notes,
          date: serverTimestamp(),
          processedBy: auth.currentUser.uid
        })
      });
    });
      
      
      await loadAgents();//Refresh the table
      chargeModal.style.display = 'none';
      alert(`Successfully charged $${amount.toFixed(2)} to user's balance`);
      
    } catch (error) {
      console.error('Error charging user:', error);
      alert(`Error: ${error.message}`);
    }finally{
      submitBtn.disabled = false;
      submitBtn.textContent = 'Process Charge';
    }
  };

  // Event Handlers
  const setupEventListeners = () => {
    initModals();
    // Table Controls
    document.getElementById('refreshAgentsBtn').addEventListener('click', loadAgents);
    document.getElementById('agentSearch').addEventListener('input', (e) => {
      agentsTable.search(e.target.value).draw();
    });
    // Edit Modals
    document.getElementById('editAgentForm').addEventListener('submit', saveAgentChanges);
    document.getElementById('cancelEditBtn').addEventListener('click', () => {
      editModal.style.display = 'none';
    }); 
    document.querySelector('#editUserModal .close').addEventListener('click', () => {
      chargeModal.style.display = 'none';
    });
    //Charge Modals
    document.getElementById('cancelChargeBtn').addEventListener('click', (e) => {
      e.preventDefault();
      chargeModal.style.display = 'none';
    });
    document.querySelector('#chargeUserModal .close').addEventListener('click', () => {
      chargeModal.style.display = 'none';
    });
    // Table Actions (Event Delegation)
    document.getElementById('agentsTable').addEventListener('click', async (e) => {
      const btn = e.target.closest('button');
      if (!btn) return;
      const userId = btn.dataset.id;
      if (btn.classList.contains('edit-btn')){
        showEditModal(userId);
        loadAgents();//refresh after edit 
      } 
      if (btn.classList.contains('delete-btn')) deleteAgent(userId);
      if (btn.classList.contains('charge-btn')){
        await showChargeModal(userId);
      } 
      if (btn.classList.contains('verify-email-btn')) {
        await verifyUserEmail(userId, btn.dataset.email);
      }
      if (btn.classList.contains('verify-phone-btn')) {
        await verifyUserPhone(userId, btn.dataset.phone);
      }
    });
  };

  export const initAgentPage = () => {
    agentsTable = initAgentTable();
    setupEventListeners();
    loadAgents();
  };

  // Add this to your user management page
  export const verifyAgentEmail = async (userId, email) => {
  try {
    // In a real implementation, you would need to fetch the user record
    // This is simplified - you'd need proper admin auth to do this
    const user = await auth.getUser(userId);
    await sendEmailVerification(user);
    alert(`Verification email sent to ${email}`);
  } catch (error) {
    console.error("Verification error:", error);
    alert(`Error: ${error.message}`);
  }
  };
  // Add these functions to agent.js
  export const verifyUserEmail = async (userId, email) => {
  try {
    // In a real app, you would use Firebase Admin SDK on backend
    const user = auth.currentUser;
    if (!user || user.uid === userId) {
      throw new Error("Admin authentication required");
    }

    // This is a frontend simulation - in reality you'd call a backend function
    const userDoc = await getDoc(doc(db, "users", userId));
    if (!userDoc.exists()) throw new Error("User not found");
   
    // This requires the user's auth token - only works if admin has access
    const user1 = await auth.getUser(userId); // Note: This requires Firebase Admin SDK in production
    await sendEmailVerification(user1);

    // // Simulate verification
    // await updateDoc(doc(db, "users", userId), {
    //   emailVerified: true,
    //   emailVerifiedAt: new Date()
    // });
    
    alert(`Verification email re-sent to ${email}`);
  } catch (error) {
    console.error("Email verification error:", error);
    alert(`Error: ${error.message}`);
  }
  };
  export const verifyUserPhone = async (userId, phone) => {
    try {
      // Similar to email verification but for phone
      const userDoc = await getDoc(doc(db, "users", userId));
      if (!userDoc.exists()) throw new Error("User not found");
      
      // In real implementation:
      // 1. Call a cloud function
      // 2. Function would send verification SMS
      // 3. Update Firestore when verified
      
      // Simulate verification
      await updateDoc(doc(db, "users", userId), {
        phoneVerified: true,
        phoneVerifiedAt: new Date()
      });
      
      alert(`Phone number ${phone} marked as verified`);
    } catch (error) {
      console.error("Phone verification error:", error);
      alert(`Error: ${error.message}`);
    }
  };
  export const adminOverrideEmailVerification = async (userId) => {
    if (!confirm("Bypass email verification for this user?")) return;
    
    try {
      await updateDoc(doc(db, "users", userId), {
        emailVerified: true, // Only in Firestore!
        emailVerifiedAt: serverTimestamp()
      });
      alert("Marked as verified in system (not in Firebase Auth)");
    } catch (error) {
      alert("Error: " + error.message);
    }
  };