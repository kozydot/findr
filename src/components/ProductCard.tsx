import { useState } from 'react';
import { Link } from 'react-router-dom';
import { ChevronRight, Star, ArrowDown, ArrowUp, Bookmark } from 'lucide-react';
import { Product, Retailer } from '../types';
import { useAuth } from '../context/AuthContext';
import { useAlerts } from '../context/AlertContext';

interface ProductCardProps {
  product: Product;
}

const ProductCard = ({ product }: ProductCardProps) => {
  const { isAuthenticated } = useAuth();
  const { addAlert, removeAlert, isProductInAlerts } = useAlerts();
  const [isAlertActive, setIsAlertActive] = useState(isProductInAlerts(product.id));
  
  const lowestPrice = Math.min(
    ...(product.retailers || []).map(retailer => retailer.currentPrice)
  );
  
  const bestRetailer = isFinite(lowestPrice)
    ? product.retailers.find(retailer => retailer.currentPrice === lowestPrice)
    : undefined;
  
  const priceChange = (retailer: Retailer) => {
    const change = retailer.priceHistory[0].price - retailer.priceHistory[1].price;
    return change;
  };
  
  const toggleAlert = () => {
    if (isAlertActive) {
      removeAlert(product.id);
      setIsAlertActive(false);
    } else {
      addAlert({
        productId: product.id,
        productName: product.name,
        targetPrice: lowestPrice * 0.9, // Default target: 10% below current price
        imageUrl: product.imageUrl,
        currentPrice: lowestPrice,
      });
      setIsAlertActive(true);
    }
  };
  
  return (
    <div className="card group">
      {/* Image and alert button */}
      <div className="relative overflow-hidden">
        <Link to={`/product/${product.id}`}>
          <img 
            src={product.imageUrl} 
            alt={product.name}
            className="w-full h-48 object-contain p-4 transition-transform group-hover:scale-105" 
          />
        </Link>
        
        {isAuthenticated && (
          <button
            onClick={toggleAlert}
            disabled={!isFinite(lowestPrice)}
            className={`absolute top-3 right-3 p-2 rounded-full transition-colors ${
              isAlertActive
                ? 'bg-primary text-white'
                : 'bg-white/90 text-gray-600 hover:text-primary'
            } ${
              !isFinite(lowestPrice)
                ? 'cursor-not-allowed opacity-50'
                : 'shadow-sm'
            }`}
          >
            <Bookmark size={18} />
          </button>
        )}
      </div>
      
      {/* Content */}
      <div className="p-4">
        <Link to={`/product/${product.id}`}>
          <h3 className="font-medium text-lg line-clamp-2 mb-2 hover:text-primary transition-colors">
            {product.name}
          </h3>
        </Link>
        
        <div className="flex items-center mb-3">
          <div className="flex items-center text-yellow-500">
            {[...Array(5)].map((_, i) => (
              <Star
                key={i}
                size={14}
                fill={i < Math.floor(product.rating) ? "currentColor" : "none"}
                stroke={i < Math.floor(product.rating) ? "currentColor" : "currentColor"}
                className="mr-0.5"
              />
            ))}
          </div>
          <span className="ml-1 text-sm text-gray-500">({product.reviews})</span>
        </div>
        
        {/* Best price section */}
        <div className="mb-4" style={{ minHeight: '52px' }}>
          {isFinite(lowestPrice) && bestRetailer ? (
            <>
              <div className="text-sm text-gray-500 mb-1">Best price from:</div>
              <div className="flex items-center justify-between">
                <div className="flex items-center">
                  <img
                    src={bestRetailer.logo}
                    alt={bestRetailer.name}
                    className="h-5 mr-2"
                  />
                  <span className="font-semibold text-secondary">{bestRetailer.name}</span>
                </div>
                <div className="text-xl font-bold text-primary">
                  {lowestPrice.toFixed(2)} AED
                </div>
              </div>
            </>
          ) : (
            <div className="flex items-center h-full">
              <p className="text-sm text-gray-500">No price information available.</p>
            </div>
          )}
        </div>
        
        {/* Price comparison mini-table */}
        <div className="border-t border-gray-100 pt-3 space-y-2">
          {product.retailers.map((retailer) => (
            <div key={retailer.id} className="flex items-center justify-between text-sm">
              <div className="flex items-center">
                <img 
                  src={retailer.logo} 
                  alt={retailer.name}
                  className="h-4 mr-2" 
                />
                <span className="text-gray-600">{retailer.name}</span>
              </div>
              <div className="flex items-center">
                <span className={`font-medium ${retailer.currentPrice === lowestPrice ? 'text-success' : 'text-gray-700'}`}>
                  {retailer.currentPrice.toFixed(2)} AED
                </span>
                
                {priceChange(retailer) !== 0 && (
                  <span className={`ml-2 flex items-center text-xs ${
                    priceChange(retailer) < 0 ? 'text-success' : 'text-error'
                  }`}>
                    {priceChange(retailer) < 0 ? (
                      <ArrowDown size={12} className="mr-0.5" />
                    ) : (
                      <ArrowUp size={12} className="mr-0.5" />
                    )}
                    {Math.abs(priceChange(retailer)).toFixed(2)}
                  </span>
                )}
              </div>
            </div>
          ))}
        </div>
        
        {/* View details link */}
        <Link 
          to={`/product/${product.id}`}
          className="mt-4 inline-flex items-center text-sm font-medium text-primary hover:underline"
        >
          View full comparison
          <ChevronRight size={16} className="ml-1" />
        </Link>
      </div>
    </div>
  );
};

export default ProductCard;