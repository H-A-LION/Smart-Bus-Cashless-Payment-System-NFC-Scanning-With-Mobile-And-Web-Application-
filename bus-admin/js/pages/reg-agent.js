// agent.js
import { db, auth, doc, setDoc, getDoc, createUserWithEmailAndPassword, sendEmailVerification } from "../core/firebase.js";

export const initRegAgentPage = () => {
  const userForm = document.getElementById('agentForm');
  const passwordInput = document.getElementById('password');
  const confirmPasswordInput = document.getElementById('confirmPassword');
  
  const Feedback = document.getElementById('confirm-feedback');
  // const strengthBars = document.querySelectorAll('.strength-bar');
  
  if (agentForm) {
    // Password strength checker
    const checkPasswordStrength = (password) => {
      let strength = 0;
      
      // Length check
      if (password.length >= 8) {//minimum length
      if (password.length >= 12) strength += 1;//best length
      if (/[A-Z]/.test(password)) strength += 1; // Uppercase
      if (/[a-z]/.test(password)) strength += 1; // Lowercase
      if (/[0-9]/.test(password)) strength += 1; // Numbers
      if (/[^A-Za-z0-9]/.test(password)) strength += 1; // Special chars
      }else if(password.length>0){
          strength+=1;
      }
      
      
      // Deduct for common patterns
      if (password.match(/password|1234|qwerty/i)) strength = Math.max(0, strength - 2);
      
      return Math.min(4, strength); // Cap at 4 for our meter
    };
    // const updateStrengthMeter = (strength) => {
    //   strengthBars.forEach((bar, index) => {
    //     if (index < strength) {
    //       // Color based on strength
    //       if (strength <= 1) {
    //         bar.style.backgroundColor = '#d32f2f'; // Weak (red)
    //         passwordFeedback.textContent = 'Weak password';
    //         passwordFeedback.className = 'password-feedback weak';
    //       } else if (strength <= 2) {
    //         bar.style.backgroundColor = '#ffa000'; // Moderate (orange)
    //         passwordFeedback.textContent = 'Moderate password';
    //         passwordFeedback.className = 'password-feedback moderate';
    //       } else if (strength <= 3) {
    //         bar.style.backgroundColor = '#388e3c'; // Strong (green)
    //         passwordFeedback.textContent = 'Strong password';
    //         passwordFeedback.className = 'password-feedback strong';
    //       } else {
    //         bar.style.backgroundColor = '#00796b'; // Very strong (teal)
    //         passwordFeedback.textContent = 'Very strong password';
    //         passwordFeedback.className = 'password-feedback very-strong';
    //       }
    //     } else {
    //       bar.style.backgroundColor = '#e0e0e0'; // Default (gray)
    //     }
    //   });
    // };
    
    const updateStrengthMeter = (strength) => {
      const bars = Array.from(document.querySelectorAll('.strength-bar'));
      
      bars.forEach((bar, index) => {
        bar.style.backgroundColor = index < strength ? 
          ['#d32f2f', '#ffa000', '#388e3c', '#00796b'][strength - 1] : '#e0e0e0';
        
      });

      // Update feedback text and class
      if (Feedback) {
        const [text, className] = 
          strength ===0 ? ['','']:
          strength <= 1 ? ['Weak password', 'weak'] :
          strength <= 2 ? ['Moderate password', 'moderate'] :
          strength <= 3 ? ['Strong password', 'strong'] :
          ['Very strong password', 'very-strong'];

      Feedback.textContent = text;
      Feedback.className = `password-feedback ${className}`;
      }

    };
    
    // Event listeners for password strength feedback
    passwordInput.addEventListener('input', (e) => {
      const password = e.target.value;
      const strength = checkPasswordStrength(password);
      updateStrengthMeter(strength);
      
    });
    confirmPasswordInput.addEventListener('input', (e) => {
      if (passwordInput.value !== e.target.value) {
        Feedback.textContent = 'Passwords do not match';
        Feedback.className = 'password-feedback weak';
      } else {
        Feedback.textContent = 'Passwords match';
        Feedback.className = 'password-feedback strong';
      }
    });

    userForm.addEventListener('submit', async (e) => {
      e.preventDefault();

      //store current user
      const currentUser=auth.currentUser;

      const formData = {
        email: document.getElementById('agentEmail').value,
        password: document.getElementById('password').value,
        confirmPassword: document.getElementById('confirmPassword').value,
        name: document.getElementById('agentName').value,
        phone: document.getElementById('agentPhone').value,
        balance: parseFloat(document.getElementById('agentBalance').value),
        role: document.querySelector('input[name="role"]:checked').value,
        status: document.querySelector('input[name="status":checked]').value
      };

      

      // Validation
      if (formData.password !== formData.confirmPassword) {
        confirmFeedback.textContent = "Passwords don't match!";
        confirmFeedback.className = 'password-feedback weak';
        return;
      }
      const strength = checkPasswordStrength(formData.password);
      if (strength < 2) { // Require at least moderate strength
        alert("Please choose a stronger password. Your password should be at least 8 characters long and include a mix of letters, numbers, and symbols.");
        return;
      }

      try {
        // 1. Create auth account
        const userCredential = await createUserWithEmailAndPassword(auth, formData.email, formData.password);
        const userId = userCredential.user.uid;

        // 2. Create Firestore document
        await setDoc(doc(db, "users", userId), {
          name: formData.name,
          email: formData.email,
          phone: formData.phone,
          balance: formData.balance,
          role: formData.role, // 'passenger' or 'Admin'
          status:formData.status, // Active or inActive
          createdAt: new Date(),
          emailVarified:false, //Track Varification status
          phoneVarified:false
        });

        // 3. Show success message with verification option
        const shouldVerify = confirm(`${formData.role} registered successfully!\n\nDo you want to verify their email now?`);

        if (shouldVerify) {
          await sendEmailVerification(userCredential.user);
          console.log(`Verification email sent to ${formData.email}`);
        }
        

        // 4. Restore admin session
        if (currentUser) {
          await auth.updateCurrentUser(currentUser);
        }

        alert(`${role} registered successfully!`);
        userForm.reset();
        
      } catch (error) {
        console.error("Registration error:", error);
        alert(`Error: ${error.message}`);
        
        // Attempt to restore original user on error
        if (currentUser) {
          await auth.updateCurrentUser(currentUser).catch(e => {
            console.error("Failed to restore user session:", e);
          });
        }
      }
    });
  }
};