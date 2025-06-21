import { useEffect, useState } from "react";
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from "recharts";
import {analyticsApi} from "../api";

export default function OrderStatusFlowChart() {
    const [data, setData] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        async function fetchData() {
            try {
                const response = await analyticsApi.get("/order/status");
                // Transform data: add a combined label field for display
                const formatted = response.data.map((d) => ({
                    ...d,
                    transition: `${d.fromStatus} → ${d.toStatus}`,
                }));
                setData(formatted);
                setError(null);
            } catch (err) {
                setError("Failed to load status duration analytics.");
            } finally {
                setLoading(false);
            }
        }

        fetchData();
    }, []);

    if (loading) return <div className="p-4 text-gray-600">Loading...</div>;
    if (error) return <div className="p-4 text-red-600">{error}</div>;

    return (
        <div className="w-full h-[500px] p-4">
            <h2 className="text-xl font-semibold mb-4">Order Status Transition Durations</h2>
            <ResponsiveContainer width="100%" height="100%">
                <BarChart
                    data={data}
                    margin={{ top: 20, right: 30, left: 20, bottom: 80 }}
                >
                    <CartesianGrid strokeDasharray="3 3" />
                    <XAxis dataKey="transition" angle={-45} textAnchor="end" interval={0} height={80} />
                    <YAxis label={{ value: 'Hours', angle: -90, position: 'insideLeft' }} />
                    <Tooltip />
                    <Legend
                        verticalAlign="top"
                        height={40}

                    />
                    <Bar dataKey="avgHoursInStatus" name="Avg Hours" fill="#8884d8" />
                    <Bar dataKey="medianHoursInStatus" name="Median Hours" fill="#82ca9d" />
                    <Bar dataKey="maxHoursInStatus" name="Max Hours" fill="#ffc658" />
                </BarChart>
            </ResponsiveContainer>
        </div>
    );
}
