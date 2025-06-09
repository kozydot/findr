import { useNavigate } from 'react-router-dom';
import { X, Bell } from 'lucide-react';
import { Alert } from '../types';
import { useAlerts } from '../context/AlertContext';

interface AlertCardProps {
  alert: Alert;
}

const AlertCard = ({ alert }: AlertCardProps) => {
  const { removeAlert } = useAlerts();
  const navigate = useNavigate();
  
  // Calculate price difference
  const priceDifference = alert.currentPrice - alert.targetPrice;
  const percentDifference = (priceDifference / alert.currentPrice) * 100;
  
  const handleViewProduct = () => {
    navigate(`/product/${alert.productId}`);
  };
  
  const handleRemoveAlert = (e: React.MouseEvent) => {
    e.stopPropagation();
    removeAlert(alert.productId);
  };
  
  return (
    <div 
      className="card cursor-pointer hover:shadow-md transition-all duration-300"
      onClick={handleViewProduct}
    >
      <div className="flex p-4">
        {/* Product image */}
        <div className="w-20 h-20 mr-4 flex-shrink-0">
          <img 
            src={alert.imageUrl} 
            alt={alert.productName}
            className="w-full h-full object-contain" 
          />
        </div>
        
        {/* Content */}
        <div className="flex-grow">
          <div className="flex justify-between">
            <h3 className="font-medium text-secondary line-clamp-2">{alert.productName}</h3>
            
            <button 
              onClick={handleRemoveAlert}
              className="text-gray-400 hover:text-error p-1 -mr-1 -mt-1"
            >
              <X size={16} />
            </button>
          </div>
          
          <div className="mt-2 flex flex-col space-y-1">
            <div className="flex justify-between items-center">
              <span className="text-sm text-gray-500">Current price:</span>
              <span className="font-medium">{alert.currentPrice.toFixed(2)} AED</span>
            </div>
            
            <div className="flex justify-between items-center">
              <span className="text-sm text-gray-500">Target price:</span>
              <span className="font-bold text-primary">{alert.targetPrice.toFixed(2)} AED</span>
            </div>
          </div>
          
          <div className="mt-3 bg-gray-100 h-2 rounded-full overflow-hidden">
            <div 
              className="bg-primary h-full rounded-full"
              style={{ width: `${100 - Math.min(Math.max(percentDifference, 0), 100)}%` }}
            />
          </div>
          
          <div className="mt-2 flex items-center text-xs text-gray-500">
            <Bell size={12} className="mr-1" />
            <span>
              {priceDifference > 0 
                ? `Waiting for price to drop by ${priceDifference.toFixed(2)} AED (${percentDifference.toFixed(1)}%)`
                : 'Target price reached! Time to buy.'}
            </span>
          </div>
        </div>
      </div>
    </div>
  );
};

export default AlertCard;