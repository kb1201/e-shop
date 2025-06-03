import React, {useEffect, useState} from 'react';
import {BarChart, Bar, XAxis, YAxis, Tooltip, Legend, ResponsiveContainer, CartesianGrid} from 'recharts';
import {analyticsApi} from "../api";

export default function OrderStatusDurationChart() {
    const [data, setData] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    useEffect(() => {
        async function fetchData() {
            try {
                const response = await analyticsApi.get("/order/status");
                setData(response.data);
                setError(null);
            } catch (err) {
                setError("Failed to load status duration analytics.");
            } finally {
                setLoading(false);
            }
        }

        fetchData();
    }, []);

    if (loading) return <div className="text-center py-10 text-gray-500">Loading status duration analytics...</div>;
    if (error) return <div className="text-red-600">{error}</div>;

    return (
        <div className="p-6 bg-white rounded-xl shadow max-w-6xl mx-auto">
            <h2 className="text-2xl font-semibold text-gray-800 mb-4">Avg Hours in Order Status</h2>

            <ResponsiveContainer width="100%" height={400}>
                <BarChart data={data}>
                    <CartesianGrid strokeDasharray="3 3"/>
                    <XAxis dataKey="status"/>
                    <YAxis label={{value: 'Hours', angle: -90, position: 'insideLeft'}}/>
                    <Tooltip/>
                    <Legend/>
                    <Bar dataKey="avgHoursInStatus" fill="#3b82f6" name="Avg Hours"/>
                    <Bar dataKey="medianHoursInStatus" fill="#f59e0b" name="Median Hours"/>
                    <Bar dataKey="maxHoursInStatus" fill="#ef4444" name="Max Hours"/>
                </BarChart>
            </ResponsiveContainer>
        </div>
    );
}
