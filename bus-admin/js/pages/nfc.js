//nfc.js
import { db, getDoc, setDoc, collection, addDoc, getDocs,
   doc, updateDoc , serverTimestamp, query, where, deleteDoc, deleteField } from "../core/firebase.js";

import { initNfcTable } from "../modules/datatable.js";

let nfcTable;

const loadNfcCards = async () => {
  try {
    const usersSnapshot = await getDocs(collection(db, "users"));
    let allCards = [];

    for (const userDoc of usersSnapshot.docs) {
      const cardsSnapshot = await getDocs(collection(db, "users", userDoc.id, "cards"));
      
      cardsSnapshot.forEach(cardDoc => {
        allCards.push({
          id: cardDoc.id,
          cardId: cardDoc.id,  // Ensure this matches DataTables expected field
          userId: userDoc.id,
          ...cardDoc.data()
        });
      });
    }
    // Also get unassigned cards if needed
    const unassignedSnapshot = await getDocs(collection(db, "unassignedCards"));
    unassignedSnapshot.forEach(doc => {
      allCards.push({
        id: doc.id,
        cardId: doc.id,
        userId: null,
        ...doc.data()
      });
    });
    nfcTable.clear().rows.add(allCards).draw();
  } catch (error) {
    console.error("Error loading NFC cards:", error);
    alert("Failed to load NFC cards. Please try again.");
  }  
};

const addNfcCard = async (e) => {
  e.preventDefault();
  try {
    const cardId = document.getElementById('cardId').value.trim();
    const userId = document.getElementById('userId').value.trim();
    const balance = parseFloat(document.getElementById('balance').value) || 0;
    const isActive = document.getElementById('isActive').checked;

    if (!cardId) {
      alert("Card ID is required");
      return;
    }
    if (userId) {
      // Check if user exists
      const userRef = doc(db, "users", userId);
      const userDoc = await getDoc(userRef);
      
      if (!userDoc.exists()) {
        alert("User not found");
        return;
      }

      // Add card to user's cards subcollection
      await setDoc(doc(db, "users", userId, "cards", cardId), {
        balance,
        isActive,
        createdAt: serverTimestamp()
      });
    } else {
      // Add to unassigned cards collection
      await setDoc(doc(db, "unassignedCards", cardId), {
        balance,
        isActive,
        createdAt: serverTimestamp()
      });
    }
    document.getElementById('nfcModal').style.display = 'none';
    document.getElementById('nfcForm').reset();
    loadNfcCards();
  } catch (error) {
    console.error("Error adding NFC card:", error);
  }
};

const deleteNfcCard = async (cardId,userId=null) => {
  if (!confirm(`Are you sure you want to delete card ${cardId}?`)) return;
  console.log('confirm');
  try {
      if (userId) {
        const cardRef = doc(db, "users", userId, "cards", cardId);
        await deleteDoc(cardRef);
        console.log(`Deleted card ${cardId} from user ${userId}'s collection`);
      }else{
        // If not found in users, check unassigned cards
        const unassignedRef = doc(db, "unassignedCards", cardId);
        await deleteDoc(unassignedRef);
        console.log(`Deleted unassigned card ${cardId}`);
      }
    //Reload the table to reflet changes
    loadNfcCards();
  } catch (error) {
    console.error("Error deleting NFC card:", error);
    if (error.code === 'not-found' && userId) {
      console.log(`Card not found in user's collection, trying unassigned...`);
      try {
        const unassignedRef = doc(db, "unassignedCards", cardId);
        await deleteDoc(unassignedRef);
        loadNfcCards();
        return;
      } catch (unassignedError) {
        console.error("Error deleting from unassigned:", unassignedError);
        throw unassignedError;
      }
    }
    throw error;
  }
};

const setupEventListeners = () => {
  // Add NFC Card button
  document.getElementById('addNfcBtn').addEventListener('click', () => {
    const form = document.getElementById('nfcForm');
    form.reset();
    delete form.dataset.mode;
    delete form.dataset.cardId;
    delete form.dataset.userId;
    document.querySelector('#nfcModal h2').textContent = 'Register NFC Card';
    document.getElementById('nfcModal').style.display = 'block';
  });

  // NFC Form submission
  document.getElementById('nfcForm').addEventListener('submit', (e)=>{
    const form = e.target;
    if (form.dataset.mode === 'edit') {
      updateNfcCard(e);
    } else {
      addNfcCard(e);
    }
  });

  // Cancel button
  document.getElementById('cancelNfcBtn').addEventListener('click', () => {
    document.getElementById('nfcModal').style.display = 'none';
    document.getElementById('nfcForm').reset();
  });

  window.addEventListener('click', (e) => {
    if (e.target === document.getElementById('nfcModal')) {
      document.getElementById('nfcModal').style.display = 'none';
    }
  });

  // Add search functionality 
  document.getElementById('searchNfc').addEventListener('input', (e) => {
    nfcTable.search(e.target.value).draw();
  });

  // Edit/Delete buttons (using event delegation)
  document.getElementById('nfcTable').addEventListener('click', async (e) => {
    const editBtn = e.target.closest('.edit-btn');
    const deleteBtn = e.target.closest('.delete-btn');

    
    if (editBtn) {
      const cardId = editBtn.dataset.id;
      console.log("Edit card:", cardId);
      await editNfcCard(cardId);
    }
    if (deleteBtn) {
      // Get the row data which contains both cardId and userId
      const row = $(deleteBtn).closest('tr');
      const rowData = nfcTable.row(row).data();
      const cardId = deleteBtn.dataset.id;
      // Pass both cardId and userId (if exists) to delete function
      await deleteNfcCard(rowData.cardId, rowData.userId || null);
    }
      
    
  });

};

