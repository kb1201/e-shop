import { createContext, useState, useEffect } from 'react';

export const AuthContext = createContext();

export const AuthProvider = ({ children }) => {
    const [token, setToken] = useState(localStorage.getItem('token'));
    const [userId, setUserId] = useState(localStorage.getItem('userId'));
    const [userRole, setUserRole] = useState(localStorage.getItem('userRole'));

    const login = ({ token: newToken, userId: newUserId, role: newRole }) => {
        localStorage.setItem('token', newToken);
        localStorage.setItem('userId', newUserId);
        localStorage.setItem('userRole', newRole);
        setToken(newToken);
        setUserId(newUserId);
        setUserRole(newRole);
    };

    const logout = () => {
        localStorage.removeItem('token');
        localStorage.removeItem('userId');
        localStorage.removeItem('userRole');
        setToken(null);
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
        return userRole === 'ROLE_ADMIN' || userRole === 'role_admin' || userRole === "ADMIN";
    };

    const isModerator = () => {
        return userRole === 'moderator' || userRole === 'admin';
    };

    const isAuthenticated = () => {
        return !!token && !!userId;
    };

    useEffect(() => {
        const storedToken = localStorage.getItem('token');
        const storedUserId = localStorage.getItem('userId');
        const storedUserRole = localStorage.getItem('userRole');

        if (storedToken) setToken(storedToken);
        if (storedUserId) setUserId(storedUserId);
        if (storedUserRole) setUserRole(storedUserRole);
    }, []);

    const contextValue = {
        token,
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