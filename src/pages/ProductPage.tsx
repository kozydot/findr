import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import {
  Share2, Star, ChevronRight, ArrowLeft,
  ShoppingCart, Heart
} from 'lucide-react';
import PriceComparisonTable from '../components/PriceComparisonTable';
import { Product } from '../types';
import BookmarkButton from '../components/BookmarkButton';
import { useAuth } from '../context/AuthContext';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const ProductPage = () => {
  const { id } = useParams<{ id: string }>();
  const [product, setProduct] = useState<Product | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<'description' | 'specifications'>('description');
  const [isFavorite, setIsFavorite] = useState(false);
  const { isAuthenticated } = useAuth();

  useEffect(() => {
    let isMounted = true;
    const client = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      onConnect: () => {
        client.subscribe(`/topic/products/${id}`, message => {
          const updatedProduct = JSON.parse(message.body);
          if (isMounted) {
            setProduct(prevProduct => {
              if (!prevProduct) return updatedProduct;

              const newProductData = { ...prevProduct };

              // Deep merge retailers to avoid duplicates
              if (updatedProduct.retailers) {
                const newRetailers = updatedProduct.retailers || [];
                const existingRetailers = prevProduct.retailers || [];
                const allRetailers = [...existingRetailers, ...newRetailers];
                const uniqueRetailers = allRetailers.filter((r, i, a) => a.findIndex(t => t.retailerId === r.retailerId) === i);
                newProductData.retailers = uniqueRetailers;
              }

              // Merge other properties, avoiding null overwrites
              for (const key in updatedProduct) {
                if (key !== 'retailers' && Object.prototype.hasOwnProperty.call(updatedProduct, key)) {
                  const value = updatedProduct[key as keyof Product];
                  if (value != null) {
                    // @ts-ignore
                    newProductData[key] = value;
                  }
                }
              }
              
              return newProductData;
            });
          }
        });
      },
    });

    const fetchProduct = async () => {
      if (!id) {
        setLoading(false);
        setError("No product ID provided.");
        return;
      }
      try {
        setLoading(true);
        const response = await fetch(`/api/v1/products/${id}`);
        if (!response.ok) {
          if (response.status === 404) {
            setProduct(null);
          } else {
            throw new Error(`Failed to fetch product data. Status: ${response.status}`);
          }
        } else {
          const productData: Product = await response.json();
          if (isMounted) {
            setProduct(productData);
            if (!productData.description) {
              await fetch(`/api/v1/products/${id}/enrich`, { method: 'POST' });
            }
            const compareResponse = await fetch(`/api/v1/products/${id}/compare`, { method: 'POST' });
            const taskId = await compareResponse.text();
            pollForComparison(taskId);
          }
        }
      } catch (err: any) {
        if (isMounted) {
          setError(err.message);
        }
      } finally {
        if (isMounted) {
          setLoading(false);
        }
      }
    };

    const pollForComparison = (taskId: string) => {
      const interval = setInterval(async () => {
        try {
          const response = await fetch(`/api/v1/products/comparison/${taskId}`);
          if (response.status === 200) {
            const updatedProduct: Product = await response.json();
            if (isMounted) {
              setProduct(prevProduct => {
                if (!prevProduct) return updatedProduct;

                const newProductData = { ...prevProduct };

                // Deep merge retailers to avoid duplicates
                if (updatedProduct.retailers) {
                  const newRetailers = updatedProduct.retailers || [];
                  const existingRetailers = prevProduct.retailers || [];
                  const allRetailers = [...existingRetailers, ...newRetailers];
                  
                  // Advanced deduplication: prioritize by retailerId first, then by normalized name
                  const seenRetailerIds = new Set<string>();
                  const seenNormalizedNames = new Set<string>();
                  
                  const uniqueRetailers = allRetailers.filter((retailer) => {
                    // Check for duplicate retailerId
                    if (seenRetailerIds.has(retailer.retailerId)) {
                      return false;
                    }
                    
                    // Check for duplicate retailer names (especially Amazon variations)
                    const normalizedName = retailer.name?.toLowerCase().replace(/[^a-z]/g, '') || '';
                    if (normalizedName.includes('amazon')) {
                      if (seenNormalizedNames.has('amazon')) {
                        return false;
                      }
                      seenNormalizedNames.add('amazon');
                    } else if (normalizedName && seenNormalizedNames.has(normalizedName)) {
                      return false;
                    } else if (normalizedName) {
                      seenNormalizedNames.add(normalizedName);
                    }
                    
                    seenRetailerIds.add(retailer.retailerId);
                    return true;
                  });
                  
                  newProductData.retailers = uniqueRetailers;
                }

                // Merge other properties, avoiding null overwrites
                for (const key in updatedProduct) {
                  if (key !== 'retailers' && Object.prototype.hasOwnProperty.call(updatedProduct, key)) {
                    const value = updatedProduct[key as keyof Product];
                    if (value != null) {
                      // @ts-ignore
                      newProductData[key] = value;
                    }
                  }
                }
                
                return newProductData;
              });
              clearInterval(interval);
            }
          }
        } catch (err) {
          console.error("Polling error:", err);
          clearInterval(interval);
        }
      }, 2000);
    };

    fetchProduct();
    client.activate();

    return () => {
      isMounted = false;
      client.deactivate();
    };
  }, [id]);

  if (loading) {
    return (
      <div className="container mx-auto px-4 py-10 min-h-screen flex items-center justify-center">
        <div className="w-12 h-12 border-4 border-primary border-t-transparent rounded-full animate-spin"></div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="container mx-auto px-4 py-10 min-h-screen flex items-center justify-center">
        <div className="text-center">
          <h2 className="text-2xl font-bold mb-4 text-red-600">Error Loading Product</h2>
          <p className="text-gray-600 mb-4">{error}</p>
          <Link to="/" className="btn btn-primary">
            Go Home
          </Link>
        </div>
      </div>
    );
  }
  
  if (!product) {
    return (
      <div className="container mx-auto px-4 py-10 min-h-screen">
        <div className="text-center">
          <h2 className="text-2xl font-bold mb-4">Product Not Found</h2>
          <p className="mb-6">The product you're looking for does not exist or has been removed.</p>
          <Link to="/search" className="btn btn-primary">
            Continue Shopping
          </Link>
        </div>
      </div>
    );
  }
  
  const hasRetailers = product && product.retailers && product.retailers.length > 0;

  const lowestPrice = hasRetailers
    ? Math.min(...product.retailers.map(retailer => retailer.currentPrice))
    : 0;
  
  const bestRetailer = hasRetailers
    ? product.retailers.find(retailer => retailer.currentPrice === lowestPrice)
    : null;
  
  const handleShareClick = () => {
    if (navigator.share) {
      navigator.share({
        title: product.name,
        text: `Check out this great deal on ${product.name}`,
        url: window.location.href,
      });
    } else {
      navigator.clipboard.writeText(window.location.href)
        .then(() => alert('Link copied to clipboard!'))
        .catch(err => console.error('Could not copy text: ', err));
    }
  };
  
  const toggleFavorite = () => {
    setIsFavorite(!isFavorite);
  };
  
  return (
    <div className="container mx-auto px-4 py-6 md:py-10">
      <div className="flex items-center text-sm text-gray-500 dark:text-gray-400 mb-6">
        <Link to="/" className="hover:text-primary">Home</Link>
        <ChevronRight size={14} className="mx-2" />
        <Link to="/search" className="hover:text-primary">Search</Link>
        <ChevronRight size={14} className="mx-2" />
        <span className="text-gray-700 dark:text-gray-300 font-medium line-clamp-1">
          {product.name}
        </span>
      </div>
      
      <Link to="/search" className="inline-flex items-center text-secondary dark:text-gray-300 hover:text-primary mb-6">
        <ArrowLeft size={18} className="mr-2" />
        Back to search results
      </Link>
      
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-12">
        {/* Main Content */}
        <div className="lg:col-span-2">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
            <div>
              <div className="bg-white dark:bg-gray-800 rounded-xl shadow-sm p-6 flex items-center justify-center">
                <img
                  src={product.imageUrl}
                  alt={product.name}
                  className="max-w-full max-h-80 object-contain"
                />
              </div>
              <div className="flex justify-center mt-4 space-x-2">
                {[1, 2, 3].map((_, index) => (
                  <button
                    key={index}
                    className={`w-16 h-16 border rounded-lg overflow-hidden ${index === 0 ? 'border-primary' : 'border-gray-200 dark:border-gray-700'}`}
                  >
                    <img
                      src={product.imageUrl}
                      alt={`${product.name} - view ${index + 1}`}
                      className="w-full h-full object-contain"
                    />
                  </button>
                ))}
              </div>
            </div>
            <div>
              <h1 className="text-2xl md:text-3xl font-bold text-secondary dark:text-white mb-4">
                {product.name}
              </h1>
              <div className="flex items-center mb-4">
                <div className="flex items-center text-yellow-500">
                  {[...Array(5)].map((_, i) => (
                    <Star
                      key={i}
                      size={16}
                      fill={i < Math.floor(product.rating) ? "currentColor" : "none"}
                      stroke="currentColor"
                      className="mr-0.5"
                    />
                  ))}
                </div>
                <span className="ml-2 text-sm text-gray-600 dark:text-gray-400">{product.rating} ({product.reviews} reviews)</span>
              </div>
              {hasRetailers && bestRetailer && (
                <>
                  <div className="mb-6">
                    <div className="text-sm text-gray-500 dark:text-gray-400 mb-1">Best price from:</div>
                    <div className="flex items-center justify-between">
                      <div className="flex items-center">
                        <span className="font-medium text-secondary dark:text-white">{bestRetailer.name}</span>
                      </div>
                      <div className="text-2xl font-bold text-primary">
                        {lowestPrice.toFixed(2)} {product.currency === 'AE' ? 'AED' : product.currency}
                      </div>
                    </div>
                  </div>
                  <div className="flex space-x-3 mb-6">
                    <a
                      href={bestRetailer.productUrl.startsWith('/aclk') ? `https://www.google.com${bestRetailer.productUrl}` : bestRetailer.productUrl}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="btn btn-primary flex-grow flex items-center justify-center"
                    >
                      <ShoppingCart size={18} className="mr-2" />
                      Buy Now
                    </a>
                    <button
                      onClick={handleShareClick}
                      className="btn bg-gray-100 dark:bg-gray-700 text-secondary dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-600 p-3"
                      aria-label="Share"
                    >
                      <Share2 size={18} />
                    </button>
                    {isAuthenticated && (
                      <BookmarkButton product={product} />
                    )}
                  </div>
                </>
              )}
            </div>
          </div>

          <div className="mt-10">
            <div className="border-b border-gray-200 dark:border-gray-700 mb-4">
              <div className="flex">
                <button
                  onClick={() => setActiveTab('description')}
                  className={`py-3 px-4 text-sm font-medium border-b-2 ${
                    activeTab === 'description'
                      ? 'border-primary text-primary'
                      : 'border-transparent text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-300'
                  }`}
                >
                  Description
                </button>
                <button
                  onClick={() => setActiveTab('specifications')}
                  className={`py-3 px-4 text-sm font-medium border-b-2 ${
                    activeTab === 'specifications'
                      ? 'border-primary text-primary'
                      : 'border-transparent text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-300'
                  }`}
                >
                  Specifications
                </button>
              </div>
            </div>
            <div className="text-gray-600 dark:text-gray-300">
              {activeTab === 'description' ? (
                <div className="prose prose-sm max-w-none dark:prose-invert">
                  <p>{product.description}</p>
                </div>
              ) : (
                <div className="border border-gray-200 dark:border-gray-700 rounded-lg overflow-hidden">
                  <div className="divide-y divide-gray-200 dark:divide-gray-700">
                    {product.specifications && product.specifications.length > 0 ? (
                      product.specifications.map((spec, index) => (
                        <div key={index} className="px-4 py-5 sm:grid sm:grid-cols-3 sm:gap-4 sm:px-6">
                          <div className="text-sm font-medium text-gray-500 dark:text-gray-400">{spec.name}</div>
                          <div className="mt-1 text-sm text-gray-900 dark:text-white sm:mt-0 sm:col-span-2">{spec.value}</div>
                        </div>
                      ))
                    ) : (
                      <div className="px-4 py-5 sm:px-6">
                        <p className="text-sm text-gray-500 dark:text-gray-400">No specifications available for this product.</p>
                      </div>
                    )}
                  </div>
                </div>
              )}
            </div>
          </div>
          
          {hasRetailers && (
            <div className="mt-10">
            </div>
          )}
        </div>

        {/* Sidebar */}
        <div className="lg:col-span-1">
          <div className="sticky top-6">
            {hasRetailers ? (
              <div className="mt-6">
                <PriceComparisonTable retailers={product.retailers} currency={product.currency} />
              </div>
            ) : (
              <div className="bg-white dark:bg-gray-800 rounded-xl shadow-sm border border-gray-100 dark:border-gray-700 p-6 text-center">
                <h3 className="font-semibold text-lg mb-2 dark:text-white">No Price Data</h3>
                <p className="text-gray-600 dark:text-gray-400 text-sm">
                  There is currently no price comparison data available for this product.
                </p>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default ProductPage;
