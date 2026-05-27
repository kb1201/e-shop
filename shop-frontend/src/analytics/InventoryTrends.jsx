import React, {useEffect, useState} from 'react';
import {
    LineChart, Line, XAxis, YAxis, Tooltip, Legend, ResponsiveContainer, CartesianGrid
} from 'recharts';
import {analyticsApi} from '../api';

export default function WeeklyInventoryTrendChart() {
    const [data, setData] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    useEffect(() => {
        async function fetchData() {
            try {
                const response = await analyticsApi.get('/analytics/inventory/weekly');
                setData(response.data.map(item => ({
                    ...item,
                    weekStart: new Date(item.weekStart).toISOString().split('T')[0]
                })));
                setError(null);
            } catch (err) {
                setError('Failed to load weekly inventory trend.');
            } finally {
                setLoading(false);
            }
        }

        fetchData();
    }, []);

    if (loading) return <div className="text-center py-10 text-gray-500">Loading weekly trend...</div>;
    if (error) return <div className="text-red-600">{error}</div>;

    return (
        <div className="p-6 bg-white rounded-xl shadow max-w-6xl mx-auto">
            <h2 className="text-2xl font-semibold text-gray-800 mb-4">Weekly Inventory Trend</h2>
            <ResponsiveContainer width="100%" height={400}>
                <LineChart data={data}>
                    <CartesianGrid strokeDasharray="3 3"/>
                    <XAxis dataKey="weekStart"/>
                    <YAxis/>
                    <Tooltip/>
                    <Legend/>
                    <Line type="monotone" dataKey="totalAvailable" stroke="#3b82f6" name="Available"/>
                    <Line type="monotone" dataKey="totalReserved" stroke="#f59e0b" name="Reserved"/>
                    <Line type="monotone" dataKey="avgAvailablePerProduct" stroke="#10b981" name="Avg Qty / Product"/>
                    <Line type="monotone" dataKey="outOfStockCount" stroke="#ef4444" name="Out of Stock"/>
                </LineChart>
            </ResponsiveContainer>
        </div>
    );
}
