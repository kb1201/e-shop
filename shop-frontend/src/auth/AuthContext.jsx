import { createContext, useState, useEffect } from 'react';
import { userApi } from '../api';

export const AuthContext = createContext();

export const AuthProvider = ({ children }) => {
    // Token lives in an HttpOnly cookie — JS never touches it.
    // Only non-sensitive identifiers are kept in localStorage.
    const [userId, setUserId] = useState(localStorage.getItem('userId'));
    const [userRole, setUserRole] = useState(localStorage.getItem('userRole'));

    const login = ({ userId: newUserId, role: newRole }) => {
        localStorage.setItem('userId', newUserId);
        localStorage.setItem('userRole', newRole);
        setUserId(newUserId);
        setUserRole(newRole);
    };

    const logout = async () => {
        try {
            await userApi.post('/users/logout');
        } catch (_) {
            // best-effort — clear client state regardless
        }
        localStorage.removeItem('userId');
        localStorage.removeItem('userRole');
        setUserId(null);
        setUserRole(null);
    };

    // Role checking utilities
    const hasRole = (requiredRole) => {
        return userRole === requiredRole;
    };

    const hasAnyRole = (roles) => {
        return roles.includes(userRole);
    };

    const isAdmin = () => {
        return userRole === 'ROLE_ADMIN' || userRole === 'role_admin' || userRole === 'ADMIN';
    };

    const isModerator = () => {
        return userRole === 'moderator' || userRole === 'admin';
    };

    const isAuthenticated = () => {
        return !!userId;
    };

    useEffect(() => {
        const storedUserId = localStorage.getItem('userId');
        const storedUserRole = localStorage.getItem('userRole');
        if (storedUserId) setUserId(storedUserId);
        if (storedUserRole) setUserRole(storedUserRole);
    }, []);

    const contextValue = {
        userId,
        userRole,
        login,
        logout,
        hasRole,
        hasAnyRole,
        isAdmin,
        isModerator,
        isAuthenticated
    };

    return (
        <AuthContext.Provider value={contextValue}>
            {children}
        </AuthContext.Provider>
    );
};
