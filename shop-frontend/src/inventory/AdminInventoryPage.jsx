import React, {useState, useEffect} from 'react';
import {inventoryApi} from "../api";


const InventoryAdmin = () => {
    const [inventory, setInventory] = useState([]);
    const [loading, setLoading] = useState(false);
    // const [searchTerm, setSearchTerm] = useState('');
    const [showCreateModal, setShowCreateModal] = useState(false);
    const [showEditModal, setShowEditModal] = useState(false);
    const [showRestockModal, setShowRestockModal] = useState(false);
    const [selectedItem, setSelectedItem] = useState(null);
    const [notification, setNotification] = useState(null);
    const [page, setPage] = useState(0);
    const [size, setSize] = useState(10);
    const [totalPages, setTotalPages] = useState(0);
    const [statusFilter, setStatusFilter] = useState('');

    // Form states
    const [createForm, setCreateForm] = useState({
        productName: '',
        quantity: '',
        minStockLevel: '',
        location: '',
        supplier: ''
    });

    const [editForm, setEditForm] = useState({
        id: '',
        productName: '',
        quantityAvailable: '',
        minStockLevel: '',
        location: '',
        supplier: ''
    });

    const [restockForm, setRestockForm] = useState({
        quantity: ''
    });


    useEffect(() => {
        fetchInventory();
    }, [page, size, statusFilter]);

    const showNotification = (message, type = 'success') => {
        setNotification({message, type});
        setTimeout(() => setNotification(null), 3000);
    };

    // API calls (replace with actual implementation)
    const fetchInventory = async () => {
        setLoading(true);
        try {
            const response = await inventoryApi.get('/inventory', {
                params: {
                    page,
                    size,
                    status: statusFilter || undefined,
                }
            });
            const data = response.data;

            setInventory(data.content);
            setTotalPages(data.totalPages);
            showNotification('Inventory loaded successfully');
        } catch (error) {
            showNotification('Failed to load inventory', 'error');
        } finally {
            setLoading(false);
        }
    };

    const createInventory = async (formData) => {
        try {
            //todo as bellow
            const response = await inventoryApi.post("/invenotry", {
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(formData)
            })

            const newItem = await response.data();

            setInventory([...inventory, newItem]);
            showNotification('Inventory item created successfully');
            setShowCreateModal(false);
            setCreateForm({productName: '', quantity: '', minStockLevel: '', location: '', supplier: ''});
        } catch (error) {
            showNotification('Failed to create inventory item', 'error');
        }
    };

    const updateInventory = async (id, formData) => {
        try {
            console.log("========");
            console.log(JSON.stringify(formData));

            const response = await inventoryApi.put(
                `inventory/${id}`,
                formData,
                {
                    headers: {'Content-Type': 'application/json'}
                }
            );

            const updatedItem = response.data;

            const updatedInventory = inventory.map(item =>
                item.id === id
                    ? {
                        ...item,
                        ...formData,
                        quantityAvailable: parseInt(updatedItem.quantityAvailable),
                        reorderThreshold: parseInt(updatedItem.reorderThreshold),
                        warehouseLocation: updatedItem.warehouseLocation,
                        shelfLocation: updatedItem.shelfLocation
                    }
                    : item
            );

            setInventory(updatedInventory);
            showNotification('Inventory item updated successfully');
            setShowEditModal(false);
        } catch (error) {
            showNotification('Failed to update inventory item', 'error');
        }
    };


    const restockInventory = async (productId, quantity) => {
        try {
            const res = await inventoryApi.put(`inventory/${productId}/restock`,
                {quantity: parseInt(quantity)},
                {headers: {'Content-Type': 'application/json'}});

            const updatedInventory = inventory.map(item =>
                item.id === res.data.id ? {
                    ...item,
                    quantityAvailable: res.data.quantityAvailable,
                    status: res.data.status
                } : item
            );
            setInventory(updatedInventory);
            showNotification(`Successfully restocked ${quantity} units`);
            setShowRestockModal(false);
            setRestockForm({quantity: ''});
        } catch (error) {
            showNotification('Failed to restock inventory', 'error');
        }
    };

    const statusColor = (status) => {
        switch (status) {
            case 'IN_STOCK':
                return 'bg-green-100 text-green-800';
            case 'LOW_STOCK':
                return 'bg-yellow-100 text-yellow-800';
            case 'OUT_OF_STOCK':
                return 'bg-red-100 text-red-800';
            default:
                return '';
        }
    };


    const openEditModal = (item) => {
        setSelectedItem(item);
        console.log(item)
        setEditForm({
            id: item.id,
            productName: item.productName,
            quantityAvailable: item.quantityAvailable.toString(),
            reorderThreshold: item.reorderThreshold.toString(),
            warehouseLocation: item.warehouseLocation,
            shelfLocation: item.shelfLocation
        });
        setShowEditModal(true);
    };

    const openRestockModal = (item) => {
        setSelectedItem(item);
        setShowRestockModal(true);
    };


    return (
        <div className="min-h-screen bg-gray-50 p-6">
            <div className="max-w-7xl mx-auto">
                {/* Header */}
                <div className="mb-8">
                    <h1 className="text-3xl font-bold text-gray-900 mb-2">Inventory Management</h1>
                    <p className="text-gray-600">Manage your inventory items, stock levels, and restocking</p>
                </div>

                {/* Notification */}
                {notification && (
                    <div className={`fixed top-4 right-4 z-50 p-4 rounded-lg shadow-lg ${
                        notification.type === 'error' ? 'bg-red-500' : 'bg-green-500'
                    } text-white flex items-center gap-2`}>
                        {notification.type === 'error' ? <span role="img" aria-label="plus">⚠️</span>
                            : <span role="img" aria-label="plus">✅</span>}
                        {notification.message}
                    </div>
                )}


                {/* Controls */}
                <div className="flex flex-col sm:flex-row gap-4 mb-6">
                    {/*<div className="relative flex-1">*/}
                    {/*    <span role="img" aria-label="plus">🔍</span>*/}
                    {/*    <input*/}
                    {/*        type="text"*/}
                    {/*        placeholder="Search inventory..."*/}
                    {/*        className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"*/}
                    {/*        value={searchTerm}*/}
                    {/*        onChange={(e) => setSearchTerm(e.target.value)}*/}
                    {/*    />*/}
                    {/*</div>*/}
                    {/*<button*/}
                    {/*    onClick={() => setShowCreateModal(true)}*/}
                    {/*    className="bg-blue-500 hover:bg-blue-600 text-white px-6 py-2 rounded-lg flex items-center gap-2 transition-colors"*/}
                    {/*>*/}
                    {/*    <span role="img" aria-label="plus">➕</span>*/}
                    {/*    Add New Item*/}
                    {/*</button>*/}
                </div>


                {/* Inventory Table */}
                <div className="bg-white rounded-lg shadow-sm border overflow-hidden">
                    <div className="overflow-x-auto">
                        <table className="min-w-full table-fixed">
                            <thead className="bg-gray-50">
                            <tr>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Id</th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Product
                                    Id
                                </th>

                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Product</th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Stock</th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Reorder
                                    Treshold
                                </th>

                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Warehouse</th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Shelf</th>
                                <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">
                                    <div className="mb-4">
                                        <label htmlFor="status" className="mr-2 font-medium">Filter by Status:</label>
                                        <select
                                            id="status"
                                            value={statusFilter}
                                            onChange={(e) => setStatusFilter(e.target.value)}
                                            className="border rounded p-1"
                                        >
                                            <option value="">All</option>
                                            <option value="IN_STOCK">In Stock</option>
                                            <option value="LOW_STOCK">Low Stock</option>
                                            <option value="OUT_OF_STOCK">Out of Stock</option>
                                        </select>
                                    </div>
                                </th>
                                <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">Actions</th>
                            </tr>
                            </thead>
                            <tbody className="bg-white divide-y divide-gray-200">
                            {inventory.map((item) => (
                                <tr key={item.id} className="hover:bg-gray-50">
                                    <td className="px-6 py-4 whitespace-nowrap">
                                        <span className="text-gray-900 font-semibold">{item.id}</span>
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap">
                                        <span className="text-gray-900 font-semibold">{item.productId}</span>
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap max-w-xs">
                                        <div
                                            className="font-medium text-gray-900 truncate"
                                            title={item.productName}
                                        >
                                            {item.productName}
                                        </div>
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap">
                                        <span className="text-gray-900 font-semibold">{item.quantityAvailable}</span>
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap">
                                        <span className="text-gray-900 font-semibold">{item.reorderThreshold}</span>
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap text-gray-500">
                                        {item.warehouseLocation}
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap text-gray-500">
                                        {item.shelfLocation}
                                    </td>
                                    <td>
                                        <span className={`badge ${statusColor(item.status)}`}>{item.status}</span>
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                                        <div className="flex justify-end gap-2">
                                            <button
                                                onClick={() => openRestockModal(item)}
                                                className="text-blue-600 hover:text-blue-900 p-1 rounded"
                                                title="Restock"
                                            >
                                                <span role="img" aria-label="plus">📦</span>
                                            </button>
                                            <button
                                                onClick={() => openEditModal(item)}
                                                className="text-indigo-600 hover:text-indigo-900 p-1 rounded"
                                                title="Edit"
                                            >
                                                <span role="img" aria-label="plus">✏️</span>
                                            </button>
                                        </div>
                                    </td>
                                </tr>
                            ))}
                            </tbody>
                        </table>
                    </div>
                    <div className="flex justify-end items-center gap-4 p-4">
                        <button
                            onClick={() => setPage(prev => Math.max(prev - 1, 0))}
                            disabled={page === 0}
                            className="px-3 py-1 bg-gray-200 rounded disabled:opacity-50"
                        >
                            Previous
                        </button>

                        <span>Page {page + 1} of {totalPages}</span>

                        <button
                            onClick={() => setPage(prev => Math.min(prev + 1, totalPages - 1))}
                            disabled={page >= totalPages - 1}
                            className="px-3 py-1 bg-gray-200 rounded disabled:opacity-50"
                        >
                            Next
                        </button>
                    </div>

                </div>

                {/* Create Modal */}
                {showCreateModal && (
                    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
                        <div className="bg-white rounded-lg p-6 w-full max-w-md">
                            <h2 className="text-xl font-bold mb-4">Create New Inventory Item</h2>
                            <div>
                                <div className="space-y-4">
                                    <div>
                                        <label className="block text-sm font-medium text-gray-700 mb-1">Product
                                            Name</label>
                                        <input
                                            type="text"
                                            required
                                            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                                            value={createForm.productName}
                                            onChange={(e) => setCreateForm({
                                                ...createForm,
                                                productName: e.target.value
                                            })}
                                        />
                                    </div>
                                    <div>
                                        <label className="block text-sm font-medium text-gray-700 mb-1">Initial
                                            Quantity</label>
                                        <input
                                            type="number"
                                            required
                                            min="0"
                                            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                                            value={createForm.quantity}
                                            onChange={(e) => setCreateForm({...createForm, quantity: e.target.value})}
                                        />
                                    </div>
                                    <div>
                                        <label className="block text-sm font-medium text-gray-700 mb-1">Minimum Stock
                                            Level</label>
                                        <input
                                            type="number"
                                            required
                                            min="0"
                                            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                                            value={createForm.minStockLevel}
                                            onChange={(e) => setCreateForm({
                                                ...createForm,
                                                minStockLevel: e.target.value
                                            })}
                                        />
                                    </div>
                                    <div>
                                        <label className="block text-sm font-medium text-gray-700 mb-1">Location</label>
                                        <input
                                            type="text"
                                            required
                                            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                                            value={createForm.location}
                                            onChange={(e) => setCreateForm({...createForm, location: e.target.value})}
                                        />
                                    </div>
                                    <div>
                                        <label className="block text-sm font-medium text-gray-700 mb-1">Supplier</label>
                                        <input
                                            type="text"
                                            required
                                            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                                            value={createForm.supplier}
                                            onChange={(e) => setCreateForm({...createForm, supplier: e.target.value})}
                                        />
                                    </div>
                                </div>
                                <div className="flex gap-3 mt-6">
                                    <button
                                        type="button"
                                        onClick={() => setShowCreateModal(false)}
                                        className="flex-1 px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50"
                                    >
                                        Cancel
                                    </button>
                                    <button
                                        onClick={() => createInventory(createForm)}
                                        className="flex-1 bg-blue-500 hover:bg-blue-600 text-white px-4 py-2 rounded-lg"
                                    >
                                        Create
                                    </button>
                                </div>
                            </div>
                        </div>
                    </div>
                )}

                {/* Edit Modal */}
                {showEditModal && (
                    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
                        <div className="bg-white rounded-lg p-6 w-full max-w-md">
                            <h2 className="text-xl font-bold mb-4">Edit Inventory Item</h2>
                            <div>
                                <div className="space-y-4">
                                    <div>
                                        <label className="block text-sm font-medium text-gray-700 mb-1">Product
                                            Name</label>
                                        <input
                                            type="text"
                                            required
                                            disabled
                                            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                                            value={editForm.productName}
                                            // onChange={(e) => setEditForm({...editForm, productName: e.target.value})}
                                        />
                                    </div>
                                    <div>
                                        <label className="block text-sm font-medium text-gray-700 mb-1">Quantity</label>
                                        <input
                                            type="number"
                                            required
                                            min="0"
                                            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                                            value={editForm.quantityAvailable}
                                            onChange={(e) => setEditForm({
                                                ...editForm,
                                                quantityAvailable: parseInt(e.target.value) || null
                                            })}
                                        />
                                    </div>
                                    <div>
                                        <label className="block text-sm font-medium text-gray-700 mb-1">Reorder Treshold
                                            Level</label>
                                        <input
                                            type="number"
                                            required
                                            min="0"
                                            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                                            value={editForm.reorderThreshold}
                                            onChange={(e) => setEditForm({
                                                ...editForm,
                                                reorderThreshold: parseInt(e.target.value) || null
                                            })}
                                        />
                                    </div>
                                    <div>
                                        <label
                                            className="block text-sm font-medium text-gray-700 mb-1">Warehouse</label>
                                        <input
                                            type="text"
                                            required
                                            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                                            value={editForm.warehouseLocation}
                                            onChange={(e) => setEditForm({
                                                ...editForm,
                                                warehouseLocation: e.target.value
                                            })}
                                        />
                                    </div>
                                    <div>
                                        <label className="block text-sm font-medium text-gray-700 mb-1">Shelf</label>
                                        <input
                                            type="text"
                                            required
                                            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                                            value={editForm.shelfLocation}
                                            onChange={(e) => setEditForm({...editForm, shelfLocation: e.target.value})}
                                        />
                                    </div>
                                </div>
                                <div className="flex gap-3 mt-6">
                                    <button
                                        type="button"
                                        onClick={() => setShowEditModal(false)}
                                        className="flex-1 px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50"
                                    >
                                        Cancel
                                    </button>
                                    <button
                                        onClick={() => updateInventory(editForm.id, editForm)}
                                        className="flex-1 bg-blue-500 hover:bg-blue-600 text-white px-4 py-2 rounded-lg"
                                    >
                                        Update
                                    </button>
                                </div>
                            </div>
                        </div>
                    </div>
                )}

                {/* Restock Modal */}
                {showRestockModal && selectedItem && (
                    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
                        <div className="bg-white rounded-lg p-6 w-full max-w-md">
                            <h2 className="text-xl font-bold mb-4">Restock Inventory</h2>
                            <div className="mb-4 p-3 bg-gray-50 rounded-lg">
                                <p className="font-medium">{selectedItem.productName}</p>
                                <p className="text-sm text-gray-600">Current Stock: {selectedItem.quantity}</p>
                            </div>
                            <form onSubmit={(e) => {
                                e.preventDefault();
                                restockInventory(selectedItem.productId, restockForm.quantity);
                            }}>
                                <div className="mb-4">
                                    <label className="block text-sm font-medium text-gray-700 mb-1">Quantity to
                                        Add</label>
                                    <input
                                        type="number"
                                        required
                                        min="1"
                                        className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                                        value={restockForm.quantity}
                                        onChange={(e) => setRestockForm({quantity: e.target.value})}
                                        placeholder="Enter quantity to add"
                                    />
                                </div>
                                <div className="flex gap-3">
                                    <button
                                        type="button"
                                        onClick={() => setShowRestockModal(false)}
                                        className="flex-1 px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50"
                                    >
                                        Cancel
                                    </button>
                                    <button
                                        type="submit"
                                        className="flex-1 bg-green-500 hover:bg-green-600 text-white px-4 py-2 rounded-lg"
                                    >
                                        Restock
                                    </button>
                                </div>
                            </form>
                        </div>
                    </div>
                )}
            </div>
        </div>
    );
};

export default InventoryAdmin;