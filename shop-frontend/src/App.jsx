import React, {useState} from 'react';
import {BrowserRouter as Router, Route, Routes} from 'react-router-dom';
import Navbar from './components/Navbar';
import Login from './auth/Login';
import Register from './auth/Register';
import Home from './Home';
import './index.css';
import ProductList from "./product/ProductList";
import CartPage from "./cart/CartPage";
import OrderHistory from "./order/OrderHistory";
import ShipmentAdmin from "./shipment/Shipment";
import InventoryAdmin from "./inventory/AdminInventoryPage";
import AdminRoute from "./auth/AdminRoute";
import Dashboard from "./analytics/Dashboard";

function App() {
    const [user, setUser] = useState(null);

    const handleLogout = () => {
        setUser(null);
    };

    return (
        <Router>
            <Navbar user={user} onLogout={handleLogout}/>
            <Routes>
                <Route path="/" element={<Home/>}/>
                <Route path="/login" element={<Login setUser={setUser}/>}/>
                <Route path="/register" element={<Register/>}/>
                <Route path="/products" element={<ProductList/>}/>
                <Route path="/orders" element={<OrderHistory/>}/>
                <Route path="/cart" element={<CartPage/>}/>
                <Route path="/shipments" element={
                    <AdminRoute>
                        <ShipmentAdmin/>
                    </AdminRoute>
                }/>
                <Route
                    path="/inventory"
                    element={
                        <AdminRoute>
                            <InventoryAdmin/>
                        </AdminRoute>
                    }
                />
                <Route
                    path="/dashboard"
                    element={
                        <AdminRoute>
                            <Dashboard/>
                        </AdminRoute>
                    }
                />
            </Routes>
        </Router>
    );
}

export default App;
