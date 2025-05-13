// core/api.js
export const getStations = async () => {
    const snapshot = await getDocs(collection(db, 'stations'));
    return snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() }));
  };
  
  export const updateStation = async (id, data) => {
    await updateDoc(doc(db, 'stations', id), data);
  };