import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Bell, PlusCircle, AlertTriangle } from 'lucide-react';
import AlertCard from '../components/AlertCard';
import { useAlerts } from '../context/AlertContext';

const AlertsPage = () => {
  const { alerts, clearAllAlerts } = useAlerts();
  const navigate = useNavigate();
  const [showConfirmClear, setShowConfirmClear] = useState(false);
  
  const handleClearAll = () => {
    setShowConfirmClear(true);
  };
  
  const confirmClearAll = () => {
    clearAllAlerts();
    setShowConfirmClear(false);
  };
  
  return (
    <div className="container mx-auto px-4 py-8">
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-secondary dark:text-white">My Price Alerts</h1>
        <p className="text-gray-600 dark:text-gray-400 mt-2">
          We'll notify you when prices drop below your target.
        </p>
      </div>
      
      {alerts.length > 0 ? (
        <>
          <div className="flex justify-between items-center mb-6">
            <p className="text-sm text-gray-600 dark:text-gray-400">
              You have {alerts.length} active {alerts.length === 1 ? 'alert' : 'alerts'}
            </p>
            <button 
              onClick={handleClearAll}
              className="text-sm text-primary hover:underline"
            >
              Clear all alerts
            </button>
          </div>
          
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {alerts.map((alert) => (
              <AlertCard key={alert.productId} alert={alert} />
            ))}
          </div>
        </>
      ) : (
        <div className="bg-white dark:bg-gray-800 rounded-xl shadow-sm p-8 text-center max-w-lg mx-auto">
          <div className="w-16 h-16 rounded-full bg-primary/10 flex items-center justify-center mx-auto mb-6">
            <Bell size={24} className="text-primary" />
          </div>
          <h2 className="text-xl font-semibold mb-3 dark:text-white">No price alerts yet</h2>
          <p className="text-gray-600 dark:text-gray-400 mb-6">
            Create alerts for products you're interested in, and we'll notify you when prices drop.
          </p>
          <button 
            onClick={() => navigate('/search')}
            className="btn btn-primary"
          >
            <PlusCircle size={18} className="mr-2" />
            Add Price Alert
          </button>
        </div>
      )}
      
      {/* How it works section */}
      <div className="mt-12 bg-gray-50 dark:bg-gray-800/50 rounded-xl p-6">
        <h2 className="text-xl font-semibold mb-4 dark:text-white">How Price Alerts Work</h2>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <div className="bg-white dark:bg-gray-800 p-4 rounded-lg">
            <div className="w-10 h-10 rounded-full bg-primary/10 flex items-center justify-center mb-3">
              <span className="font-semibold text-primary">1</span>
            </div>
            <h3 className="font-medium mb-2 dark:text-white">Set Your Target Price</h3>
            <p className="text-sm text-gray-600 dark:text-gray-400">
              Choose a price that you'd be happy to pay for a product.
            </p>
          </div>
          
          <div className="bg-white dark:bg-gray-800 p-4 rounded-lg">
            <div className="w-10 h-10 rounded-full bg-primary/10 flex items-center justify-center mb-3">
              <span className="font-semibold text-primary">2</span>
            </div>
            <h3 className="font-medium mb-2 dark:text-white">We Monitor Prices</h3>
            <p className="text-sm text-gray-600 dark:text-gray-400">
              Our system checks prices across all supported retailers multiple times daily.
            </p>
          </div>
          
          <div className="bg-white dark:bg-gray-800 p-4 rounded-lg">
            <div className="w-10 h-10 rounded-full bg-primary/10 flex items-center justify-center mb-3">
              <span className="font-semibold text-primary">3</span>
            </div>
            <h3 className="font-medium mb-2 dark:text-white">Get Notified</h3>
            <p className="text-sm text-gray-600 dark:text-gray-400">
              Receive email notifications as soon as the price drops below your target.
            </p>
          </div>
        </div>
      </div>
      
      {/* Confirmation modal */}
      {showConfirmClear && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
          <div className="bg-white dark:bg-gray-800 rounded-xl p-6 max-w-md w-full">
            <div className="flex items-start mb-4">
              <AlertTriangle size={24} className="text-warning mr-3 flex-shrink-0" />
              <div>
                <h3 className="text-lg font-semibold mb-2 dark:text-white">Clear all alerts?</h3>
                <p className="text-gray-600 dark:text-gray-400 text-sm">
                  This will remove all your price alerts. This action cannot be undone.
                </p>
              </div>
            </div>
            
            <div className="flex justify-end space-x-3 mt-6">
              <button
                onClick={() => setShowConfirmClear(false)}
                className="btn bg-gray-100 text-secondary hover:bg-gray-200 dark:bg-gray-700 dark:text-gray-300 dark:hover:bg-gray-600"
              >
                Cancel
              </button>
              <button 
                onClick={confirmClearAll}
                className="btn bg-error text-white hover:bg-error/90"
              >
                Clear All
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default AlertsPage;