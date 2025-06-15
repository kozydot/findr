import { ShoppingCart, ExternalLink, ArrowDown, ArrowUp } from 'lucide-react';
import { Retailer } from '../types';

interface PriceComparisonTableProps {
  retailers: Retailer[];
}

const PriceComparisonTable = ({ retailers }: PriceComparisonTableProps) => {
  if (!retailers || retailers.length <= 1) {
    return (
      <div className="bg-white rounded-xl shadow-sm p-5 text-center">
        <h3 className="text-xl font-semibold text-secondary mb-2">Similar Products</h3>
        <p className="text-gray-500">No other retailers found for this product.</p>
      </div>
    );
  }

  // Sort retailers by price (lowest first)
  const sortedRetailers = [...retailers].sort((a, b) => a.currentPrice - b.currentPrice);
  
  // Get lowest price for highlighting
  const lowestPrice = sortedRetailers[0]?.currentPrice;
  
  // Calculate price change
  const getPriceChange = (retailer: Retailer) => {
    if (!retailer.priceHistory || retailer.priceHistory.length < 2) return 0;
    return retailer.priceHistory[0].price - retailer.priceHistory[1].price;
  };
  
  return (
    <div className="bg-white rounded-xl shadow-sm overflow-hidden">
      <div className="p-4 border-b border-gray-100">
        <h3 className="text-xl font-semibold text-secondary">Similar Products</h3>
      </div>
      
      <div className="overflow-x-auto">
        <table className="w-full min-w-full">
          <thead>
            <tr className="bg-gray-50">
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Retailer
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Price
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Change
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Stock
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Shipping
              </th>
              <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">
                Actions
              </th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-200">
            {sortedRetailers.map((retailer) => {
              const priceChange = getPriceChange(retailer);
              
              return (
                <tr key={retailer.retailerId} className="hover:bg-gray-50 transition-colors">
                  <td className="px-6 py-4">
                    <div className="flex items-center min-w-max">
                      <span className="font-medium">{retailer.name}</span>
                    </div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <span className={`font-bold text-lg ${retailer.currentPrice === lowestPrice ? 'text-success' : ''}`}>
                      {retailer.currentPrice.toFixed(2)} AED
                    </span>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    {priceChange !== 0 ? (
                      <div className={`flex items-center ${
                        priceChange < 0 ? 'text-success' : 'text-error'
                      }`}>
                        {priceChange < 0 ? (
                          <ArrowDown size={16} className="mr-1" />
                        ) : (
                          <ArrowUp size={16} className="mr-1" />
                        )}
                        <span>{Math.abs(priceChange).toFixed(2)} AED</span>
                      </div>
                    ) : (
                      <span className="text-gray-500">—</span>
                    )}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <span className={`px-2 py-1 inline-flex text-xs leading-5 font-semibold rounded-full ${
                      retailer.inStock ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
                    }`}>
                      {retailer.inStock ? 'In Stock' : 'Out of Stock'}
                    </span>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm">
                    {retailer.freeShipping ? (
                      <span className="text-success font-medium">Free</span>
                    ) : (
                      <span>{retailer.shippingCost.toFixed(2)} AED</span>
                    )}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-right text-sm">
                    <div className="flex justify-end space-x-2">
                      <a 
                        href={retailer.productUrl} 
                        target="_blank" 
                        rel="noopener noreferrer"
                        className="btn btn-primary py-2 text-xs"
                      >
                        <ShoppingCart size={14} className="mr-1" />
                        Buy Now
                      </a>
                      <a 
                        href={retailer.productUrl} 
                        target="_blank" 
                        rel="noopener noreferrer"
                        className="p-2 text-gray-500 hover:text-gray-700 bg-gray-100 rounded-lg"
                        title="Visit retailer"
                      >
                        <ExternalLink size={14} />
                      </a>
                    </div>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export default PriceComparisonTable;