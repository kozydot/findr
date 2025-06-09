import { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { Alert } from '../types';

interface AlertContextType {
  alerts: Alert[];
  addAlert: (alert: Alert) => void;
  removeAlert: (productId: string) => void;
  clearAllAlerts: () => void;
  isProductInAlerts: (productId: string) => boolean;
  getAlertForProduct: (productId: string) => Alert | undefined;
}

const AlertContext = createContext<AlertContextType | undefined>(undefined);

export const AlertProvider = ({ children }: { children: ReactNode }) => {
  const [alerts, setAlerts] = useState<Alert[]>([]);
  
  useEffect(() => {
    // Load alerts from localStorage
    const savedAlerts = localStorage.getItem('priceAlerts');
    if (savedAlerts) {
      setAlerts(JSON.parse(savedAlerts));
    }
  }, []);
  
  const saveAlerts = (newAlerts: Alert[]) => {
    localStorage.setItem('priceAlerts', JSON.stringify(newAlerts));
  };
  
  const addAlert = (alert: Alert) => {
    setAlerts(prev => {
      // Replace if exists, add if new
      const exists = prev.some(a => a.productId === alert.productId);
      let newAlerts;
      
      if (exists) {
        newAlerts = prev.map(a => a.productId === alert.productId ? alert : a);
      } else {
        newAlerts = [...prev, alert];
      }
      
      saveAlerts(newAlerts);
      return newAlerts;
    });
  };
  
  const removeAlert = (productId: string) => {
    setAlerts(prev => {
      const newAlerts = prev.filter(alert => alert.productId !== productId);
      saveAlerts(newAlerts);
      return newAlerts;
    });
  };
  
  const clearAllAlerts = () => {
    setAlerts([]);
    localStorage.removeItem('priceAlerts');
  };
  
  const isProductInAlerts = (productId: string): boolean => {
    return alerts.some(alert => alert.productId === productId);
  };
  
  const getAlertForProduct = (productId: string): Alert | undefined => {
    return alerts.find(alert => alert.productId === productId);
  };
  
  return (
    <AlertContext.Provider value={{ 
      alerts, 
      addAlert, 
      removeAlert, 
      clearAllAlerts,
      isProductInAlerts,
      getAlertForProduct
    }}>
      {children}
    </AlertContext.Provider>
  );
};

export const useAlerts = () => {
  const context = useContext(AlertContext);
  if (context === undefined) {
    throw new Error('useAlerts must be used within an AlertProvider');
  }
  return context;
};