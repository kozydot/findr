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
    
    const normalizedQuery = query.toLowerCase().trim();
    const queryTerms = normalizedQuery.split(/\s+/);
    
    // Enhanced search with relevance scoring
    const productsWithScores = products.map(product => {
      const score = calculateRelevanceScore(product, normalizedQuery, queryTerms);
      return { product, score };
    });
    
    return productsWithScores
      .filter(({ score }) => score >= 0.3) // Filter out irrelevant products
      .sort((a, b) => b.score - a.score) // Sort by relevance score
      .slice(0, 50) // Limit to top 50 results
      .map(({ product }) => product);
  }, [products]);
  
  const calculateRelevanceScore = (product: Product, normalizedQuery: string, queryTerms: string[]): number => {
    const productName = product.name.toLowerCase();
    const productDescription = product.description?.toLowerCase() || '';
    
    // Extract brand from product name (first word or common brand patterns)
    const extractedBrand = extractBrandFromName(productName);
    
    let score = 0;
    
    // 1. Exact query match in product name (highest weight)
    if (productName.includes(normalizedQuery)) {
      if (productName.startsWith(normalizedQuery)) {
        score += 10; // Product name starts with query
      } else if (isMainProduct(productName, normalizedQuery)) {
        score += 8; // Query appears as main product
      } else {
        score += 3; // Query appears somewhere in name
      }
    }
    
    // 2. Individual term matching
    queryTerms.forEach(term => {
      if (term.length < 2) return;
      
      // Brand matching
      if (extractedBrand.includes(term)) {
        score += 5;
      }
      
      // Product name term matching with position weighting
      if (productName.includes(term)) {
        const position = productName.indexOf(term);
        if (position === 0) {
          score += 4; // Term at beginning
        } else if (position < productName.length / 3) {
          score += 3; // Term in first third
        } else {
          score += 1.5; // Term elsewhere
        }
      }
      
      // Description matching (lower priority)
      if (productDescription.includes(term)) {
        score += 1;
      }
    });
    
    // 3. Penalize accessory products
    if (isAccessoryProduct(productName, productDescription)) {
      score *= 0.3;
    }
    
    // 4. Boost main category products
    if (isMainCategoryProduct(productName, normalizedQuery)) {
      score *= 1.5;
    }
    
    return Math.max(0, score);
  };
  
  const extractBrandFromName = (productName: string): string => {
    const words = productName.toLowerCase().split(/\s+/);
    if (words.length > 0) {
      const firstWord = words[0];
      // Common brand names
      if (['apple', 'samsung', 'google', 'microsoft', 'sony', 'lg', 'dell', 'hp', 'lenovo', 'asus'].includes(firstWord)) {
        return firstWord;
      }
    }
    return '';
  };
  
  const isMainProduct = (productName: string, query: string): boolean => {
    const productWords = productName.split(/\W+/);
    const queryWords = query.split(/\W+/);
    
    // Look for consecutive matching words from query in product name
    for (let i = 0; i <= productWords.length - queryWords.length; i++) {
      let matches = true;
      for (let j = 0; j < queryWords.length; j++) {
        if (productWords[i + j] !== queryWords[j]) {
          matches = false;
          break;
        }
      }
      if (matches) return true;
    }
    return false;
  };
  
  const isAccessoryProduct = (productName: string, description: string): boolean => {
    const accessoryKeywords = [
      'adapter', 'cable', 'charger', 'case', 'cover', 'stand', 'mount', 'holder',
      'screen protector', 'tempered glass', 'usb', 'lightning',
      'wireless charger', 'power bank', 'car charger', 'wall charger', 'dock',
      'headphones', 'earphones', 'speaker', 'bluetooth', 'airpods case',
      'repair tool', 'screwdriver', 'kit', 'tool set', 'cleaning', 'cleaner',
      'stylus', 'pen', 'grip', 'ring holder', 'car mount', 'dashboard'
    ];
    
    const combinedText = (productName + ' ' + description).toLowerCase();
    return accessoryKeywords.some(keyword => combinedText.includes(keyword));
  };
  
  const isMainCategoryProduct = (productName: string, query: string): boolean => {
    if (query.includes('iphone')) {
      return /\biphone\s+\d+/.test(productName) || /\biphone\s+(pro|mini|plus|max)/.test(productName);
    }
    
    if (query.includes('samsung') || query.includes('galaxy')) {
      return /\bgalaxy\s+\w+\d+/.test(productName) || /\bsamsung\s+galaxy/.test(productName);
    }
    
    if (query.includes('macbook')) {
      return /\bmacbook\s+(air|pro)/.test(productName);
    }
    
    if (query.includes('ipad')) {
      return /\bipad\s+(air|pro|mini)?/.test(productName);
    }
    
    return false;
  };

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
