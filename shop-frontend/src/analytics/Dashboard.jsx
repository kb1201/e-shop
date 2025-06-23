import WarehousePerformanceTable from "./WarehousePerformance";
import OrderAnalyticsDashboard from "./OrderSummary";
import PaymentMethodChart from "./PaymentMethodStatsChart";
import InventoryHealthChart from "./InventorySummaryCard";
import WeeklyInventoryTrendChart from "./InventoryTrends";
import ShipmentAnalyticsChart from "./ShipmentSummary";
import ShipmentStatusDurationChart from "./ShipmentStatusDuration";
import OrderStatusFlowChart from "./OrderStatusDurationCard";


const Dashboard = () => {

    return (
        <div className="min-h-screen bg-gradient-to-br from-slate-50 via-blue-50 to-indigo-50 p-4 md:p-6 lg:p-8">
            <div className="max-w-7xl mx-auto">
                {/* Header */}
                <div className="mb-8 text-center">
                    <h1 className="text-4xl md:text-5xl font-bold bg-gradient-to-r from-slate-800 via-blue-800 to-indigo-800 bg-clip-text text-transparent mb-3">
                        Analytics Dashboard
                    </h1>
                </div>

                {/* Dashboard Grid - Flexible Layout */}
                <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 auto-rows-min">

                    {/* Main Analytics - Takes prominent space */}
                    <div
                        className="lg:col-span-8 bg-white rounded-2xl shadow-lg border border-slate-200 p-6 hover:shadow-xl transition-all duration-300">
                        <OrderAnalyticsDashboard/>
                    </div>

                    {/* Payment Methods - Compact card */}
                    <div
                        className="lg:col-span-4 bg-white rounded-2xl shadow-lg border border-slate-200 p-6 hover:shadow-xl transition-all duration-300">
                        <PaymentMethodChart/>
                    </div>

                    {/* Order Status Duration - Medium width */}
                    <div
                        className="lg:col-span-5 bg-white rounded-2xl shadow-lg border border-slate-200 p-6 hover:shadow-xl transition-all duration-300">
                        <OrderStatusFlowChart/>
                    </div>

                    {/* Inventory Health - Medium width */}
                    <div
                        className="lg:col-span-7 bg-white rounded-2xl shadow-lg border border-slate-200 p-6 hover:shadow-xl transition-all duration-300">
                        <InventoryHealthChart/>
                    </div>

                    {/* Warehouse Performance - Full width table */}
                    <div
                        className="lg:col-span-12 bg-white rounded-2xl shadow-lg border border-slate-200 p-6 hover:shadow-xl transition-all duration-300">
                        <WarehousePerformanceTable/>
                    </div>

                    {/* Weekly Trends - Takes more space for time series */}
                    <div
                        className="lg:col-span-12 bg-white rounded-2xl shadow-lg border border-slate-200 p-6 hover:shadow-xl transition-all duration-300">
                        <WeeklyInventoryTrendChart/>
                    </div>

                    {/* Shipment Analytics - Compact */}
                    {/*<div*/}
                    {/*    className="lg:col-span-4 bg-white rounded-2xl shadow-lg border border-slate-200 p-6 hover:shadow-xl transition-all duration-300">*/}
                    {/*    <ShipmentStatusDurationChart/>*/}
                    
                    {/*</div>*/}

                    <div
                        className="lg:col-span-12 bg-white rounded-2xl shadow-lg border border-slate-200 p-6 hover:shadow-xl transition-all duration-300">
                        <ShipmentAnalyticsChart/>
                    </div>

                </div>

            </div>
        </div>
    );
};

export default Dashboard;