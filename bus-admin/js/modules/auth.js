//auth.js
import { auth, signInWithEmailAndPassword, signOut,
   createUserWithEmailAndPassword, sendEmailVerification ,
    db, doc, getDoc, onAuthStateChanged,
     browserSessionPersistence, setPersistence,
     query, getDocs, 
     collection,
     setDoc, where,
     serverTimestamp} from "../core/firebase.js";


export const initAuth = () => {
  

  // Login
  const login=document.getElementById('loginForm');
  if(login){
    login.addEventListener('submit', async (e) => {
    e.preventDefault();
    const email=document.getElementById('email').value;
    const password=document.getElementById('password').value;
    const submitBtn = document.getElementById('submitBtn');
   
    // Enhanced validation
    if (!email) {
      alert('Please enter an email address');
      return;
    }
    
    // Strict email validation
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      alert('Please enter a valid email address (e.g., user@example.com)');
      return;
    }
    
    if (!password || password.length < 6) {
      alert('Password must be at least 6 characters');
      return;
    }

    try {
          submitBtn.disabled = true;
          submitBtn.textContent = 'Logging in...';

          await setPersistence(auth, browserSessionPersistence);          
          const userCredential = await signInWithEmailAndPassword(auth, email, password);
          const user=userCredential.user;

          // Add debug logging
          console.log("Auth successful. User UID:", user.uid);
          console.log("User email:", user.email);

          // Check users collection using UID
          const userDocRef = doc(db, "users", user.uid);
          console.log("Checking Firestore document at:", `users/${user.uid}`);

          // Check users collection for admin role
          const userDoc = await getDoc(userDocRef);
          console.log("Document exists:", userDoc.exists());   
          if (!userDoc.exists()) {
            console.log("Checking agents collection for: ", user.email);
            try{
              //agents query
            const agentsQuery=query(
              collection(db,"agents"),
              where("email","==",user.email)
            );
            //get a snapshot with security rules
          const agentsSnapshot=await getDocs(agentsQuery);
          if(!agentsSnapshot.empty){
            //Found in agents - migrate to users 
            console.log("Found in agents collection, migrating...");
            const agentData=agentsSnapshot.docs[0].data();
            console.log("Migrating agent to users collection: ",agentData);

            await setDoc(doc(db,"users",user.uid),{
              ...agentData,
              migratedFrom:"agents",
              migratedAt:serverTimestamp() 
            });
            console.log("Migrating agent to users collection is Done ");

            //Get the newly created document
            userDoc=await getDoc(doc(db,"users",user.uid));
            console.log("new document in users is handled");
          }}catch(queryError){
            console.error("Error querying agents collection:", queryError);
            // More specific error handling
      if (queryError.code === 'permission-denied') {
        alert("System configuration error. Please contact administrator.");
      } else {
        alert("Could not verify your account permissions");
      }
      await signOut(auth);
      return;
          }
          }

          // Final verification
          if (!userDoc.exists()) {
            console.log("No user document found");
            await signOut(auth);
            alert("Your account is not properly configured");
            return;
          }
          if (userDoc.data().role !== "admin") {
            console.log("Access denied - Not an admin");
            await signOut(auth);
            alert("Only admin users can access this portal");
            return;
          }
          console.log('User logged in:',user.email);
          window.location.href = 'admin-dashboard.html';
          
        } catch (error) {
          console.error('Login Error:', error);
          
          let errorMessage = 'Login Failed. ';
          switch(error.code) {
            case 'auth/user-not-found':
              errorMessage = 'No account found with this email.';
              break;
            case 'auth/wrong-password':
              errorMessage = 'Incorrect password. Please try again.';
              break;
            case 'auth/invalid-email':
              errorMessage = 'Invalid email format.';
              break;
            case 'auth/too-many-requests':
              errorMessage = 'Too many attempts. Account temporarily locked.';
              break;
            default:
              errorMessage += error.message.replace('Firebase: ', '');
          }
          
          alert(errorMessage);
        } finally {
          submitBtn.disabled = false;
          submitBtn.textContent = 'Login';
        }
    });
  }

  // Logout
  const logoutBtn=document.getElementById('logoutBtn');
  if(logoutBtn){
    logoutBtn.addEventListener('click', async () => {
      try {
        await signOut(auth);
        window.location.href = 'index.html';
      } catch (error) {
        console.error('Logout error:', error);
        alert('Logout failed. Please try again.');
      }
    });
  }


  // Auth state listener with role checking
  onAuthStateChanged(auth, async (user) => {
    const isLoginPage = window.location.pathname.includes("index.html");

    if(user){
      try {
        // Check user role in Firestore (agents collection)
        const userDoc = await getDoc(doc(db, "users", user.uid));
        if (!userDoc.exists()) {
          // User doesn't exist in agents collection
          await signOut(auth);
          if (!isLoginPage) window.location.href = "index.html?error=no_profile";
          return;
        }
        const isVerified = userDoc.data()?.emailVerified || user.emailVerified;
    
        if (!isVerified) {
          console.log(`${userDoc.email} is not verifid`);
        }
        
        const userData = userDoc.data();
        
        // Only allow admins to access web portal
        if (userData.role !== "admin") {
          await signOut(auth);
          if (!isLoginPage) window.location.href = "index.html?error=unauthorized";
          return;
        }
        
        // If admin and on login page, redirect to dashboard
        if (isLoginPage) {
          window.location.href = "admin-dashboard.html";
        }
        // Verification status to UI (optional)
      if (!user.emailVerified && document.getElementById('verification-status')) {
        document.getElementById('verification-status').textContent = 'Email not verified';
      }
        
      } catch (error) {
        console.error("Auth check error:", error);
        await signOut(auth);
        if (!isLoginPage) window.location.href = "index.html?error=auth_error";
      }

    }else{
      // Not logged in - redirect to login except for login page
      if (!isLoginPage) {
        window.location.href = "index.html";
      }
    }

  });
};
