import React, {useEffect, useState} from 'react';
import {
    BarChart, Bar, XAxis, YAxis, Tooltip, Legend, ResponsiveContainer, CartesianGrid
} from 'recharts';
import {analyticsApi} from '../api';

export default function WarehouseInventoryChart() {
    const [data, setData] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    useEffect(() => {
        async function fetchData() {
            try {
                const response = await analyticsApi.get('/analytics/inventory/warehouse');
                setData(response.data);
                setError(null);
            } catch (err) {
                setError('Failed to load warehouse inventory stats.');
            } finally {
                setLoading(false);
            }
        }

        fetchData();
    }, []);

    if (loading) return <div className="text-center py-10 text-gray-500">Loading warehouse inventory...</div>;
    if (error) return <div className="text-red-600">{error}</div>;

    return (
        <div className="p-6 bg-white rounded-xl shadow max-w-6xl mx-auto">
            <h2 className="text-2xl font-semibold text-gray-800 mb-4">Inventory by Warehouse</h2>
            <ResponsiveContainer width="100%" height={400}>
                <BarChart data={data}>
                    <CartesianGrid strokeDasharray="3 3"/>
                    <XAxis dataKey="warehouseLocation"/>
                    <YAxis yAxisId="left" />
                    <YAxis yAxisId="right" orientation="right" />
                    <Tooltip/>
                    <Legend/>
                    <Bar yAxisId="left" dataKey="totalAvailableQty" fill="#3b82f6" name="Total Available"/>
                    <Bar yAxisId="left" dataKey="totalReservedQty" fill="#f59e0b" name="Reserved"/>
                    <Bar yAxisId="right" dataKey="stockHealthPct" fill="#10b981" name="Stock Health (%)"/>
                </BarChart>
            </ResponsiveContainer>
        </div>
    );
}
