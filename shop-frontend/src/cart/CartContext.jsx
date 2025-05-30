import React, {createContext, useContext, useState, useEffect} from 'react';
import axios from 'axios';
import {orderApi} from "../api";
import {AuthContext} from "../auth/AuthContext";

const CartContext = createContext();

export const useCart = () => {
    return useContext(CartContext);
};

export const CartProvider = ({children}) => {
    const {token, userId, logout, isAdmin} = useContext(AuthContext);
    const [cart, setCart] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);

    // Fetch cart items on component mount
    useEffect(() => {
        setError(null);
        if (!isAdmin()) {
            fetchCart();
        }
    }, []);

    // Get cart items from API
    const fetchCart = async () => {
        setLoading(true);
        try {
            const response = await orderApi.get('/cart');
            setCart(response.data); // Direct array, no wrapper
            setError(null);
        } catch (err) {
            setError(getErrorMessage(err));
            console.error('Error fetching cart:', err);
        } finally {
            setLoading(false);
        }
    };

    // Add item to cart
    const addToCart = async (product) => {
        if (isAdmin()) return;
        setLoading(true);
        try {
            const cartItem = {
                productId: product.id,
                productName: product.name,
                price: product.price,
                quantity: 1
            };

            const response = await orderApi.post('/cart', cartItem);

            // Update cart state by either adding new item or updating existing
            const newItem = response.data;
            setCart(prevCart => {
                const existingItemIndex = prevCart.findIndex(item => item.productId === newItem.productId);
                if (existingItemIndex >= 0) {
                    // Update existing item
                    const updatedCart = [...prevCart];
                    updatedCart[existingItemIndex] = newItem;
                    return updatedCart;
                } else {
                    // Add new item
                    return [...prevCart, newItem];
                }
            });

            setError(null);
            return newItem;
        } catch (err) {
            const errorMessage = getErrorMessage(err);
            setError(errorMessage);
            console.error('Error adding to cart:', err);

            // Show user-friendly messages for specific errors
            if (err.response?.data?.errorCode === 'INSUFFICIENT_INVENTORY') {
                throw new Error('Sorry, there is not enough inventory for this item.');
            }
            throw new Error(errorMessage);
        } finally {
            setLoading(false);
        }
    };

    // Update cart item quantity
    const updateCartItem = async (cartItemId, quantity) => {
        if (isAdmin()) return;
        if (quantity <= 0) {
            return removeFromCart(cartItemId);
        }

        setLoading(true);
        try {
            const itemToUpdate = cart.find(item => item.id === cartItemId);
            if (!itemToUpdate) {
                throw new Error('Cart item not found');
            }

            const updatedItemData = {
                ...itemToUpdate,
                quantity: quantity
            };

            const response = await orderApi.put(`/cart/${cartItemId}`, updatedItemData);
            const updatedItem = response.data;

            // Update the specific item in cart state
            setCart(prevCart =>
                prevCart.map(item =>
                    item.id === cartItemId ? updatedItem : item
                )
            );

            setError(null);
            return updatedItem;
        } catch (err) {
            const errorMessage = getErrorMessage(err);
            setError(errorMessage);
            console.error('Error updating cart item:', err);

            if (err.response?.data?.errorCode === 'INSUFFICIENT_INVENTORY') {
                throw new Error('Sorry, there is not enough inventory for the requested quantity.');
            }
            throw new Error(errorMessage);
        } finally {
            setLoading(false);
        }
    };

    // Remove item from cart
    const removeFromCart = async (cartItemId) => {
        if (isAdmin()) return;
        setLoading(true);
        try {
            await orderApi.delete(`/cart/${cartItemId}`);

            // Remove item from cart state
            setCart(prevCart => prevCart.filter(item => item.id !== cartItemId));
            setError(null);
        } catch (err) {
            const errorMessage = getErrorMessage(err);
            setError(errorMessage);
            console.error('Error removing from cart:', err);
            throw new Error(errorMessage);
        } finally {
            setLoading(false);
        }
    };

    // Clear cart
    const clearCart = async () => {
        if (isAdmin()) return;
        setLoading(true);
        try {
            await orderApi.delete('/cart');
            setCart([]);
            setError(null);
        } catch (err) {
            const errorMessage = getErrorMessage(err);
            setError(errorMessage);
            console.error('Error clearing cart:', err);
            throw new Error(errorMessage);
        } finally {
            setLoading(false);
        }
    };

    // Create order from cart
    const createOrder = async (orderDetails) => {
        if (isAdmin()) return;
        setLoading(true);
        try {
            const orderRequest = {
                ...orderDetails,
                // Don't include items - backend gets them from cart
            };

            const response = await orderApi.post('/orders', orderRequest);

            // After successful order creation, cart is automatically cleared by backend
            setCart([]);
            setError(null);
            return response.data;
        } catch (err) {
            const errorMessage = getErrorMessage(err);
            setError(errorMessage);
            console.error('Error creating order:', err);
            throw new Error(errorMessage);
        } finally {
            setLoading(false);
        }
    };

    // Helper function to extract error messages
    const getErrorMessage = (error) => {
        if (error.response?.data?.message) {
            return error.response.data.message;
        }
        if (error.response?.data?.error) {
            return error.response.data.error;
        }
        if (error.message) {
            return error.message;
        }
        return 'An unexpected error occurred';
    };

    // Calculate cart total
    const getCartTotal = () => {
        return cart.reduce((total, item) => {
            const price = typeof item.price === 'number' ? item.price : parseFloat(item.price) || 0;
            const quantity = typeof item.quantity === 'number' ? item.quantity : parseInt(item.quantity) || 0;
            return total + (price * quantity);
        }, 0);
    };

    // Calculate total items in cart
    const getCartItemCount = () => {
        return cart.reduce((count, item) => {
            const quantity = typeof item.quantity === 'number' ? item.quantity : parseInt(item.quantity) || 0;
            return count + quantity;
        }, 0);
    };

    // Check if cart is empty
    const isCartEmpty = () => {
        return cart.length === 0;
    };

    // Get item by product ID
    const getCartItemByProductId = (productId) => {
        return cart.find(item => item.productId === productId);
    };

    return (
        <CartContext.Provider value={{
            cart,
            loading,
            error,
            addToCart,
            updateCartItem,
            removeFromCart,
            clearCart,
            createOrder,
            getCartTotal,
            getCartItemCount,
            isCartEmpty,
            getCartItemByProductId,
            fetchCart,
            setError // Allow components to clear errors
        }}>
            {children}
        </CartContext.Provider>
    );
};