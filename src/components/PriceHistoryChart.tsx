import { useState } from 'react';
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from 'recharts';
import { ArrowDownRight, Download } from 'lucide-react';
import { PriceHistoryEntry, Retailer } from '../types';

interface PriceHistoryChartProps {
  retailers: Retailer[];
  productName: string;
}

type TimeRange = '7days' | '1month' | '3months' | 'all';

const PriceHistoryChart = ({ retailers, productName }: PriceHistoryChartProps) => {
  const [timeRange, setTimeRange] = useState<TimeRange>('1month');

  if (!retailers || retailers.length === 0) {
    return (
      <div className="bg-white rounded-xl shadow-sm p-5 flex items-center justify-center h-64">
        <p className="text-gray-500">Price history is not available.</p>
      </div>
    );
  }
  
  // Function to filter data based on selected time range
  const getFilteredData = () => {
    const now = new Date();
    const timeFilters = {
      '7days': new Date(now.setDate(now.getDate() - 7)),
      '1month': new Date(now.setMonth(now.getMonth() - 1)),
      '3months': new Date(now.setMonth(now.getMonth() - 3)),
      'all': new Date(0), // beginning of time
    };
    
    const cutoffDate = timeFilters[timeRange];
    
    // Create an object to track min price by date
    const dateToMinPrice: Record<string, number> = {};
    const allPrices: Record<string, Record<string, number>> = {};
    
    retailers.forEach(retailer => {
      retailer.priceHistory.forEach(entry => {
        const entryDate = new Date(entry.date);
        if (entryDate >= cutoffDate) {
          const dateStr = entryDate.toISOString().split('T')[0];
          
          // Track prices by retailer
          if (!allPrices[dateStr]) {
            allPrices[dateStr] = {};
          }
          allPrices[dateStr][retailer.name] = entry.price;
          
          // Track min price
          if (!dateToMinPrice[dateStr] || entry.price < dateToMinPrice[dateStr]) {
            dateToMinPrice[dateStr] = entry.price;
          }
        }
      });
    });
    
    // Convert to chart data format
    const data = Object.keys(allPrices).sort().map(date => {
      const entry: any = { date };
      
      retailers.forEach(retailer => {
        entry[retailer.name] = allPrices[date][retailer.name] || null;
      });
      
      entry.lowestPrice = dateToMinPrice[date];
      return entry;
    });
    
    return data;
  };
  
  const data = getFilteredData();
  
  // Get min and max price to set chart bounds
  const allPrices = data.flatMap(entry => 
    retailers.map(retailer => entry[retailer.name]).filter(Boolean)
  );
  const minPrice = Math.min(...allPrices) * 0.95;
  const maxPrice = Math.max(...allPrices) * 1.05;
  
  // Color map for retailers
  const retailerColors = {
    'Amazon.ae': '#FF9900',
    'Noon': '#FEEE00',
    'Lulu': '#E40521',
    'lowestPrice': '#4CAF50'
  };
  
  // Calculate lowest ever price
  const lowestEverPrice = Math.min(...retailers.flatMap(r => 
    r.priceHistory.map(entry => entry.price)
  ));
  
  const formatDate = (date: string) => {
    const d = new Date(date);
    return `${d.getDate()}/${d.getMonth() + 1}`;
  };
  
  return (
    <div className="bg-white rounded-xl shadow-sm p-5">
      <div className="flex flex-col md:flex-row md:items-center justify-between mb-6">
        <h3 className="text-xl font-semibold text-secondary">Price History</h3>
        
        <div className="flex items-center mt-4 md:mt-0">
          {/* Time range selector */}
          <div className="flex rounded-lg border border-gray-200 overflow-hidden mr-4">
            {(["7days", "1month", "3months", "all"] as TimeRange[]).map((range) => (
              <button
                key={range}
                onClick={() => setTimeRange(range)}
                className={`px-3 py-1.5 text-xs font-medium ${
                  timeRange === range 
                    ? 'bg-primary text-white' 
                    : 'bg-white text-gray-600 hover:bg-gray-50'
                }`}
              >
                {range === '7days' ? '7d' : 
                 range === '1month' ? '1m' : 
                 range === '3months' ? '3m' : 'All'}
              </button>
            ))}
          </div>
          
          {/* Export button */}
          <button 
            className="inline-flex items-center text-sm text-secondary hover:text-primary"
            title="Export data"
          >
            <Download size={18} />
          </button>
        </div>
      </div>
      
      <div className="mb-4 flex items-center text-sm">
        <ArrowDownRight size={14} className="text-success mr-1" />
        <span>Lowest ever price: <strong>{lowestEverPrice.toFixed(2)} AED</strong></span>
      </div>
      
      <div className="h-64 md:h-80">
        <ResponsiveContainer width="100%" height="100%">
          <LineChart
            data={data}
            margin={{ top: 5, right: 10, left: 10, bottom: 5 }}
          >
            <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
            <XAxis 
              dataKey="date" 
              tickFormatter={formatDate}
              stroke="#9CA3AF"
              tick={{ fontSize: 12 }}
            />
            <YAxis 
              domain={[minPrice, maxPrice]} 
              stroke="#9CA3AF"
              tick={{ fontSize: 12 }}
              tickFormatter={(value) => `${value.toFixed(0)}`}
            />
            <Tooltip 
              formatter={(value: number) => [`${value.toFixed(2)} AED`, ""]}
              labelFormatter={(label) => new Date(label).toLocaleDateString()}
            />
            <Legend />
            
            {retailers.map((retailer) => (
              <Line
                key={retailer.id}
                type="monotone"
                dataKey={retailer.name}
                stroke={retailerColors[retailer.name as keyof typeof retailerColors] || '#8884d8'}
                strokeWidth={2}
                dot={{ r: 3 }}
                activeDot={{ r: 5 }}
              />
            ))}
            
            <Line
              type="monotone"
              dataKey="lowestPrice"
              stroke={retailerColors.lowestPrice}
              strokeWidth={2}
              strokeDasharray="5 5"
              dot={false}
              name="Lowest Price"
            />
          </LineChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
};

export default PriceHistoryChart;