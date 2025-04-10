import { auth, signInWithEmailAndPassword, signOut, onAuthStateChanged } from "../core/firebase.js";

export const initAuth = () => {
  // Login
  document.getElementById('loginForm')?.addEventListener('submit', async (e) => {
    e.preventDefault();
    try {
      await signInWithEmailAndPassword(auth, document.getElementById('email').value, document.getElementById('password').value);
      window.location.href = 'admin-dashboard.html';
    } catch (error) {
      alert('Login failed: ' + error.message);
    }
  });

  // Logout
  document.getElementById('logoutBtn')?.addEventListener('click', async () => {
    try {
      await signOut(auth);
      window.location.href = 'index.html';
    } catch (error) {
      console.error('Logout error:', error);
    }
  });

  // Auth State Listener
  onAuthStateChanged(auth, (user) => {
    const isLoginPage = window.location.pathname.endsWith('index.html');
    if (user && isLoginPage) window.location.href = 'admin-dashboard.html';
    if (!user && !isLoginPage) window.location.href = 'index.html';
    if (user) document.getElementById('adminName')?.textContent = user.email;
  });
};