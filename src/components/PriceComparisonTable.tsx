import { ShoppingCart, ExternalLink } from 'lucide-react';
import { Retailer } from '../types';
import { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

interface PriceComparisonTableProps {
  retailers: Retailer[];
  currency?: string;
}

const PriceComparisonTable = ({ retailers, currency }: PriceComparisonTableProps) => {
  const { id } = useParams<{ id: string }>();
  const [isComparisonLoading, setIsComparisonLoading] = useState(true);

  useEffect(() => {
    if (retailers && retailers.length > 1) {
      setIsComparisonLoading(false);
      return;
    }

    const client = new Client({
      webSocketFactory: () => new SockJS('/ws'),      onConnect: () => {
        client.subscribe(`/topic/products/${id}/progress`, message => {
          const progress = Number(message.body);
          if (progress === 100) {
            setTimeout(() => setIsComparisonLoading(false), 1000);
          }
        });

        client.subscribe(`/topic/products/${id}`, () => {
          setIsComparisonLoading(false);
        });
      },
    });

    client.activate();

    return () => {
      client.deactivate();
    };
  }, [id, retailers]);

  if (isComparisonLoading) {
    return (
      <div className="bg-white dark:bg-gray-800 rounded-xl shadow-sm p-5 text-center flex flex-col items-center justify-center">
        <h3 className="text-xl font-semibold text-secondary dark:text-white mb-4">Finding Best Prices...</h3>
        <div className="w-12 h-12 border-4 border-primary border-t-transparent rounded-full animate-spin"></div>
      </div>
    );
  }

  if (!retailers || retailers.length <= 1) {
    return (
      <div className="bg-white dark:bg-gray-800 rounded-xl shadow-sm p-5 text-center">
        <h3 className="text-xl font-semibold text-secondary dark:text-white mb-2">Price Comparisons</h3>
        <p className="text-gray-500 dark:text-gray-400">No other retailers found for this product.</p>
      </div>
    );
  }

  // Sort retailers by price (lowest first)
  const sortedRetailers = [...retailers].sort((a, b) => a.currentPrice - b.currentPrice);

  const processedRetailers = sortedRetailers;
    // Get lowest price for highlighting
  const lowestPrice = sortedRetailers[0]?.currentPrice;
  
  return (
    <div className="bg-white dark:bg-gray-800 rounded-xl shadow-sm overflow-hidden">
      <div className="p-4 border-b border-gray-100 dark:border-gray-700 text-center">
        <h3 className="text-xl font-semibold text-secondary dark:text-white">Price Comparisons</h3>
      </div>
      
      <div>
        <table className="w-full table-fixed">
          <thead className="bg-gray-50 dark:bg-gray-700">
            <tr>
              <th className="w-1/3 px-2 sm:px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">
                Retailer
              </th>
              <th className="w-1/4 px-2 sm:px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">
                Price
              </th>
              <th className="hidden md:table-cell w-1/6 px-2 sm:px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">
                Stock
              </th>
              <th className="w-1/3 px-2 sm:px-4 py-3 text-right text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">
                Actions
              </th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-200 dark:divide-gray-700">
            {processedRetailers.map((retailer, index) => (
              <tr key={`${retailer.retailerId}-${index}`} className="hover:bg-gray-50 dark:hover:bg-gray-700/50 transition-colors">
                <td className="px-2 sm:px-4 py-4">
                  <div className="flex items-center">
                    <span className="font-medium dark:text-white truncate">{retailer.name}</span>
                  </div>
                </td>
                <td className="px-2 sm:px-4 py-4">
                  <div className="flex items-baseline">
                    <span className={`font-bold text-base sm:text-lg ${retailer.currentPrice === lowestPrice ? 'text-success' : 'dark:text-white'}`}>
                      {retailer.currentPrice.toFixed(2)}
                    </span>
                    <span className="text-sm text-gray-500 dark:text-gray-400 ml-1">{currency === 'AE' ? 'AED' : currency}</span>
                  </div>
                </td>
                <td className="hidden md:table-cell px-2 sm:px-4 py-4 whitespace-nowrap">
                  <span className={`px-2 py-1 inline-flex text-xs leading-5 font-semibold rounded-full ${
                    retailer.inStock ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
                  }`}>
                    {retailer.inStock ? 'In Stock' : 'Out of Stock'}
                  </span>
                </td>
                <td className="px-2 sm:px-4 py-4 whitespace-nowrap text-right text-sm">
                  <div className="flex justify-end items-center space-x-1 sm:space-x-2">
                    <a
                      href={retailer.productUrl.startsWith('/aclk') ? `https://www.google.com${retailer.productUrl}` : retailer.productUrl}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="btn btn-primary py-1.5 sm:py-2 px-2 sm:px-3 text-xs"
                    >
                      <ShoppingCart size={14} className="mr-1 hidden sm:inline-block" />
                      Buy
                    </a>
                    <a
                      href={retailer.productUrl.startsWith('/aclk') ? `https://www.google.com${retailer.productUrl}` : retailer.productUrl}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="p-2 text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-white bg-gray-100 dark:bg-gray-700 rounded-lg"
                      title="Visit retailer"
                    >
                      <ExternalLink size={14} />
                    </a>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export default PriceComparisonTable;