import { useState, useEffect } from 'react';
import { Bell, ChevronDown } from 'lucide-react';
import { useAlerts } from '../context/AlertContext';
import { Product } from '../types';

interface AlertFormProps {
  product: Product;
}

const AlertForm = ({ product }: AlertFormProps) => {
  const { addAlert, removeAlert, isProductInAlerts, getAlertForProduct } = useAlerts();
  const existingAlert = getAlertForProduct(product.id);
  
  const [targetPrice, setTargetPrice] = useState<number>(
    existingAlert?.targetPrice || 
    Math.round(product.retailers[0].currentPrice * 0.9) // Default to 10% below current price
  );
  const [isOpen, setIsOpen] = useState(false);
  const [isActive, setIsActive] = useState(isProductInAlerts(product.id));
  
  const lowestPrice = Math.min(
    ...product.retailers.map(retailer => retailer.currentPrice)
  );
  
  const discountOptions = [5, 10, 15, 20, 30];
  
  const toggleAlert = () => {
    if (isActive) {
      removeAlert(product.id);
      setIsActive(false);
    } else {
      addAlert({
        productId: product.id,
        productName: product.name,
        targetPrice,
        imageUrl: product.imageUrl,
        currentPrice: lowestPrice,
      });
      setIsActive(true);
      setIsOpen(false);
    }
  };
  
  // Set target price when selecting a discount percentage
  const setDiscountPercentage = (percentage: number) => {
    const newPrice = Math.round(lowestPrice * (100 - percentage) / 100);
    setTargetPrice(newPrice);
  };
  
  // Handle manual price input
  const handlePriceChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = parseInt(e.target.value, 10);
    if (!isNaN(value) && value > 0) {
      setTargetPrice(value);
    }
  };
  
  return (
    <div className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden">
      {/* Header */}
      <div 
        className={`flex items-center justify-between p-4 cursor-pointer ${isActive ? 'bg-primary/10' : ''}`} 
        onClick={() => setIsOpen(!isOpen)}
      >
        <div className="flex items-center">
          <Bell size={18} className={isActive ? 'text-primary' : 'text-gray-500'} />
          <span className="font-medium ml-2">Price Drop Alert</span>
        </div>
        <ChevronDown 
          size={18} 
          className={`text-gray-500 transition-transform ${isOpen ? 'rotate-180' : ''}`} 
        />
      </div>
      
      {/* Form */}
      <div className={`overflow-hidden transition-all ${isOpen || isActive ? 'max-h-96' : 'max-h-0'}`}>
        <div className="p-4 pt-1 border-t border-gray-100">
          {isActive ? (
            <div className="flex flex-col space-y-4">
              <div className="bg-primary/5 rounded-lg p-3 text-sm">
                <p className="text-secondary">
                  We'll notify you when the price drops below:
                </p>
                <p className="font-bold text-lg text-primary mt-1">
                  {existingAlert?.targetPrice.toFixed(2)} AED
                </p>
                <p className="text-gray-500 text-xs mt-1">
                  Current lowest price: {lowestPrice.toFixed(2)} AED
                </p>
              </div>
              
              <button 
                onClick={toggleAlert}
                className="btn btn-outline text-sm"
              >
                Remove Alert
              </button>
            </div>
          ) : (
            <div className="flex flex-col space-y-4">
              <p className="text-sm text-gray-600">
                We'll notify you when the price drops below your target price.
              </p>
              
              <div>
                <label htmlFor="targetPrice" className="block text-sm font-medium text-gray-700 mb-1">
                  Target Price (AED)
                </label>
                <input
                  type="number"
                  id="targetPrice"
                  value={targetPrice}
                  onChange={handlePriceChange}
                  className="input text-sm"
                  min="1"
                />
              </div>
              
              <div>
                <span className="block text-sm font-medium text-gray-700 mb-2">
                  Quick select discount
                </span>
                <div className="flex flex-wrap gap-2">
                  {discountOptions.map(discount => (
                    <button
                      key={discount}
                      onClick={() => setDiscountPercentage(discount)}
                      className={`px-3 py-1.5 rounded-full text-xs font-medium transition-colors ${
                        Math.round(lowestPrice * (100 - discount) / 100) === targetPrice
                          ? 'bg-primary text-white'
                          : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                      }`}
                    >
                      {discount}% off
                    </button>
                  ))}
                </div>
              </div>
              
              <div className="text-sm text-gray-600">
                <p>Current lowest price: <span className="font-medium">{lowestPrice.toFixed(2)} AED</span></p>
                <p>Your target: <span className="font-medium text-primary">{targetPrice.toFixed(2)} AED</span></p>
              </div>
              
              <button 
                onClick={toggleAlert}
                className="btn btn-primary text-sm"
              >
                Set Alert
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default AlertForm;