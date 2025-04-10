import { db, collection, addDoc, getDocs, doc, updateDoc } from "../core/firebase.js";

import { initNfcTable } from "../modules/datatable.js";

let nfcTable;

const loadNfcCards = async () => {
  const querySnapshot = await getDocs(collection(db, "cards"));
  const cards = querySnapshot.docs.map(doc => ({ id: doc.id, ...doc.data() }));
  nfcTable.clear().rows.add(cards).draw();
};

const addNfcCard = async (e) => {
  e.preventDefault();
  try {
    await addDoc(collection(db, "cards"), {
      cardId: document.getElementById('cardId').value,
      userId: document.getElementById('userId').value || null,
      balance: 0,
      isActive: false,
      createdAt: serverTimestamp()
    });
    document.getElementById('nfcModal').style.display = 'none';
    loadNfcCards();
  } catch (error) {
    console.error("Error adding NFC card:", error);
  }
};

export const initNfcPage = () => {
  nfcTable = initNfcTable();
  
  // Event Listeners
  document.getElementById('addNfcBtn').addEventListener('click', () => {
    document.getElementById('nfcModal').style.display = 'block';
  });
  
  document.getElementById('nfcForm').addEventListener('submit', addNfcCard);
  
  loadNfcCards();
};