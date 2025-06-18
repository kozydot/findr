import { createContext, useContext, useState, ReactNode, useEffect, useCallback } from 'react';
import { ref, onValue } from 'firebase/database';
import { db } from '../../firebase';
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
    const fetchProducts = () => {
      const productsRef = ref(db, 'products');
      
      const unsubscribe = onValue(productsRef, (snapshot) => {
        try {
          if (snapshot.exists()) {
            const data = snapshot.val();
            const productArray: Product[] = Object.keys(data).map(key => ({
              id: key,
              ...data[key]
            }));
            
            setProducts(productArray);
            
            // Filter for popular Amazon products and sort by reviews (popularity)
            const amazonProducts = productArray.filter(product => 
              product.retailers?.some(retailer => 
                retailer.name?.toLowerCase().includes('amazon')
              )
            );
            
            // Sort by review count (descending) to get most popular products
            const popularProducts = amazonProducts
              .sort((a, b) => (b.reviews || 0) - (a.reviews || 0))
              .slice(0, 6); // Show top 6 most popular
            
            setFeaturedProducts(popularProducts);
          } else {
            setProducts([]);
            setFeaturedProducts([]);
          }
          setError(null);
        } catch (err: any) {
          setError(err.message || 'Failed to fetch products');
        } finally {
          setLoading(false);
        }
      }, (error) => {
        setError(error.message || 'Failed to fetch products');
        setLoading(false);
      });
      
      return unsubscribe;
    };

    const unsubscribe = fetchProducts();
    
    // Cleanup subscription on unmount
    return () => {
      if (unsubscribe) {
        unsubscribe();
      }
    };
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
