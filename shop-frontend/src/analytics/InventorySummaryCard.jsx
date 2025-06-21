import React, {useEffect, useState} from 'react';
import {PieChart, Pie, Cell, Tooltip, ResponsiveContainer} from 'recharts';
import {analyticsApi} from "../api";

const COLORS = ['#10b981', '#f59e0b', '#ef4444', '#9ca3af'];

export default function InventoryHealthChart() {
    const [data, setData] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    useEffect(() => {
        async function fetchData() {
            try {
                const response = await analyticsApi.get("/inventory/summary");
                setData(response.data);
                setError(null);
            } catch (err) {
                setError("Failed to load inventory health data.");
            } finally {
                setLoading(false);
            }
        }

        fetchData();
    }, []);

    if (loading) return <div className="text-center py-10 text-gray-500">Loading inventory health...</div>;
    if (error) return <div className="text-red-600">{error}</div>;
    if (!data) return null;

    const pieData = [
        {name: 'In Stock', value: data.inStockProducts},
        {name: 'Low Stock', value: data.lowStockProducts},
        {name: 'Out of Stock', value: data.outOfStockProducts},
    ];

    return (
        <div className="p-6 bg-white rounded-xl shadow max-w-6xl mx-auto">
            <h2 className="text-2xl font-semibold text-gray-800 mb-6">Inventory Health</h2>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-8 items-center">
                <ResponsiveContainer width="100%" height={300}>
                    <PieChart>
                        <Pie
                            data={pieData}
                            dataKey="value"
                            nameKey="name"
                            cx="50%"
                            cy="50%"
                            outerRadius={100}
                            label
                        >
                            {pieData.map((entry, index) => (
                                <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]}/>
                            ))}
                        </Pie>
                        <Tooltip/>
                    </PieChart>
                </ResponsiveContainer>

                <div className="text-gray-700 space-y-2">
                    <div><strong>Total Available Quantity:</strong> {data.totalAvailableQty}</div>
                    <div><strong>Total Reserved Quantity:</strong> {data.totalReservedQty}</div>
                    <div><strong>Average Available per Product:</strong> {data.avgAvailablePerProduct}</div>
                    <div><strong>In-Stock Rate:</strong> {data.inStockRatePct}%</div>
                    <div><strong>Out-of-Stock Rate:</strong> {data.outOfStockRatePct}%</div>
                    <div><strong>Products Needing Reorder:</strong> {data.productsNeedReorder}</div>
                </div>
            </div>
        </div>
    );
}
