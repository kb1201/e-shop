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

function App() {
    const [user, setUser] = useState(null);

    const handleLogout = () => {
        localStorage.removeItem('token');
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
                <Route path="/shipments" element={<ShipmentAdmin/>}/>
            </Routes>
        </Router>
    );
}

export default App;
