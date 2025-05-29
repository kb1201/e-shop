// src/auth/AdminRoute.jsx
import React, {useContext} from 'react';
import {Navigate} from 'react-router-dom';
import {AuthContext} from "./AuthContext";

const AdminRoute = ({children}) => {
    const {isAdmin, isAuthenticated} = useContext(AuthContext);

    if (!isAuthenticated() || !isAdmin()) {
        return <Navigate to="/"/>;
    }

    return children;
};

export default AdminRoute;