const editNfcCard = async (cardId) => {
  try {
    // First check if card is assigned to a user
    const usersQuery = query(collection(db, "users"));
    const usersSnapshot = await getDocs(usersQuery);
    
    for (const userDoc of usersSnapshot.docs) {
      const cardRef = doc(db, "users", userDoc.id, "cards", cardId);
      const cardDoc = await getDoc(cardRef);
      
      if (cardDoc.exists()) {
        // Populate the form with existing data
        document.getElementById('cardId').value = cardId;
        document.getElementById('userId').value = userDoc.id;
        document.getElementById('balance').value = cardDoc.data().balance;
        document.getElementById('isActive').checked = cardDoc.data().isActive;
        
        // Change the form to update mode
        const form = document.getElementById('nfcForm');
        form.dataset.mode = 'edit';
        form.dataset.cardId = cardId;
        form.dataset.userId = userDoc.id;
        
        document.getElementById('nfcModal').style.display = 'block';
        document.querySelector('#nfcModal h2').textContent = 'Edit NFC Card';
        return;
      }
    }
    
    // Check unassigned cards
    const unassignedRef = doc(db, "unassignedCards", cardId);
    const unassignedDoc = await getDoc(unassignedRef);
    
    if (unassignedDoc.exists()) {
      document.getElementById('cardId').value = cardId;
      document.getElementById('userId').value = '';
      document.getElementById('balance').value = unassignedDoc.data().balance;
      document.getElementById('isActive').checked = unassignedDoc.data().isActive;
      
      const form = document.getElementById('nfcForm');
      form.dataset.mode = 'edit';
      form.dataset.cardId = cardId;
      form.dataset.userId = '';
      
      document.getElementById('nfcModal').style.display = 'block';
      document.querySelector('#nfcModal h2').textContent = 'Edit NFC Card';
      return;
    }
    
    alert('Card not found');
  } catch (error) {
    console.error("Error editing NFC card:", error);
    alert("Failed to load card details. Please try again.");
  }
};

const updateNfcCard = async (e) => {
  e.preventDefault();
  try {
    const form = e.target;
    const cardId = form.dataset.cardId;
    const oldUserId = form.dataset.userId;
    const newUserId = document.getElementById('userId').value.trim();
    const balance = parseFloat(document.getElementById('balance').value) || 0;
    const isActive = document.getElementById('isActive').checked;

    if (oldUserId === newUserId) {
      // Only update balance and status if user hasn't changed
      if (oldUserId) {
        await updateDoc(doc(db, "users", oldUserId, "cards", cardId), {
          balance,
          isActive
        });
      } else {
        await updateDoc(doc(db, "unassignedCards", cardId), {
          balance,
          isActive
        });
      }
    } else {
      // User has changed - need to move the card
      if (oldUserId) {
        await deleteDoc(doc(db, "users", oldUserId, "cards", cardId));
      } else {
        await deleteDoc(doc(db, "unassignedCards", cardId));
      }

      if (newUserId) {
        await setDoc(doc(db, "users", newUserId, "cards", cardId), {
          balance,
          isActive,
          createdAt: serverTimestamp()
        });
      } else {
        await setDoc(doc(db, "unassignedCards", cardId), {
          balance,
          isActive,
          createdAt: serverTimestamp()
        });
      }
    }

    document.getElementById('nfcModal').style.display = 'none';
    form.reset();
    delete form.dataset.mode;
    delete form.dataset.cardId;
    delete form.dataset.userId;
    loadNfcCards();
  } catch (error) {
    console.error("Error updating NFC card:", error);
    alert("Failed to update NFC card. Please try again.");
  }
};

export const initNfcPage = () => {
  nfcTable = initNfcTable();
  setupEventListeners();
  loadNfcCards();
};