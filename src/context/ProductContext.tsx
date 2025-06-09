import { createContext, useContext, useState, ReactNode } from 'react';
import { Product } from '../types';
import { mockProductDetails } from '../data/mockData';

interface ProductContextType {
  featuredProducts: Product[];
  getProductById: (id: string) => Product | undefined;
  searchProducts: (query: string) => Product[];
}

const ProductContext = createContext<ProductContextType | undefined>(undefined);

export const ProductProvider = ({ children }: { children: ReactNode }) => {
  const [featuredProducts] = useState<Product[]>(mockProductDetails.slice(0, 6));
  
  const getProductById = (id: string): Product | undefined => {
    return mockProductDetails.find(product => product.id === id);
  };
  
  const searchProducts = (query: string): Product[] => {
    if (!query) return mockProductDetails;
    
    const lowerQuery = query.toLowerCase();
    return mockProductDetails.filter(product => 
      product.name.toLowerCase().includes(lowerQuery) ||
      product.description.toLowerCase().includes(lowerQuery)
    );
  };
  
  return (
    <ProductContext.Provider value={{ featuredProducts, getProductById, searchProducts }}>
      {children}
    </ProductContext.Provider>
  );
};

export const useProducts = () => {
  const context = useContext(ProductContext);
  if (context === undefined) {
    throw new Error('useProducts must be used within a ProductProvider');
  }
  return context;
};