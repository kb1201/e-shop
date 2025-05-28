import React, { useEffect, useState } from 'react';
import axios from 'axios';

const AdminInventoryPage = () => {
    const [inventory, setInventory] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        const fetchInventory = async () => {
            try {
                const response = await axios.get('/inventory', {
                    headers: {
                        Authorization: `Bearer ${localStorage.getItem('token')}`,
                    },
                });
                setInventory(response.data);
            } catch (err) {
                setError('Failed to fetch inventory.');
            } finally {
                setLoading(false);
            }
        };

        fetchInventory();
    }, []);

    return (
        <div className="container mx-auto px-4 py-8">
            <h1 className="text-2xl font-bold mb-4">Inventory Management</h1>

            {loading ? (
                <p>Loading...</p>
            ) : error ? (
                <p className="text-red-600">{error}</p>
            ) : (
                <div className="overflow-x-auto">
                    <table className="min-w-full bg-white border border-gray-300">
                        <thead className="bg-gray-100">
                        <tr>
                            <th className="px-4 py-2 border">ID</th>
                            <th className="px-4 py-2 border">Product ID</th>
                            <th className="px-4 py-2 border">SKU</th>
                            <th className="px-4 py-2 border">Available</th>
                            <th className="px-4 py-2 border">Reserved</th>
                            <th className="px-4 py-2 border">Reorder Threshold</th>
                            <th className="px-4 py-2 border">Status</th>
                            <th className="px-4 py-2 border">Warehouse</th>
                            <th className="px-4 py-2 border">Shelf</th>
                            <th className="px-4 py-2 border">Last Updated</th>
                        </tr>
                        </thead>
                        <tbody>
                        {inventory.map((item) => (
                            <tr key={item.id} className="text-center">
                                <td className="px-4 py-2 border">{item.id}</td>
                                <td className="px-4 py-2 border">{item.productId}</td>
                                <td className="px-4 py-2 border">{item.sku}</td>
                                <td className="px-4 py-2 border">{item.quantityAvailable}</td>
                                <td className="px-4 py-2 border">{item.reservedQuantity}</td>
                                <td className="px-4 py-2 border">{item.reorderThreshold}</td>
                                <td className="px-4 py-2 border">{item.status}</td>
                                <td className="px-4 py-2 border">{item.warehouseLocation}</td>
                                <td className="px-4 py-2 border">{item.shelfLocation}</td>
                                <td className="px-4 py-2 border">{new Date(item.lastUpdated).toLocaleString()}</td>
                            </tr>
                        ))}
                        </tbody>
                    </table>
                </div>
            )}
        </div>
    );
};

export default AdminInventoryPage;
