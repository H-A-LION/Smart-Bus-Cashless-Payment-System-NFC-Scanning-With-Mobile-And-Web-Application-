package com.example.smartbuspayment;
import  android.text.TextUtils;

public class PasswordValidator {
    public enum StrengthLevel {
        WEAK, MEDIUM, STRONG
    }

    public static class PasswordStrength {
        public StrengthLevel strengthLevel = StrengthLevel.WEAK;
        public String errorMessage = "";
    }

    public static PasswordStrength checkPasswordStrength(String password) {
        PasswordStrength result = new PasswordStrength();

        if (TextUtils.isEmpty(password)) {
            result.errorMessage = "Password is required";
            return result;
        }

        if (password.length() < 8) {
            result.errorMessage = "Password must be at least 8 characters";
            return result;
        }

        int strengthScore = 0;

        if (password.matches(".*[A-Z].*")) strengthScore++;
        if (password.matches(".*[a-z].*")) strengthScore++;
        if (password.matches(".*\\d.*")) strengthScore++;
        if (password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) strengthScore++;

        if (password.length() >= 12 && strengthScore == 4) {
            result.strengthLevel = StrengthLevel.STRONG;
        } else if (password.length() >= 10 && strengthScore >= 3) {
            result.strengthLevel = StrengthLevel.MEDIUM;
        } else {
            result.strengthLevel = StrengthLevel.WEAK;
            result.errorMessage = getPasswordRequirements(strengthScore);
        }

        return result;
    }

    private static String getPasswordRequirements(int strengthScore) {
        StringBuilder requirements = new StringBuilder("Password should contain:");
        if ((strengthScore & 1) == 0)
            requirements.append("\n- Uppercase letter");
        if ((strengthScore & 2) == 0)
            requirements.append("\n- Lowercase letter");
        if ((strengthScore & 4) == 0)
            requirements.append("\n- Number");
        if ((strengthScore & 8) == 0)
            requirements.append("\n- Special character");
        requirements.append("\n- Minimum 8 characters (12+ for strong)");
        return requirements.toString();
    }
}
