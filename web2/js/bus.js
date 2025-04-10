import { db, rtdb, collection, addDoc, ref, set } from "../core/firebase.js";
import { initBusTable } from "../modules/datatable.js";

export const initBusPage = () => {
  const busTable = initBusTable();
  
  document.getElementById('busForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    const busId = document.getElementById('busId').value;
    
    await addDoc(collection(db, "Buses"), {
      busId,
      lastUpdated: serverTimestamp()
    });
    
    await set(ref(rtdb, `buses/${busId}`), {
      currentLocation: null,
      lastUpdated: Date.now()
    });
    
    busTable.ajax.reload();
    document.getElementById('busModal').style.display = 'none';
  });
};