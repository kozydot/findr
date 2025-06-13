import { createContext, useContext, useState, ReactNode, useEffect, useCallback } from 'react';
import { Product } from '../types';

interface ProductContextType {
  products: Product[];
  featuredProducts: Product[];
  loading: boolean;
  error: string | null;
  getProductById: (id: string) => Product | undefined;
  searchProducts: (query: string) => Product[];
}

const ProductContext = createContext<ProductContextType | undefined>(undefined);

export const ProductProvider = ({ children }: { children: ReactNode }) => {
  const [products, setProducts] = useState<Product[]>([]);
  const [featuredProducts, setFeaturedProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchProducts = async () => {
      try {
        const response = await fetch('/api/v1/products');
        if (!response.ok) {
          throw new Error('Failed to fetch products');
        }
        const data = await response.json();
        setProducts(data);
      } catch (err: any) {
        setError(err.message);
      } finally {
        setLoading(false);
      }
    };

    const fetchFeaturedProducts = async () => {
      try {
        const response = await fetch('/api/v1/products/featured');
        if (!response.ok) {
          throw new Error('Failed to fetch featured products');
        }
        const data = await response.json();
        setFeaturedProducts(data);
      } catch (err: any) {
        setError(err.message);
      }
    };

    fetchProducts();
    fetchFeaturedProducts();
  }, []);
  
  const getProductById = useCallback((id: string): Product | undefined => {
    return products.find(product => product.id === id);
  }, [products]);
  
  const searchProducts = useCallback((query: string): Product[] => {
    if (!query) return products;
    
    const lowerQuery = query.toLowerCase();
    return products.filter(product =>
      product.name.toLowerCase().includes(lowerQuery) ||
      (product.description && product.description.toLowerCase().includes(lowerQuery))
    );
  }, [products]);

  return (
    <ProductContext.Provider value={{ products, featuredProducts, loading, error, getProductById, searchProducts }}>
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
