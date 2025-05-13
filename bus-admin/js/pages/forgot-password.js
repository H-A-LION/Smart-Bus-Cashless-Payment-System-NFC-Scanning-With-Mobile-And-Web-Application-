//forgot-password.js
import { 
    auth, sendPasswordResetEmail, confirmPasswordReset, createUserWithEmailAndPassword,
    signInWithEmailAndPassword, db, doc, setDoc, getDoc , query, collection, where, getDocs
  } from "../core/firebase.js";
  
  


  export const initForgotPasswordPage = () => {
    const resetForm = document.getElementById('resetPasswordForm');
    const feedback = document.getElementById('password-feedback');
  
    // Password strength checker
    const checkPasswordStrength = (password) => {
      let strength = 0;
      if (password.length >= 8) {
        if (password.length >= 12) strength += 1;
        if (/[A-Z]/.test(password)) strength += 1;
        if (/[a-z]/.test(password)) strength += 1;
        if (/[0-9]/.test(password)) strength += 1;
        if (/[^A-Za-z0-9]/.test(password)) strength += 1;
      } else if (password.length > 0) {
        strength += 1;
      }
      if (password.match(/password|1234|qwerty/i)) strength = Math.max(0, strength - 2);
      return Math.min(4, strength);
    };
  
    const updateStrengthMeter = (strength) => {
      const bars = Array.from(document.querySelectorAll('.strength-bar'));
      bars.forEach((bar, index) => {
        bar.style.backgroundColor = index < strength ? 
          ['#d32f2f', '#ffa000', '#388e3c', '#00796b'][strength - 1] : '#e0e0e0';
      });
  
      if (feedback) {
        const [text, className] = 
          strength === 0 ? ['',''] :
          strength <= 1 ? ['Weak password', 'weak'] :
          strength <= 2 ? ['Moderate password', 'moderate'] :
          strength <= 3 ? ['Strong password', 'strong'] :
          ['Very strong password', 'very-strong'];
        feedback.textContent = text;
        feedback.className = `password-feedback ${className}`;
      }
    };
  
    // Password strength tracking
    const newPasswordInput = document.getElementById('newPassword');
    newPasswordInput.addEventListener('input', (e) => {
      const strength = checkPasswordStrength(e.target.value);
      updateStrengthMeter(strength);
    });
  
    // Password match checking
    const confirmPasswordInput = document.getElementById('confirmNewPassword');
    confirmPasswordInput.addEventListener('input', (e) => {
      if (newPasswordInput.value !== e.target.value) {
        feedback.textContent = 'Passwords do not match';
        feedback.className = 'password-feedback weak';
      } else {
        feedback.textContent = 'Passwords match';
        feedback.className = 'password-feedback strong';
      }
    });
  
    resetForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const email = document.getElementById('resetEmail').value;
      const newPassword = newPasswordInput.value;
  
      try {
        // Check if email exists in agents or users
        
        const agentsQuery = query(
         collection(db, "agents"),
          where("email", "==", email)
        );
        console.log("Check if email exists in agents");
        const usersQuery = query(
          collection(db, "users"),
          where("email", "==", email)
        );
        console.log("Check if email exists in users");
    
        const [agentsSnapshot, usersSnapshot] = await Promise.all([
          getDocs(agentsQuery),
          getDocs(usersQuery)
        ]);
        console.log("getDocs from users and agents that match query");

        if (agentsSnapshot.empty && usersSnapshot.empty) {
          console.log("No account found with this email");
          return;
        }
        
        //Send password reset 
        await sendPasswordResetEmail(auth, email);
        alert(`Password reset email sent to ${email}. Please check your inbox.`);
        window.location.href = "index.html";
      } catch (error) {
        if (error.code === 'auth/user-not-found') {
          alert("No account found with this email");
        } else{
        console.error("Password reset error:", error);
        alert(`Error: ${error.message}`);
        }
      }
    });
  };
  
  // Initialize if this is the forgot password page
  if (document.getElementById('resetPasswordForm')) {
    initForgotPasswordPage();
  }
