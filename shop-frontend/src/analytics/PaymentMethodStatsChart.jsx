import React, {useEffect, useState} from 'react';
import {
    BarChart, Bar, XAxis, YAxis, Tooltip, Legend, ResponsiveContainer, CartesianGrid
} from 'recharts';
import {analyticsApi} from "../api";

export default function PaymentMethodChart() {
    const [data, setData] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    useEffect(() => {
        async function fetchData() {
            try {
                const response = await analyticsApi.get("/analytics/order/payment");
                setData(response.data);
                setError(null);
            } catch (err) {
                setError("Failed to load payment method analytics.");
            } finally {
                setLoading(false);
            }
        }

        fetchData();
    }, []);

    if (loading) return <div className="text-center py-10 text-gray-500">Loading payment method analytics...</div>;
    if (error) return <div className="text-red-600">{error}</div>;

    return (
        <div className="p-6 bg-white rounded-xl shadow max-w-6xl mx-auto">
            <h2 className="text-2xl font-semibold text-gray-800 mb-4">Payment Method Share by Orders (%)</h2>

            <ResponsiveContainer width="100%" height={400}>
                <BarChart data={data}>
                    <CartesianGrid strokeDasharray="3 3"/>
                    <XAxis dataKey="paymentMethod"/>
                    <YAxis domain={[0, 100]} tickFormatter={(value) => `${value}%`}/>
                    <Tooltip formatter={(value) => `${value}%`}/>
                    <Bar dataKey="percentageOfOrders" fill="#3b82f6" name="% of Orders"/>
                </BarChart>
            </ResponsiveContainer>
        </div>

    )

}
