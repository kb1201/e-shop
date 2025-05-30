package hr.fer.dipl.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtils {

    /**
     * Gets the ID of the currently authenticated user
     * 
     * @return the user ID of the currently logged-in user
     */
    public Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new SecurityException("User is not authenticated");
        }
        
        // In a real application, this would extract the user ID from a custom authentication principal
        // For simplicity, we assume the principal contains the user ID as a string
        return Long.parseLong(authentication.getName());
    }
    

}