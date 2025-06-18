import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import {
  Share2, Star, ChevronRight, ArrowLeft, ChevronLeft,
  ShoppingCart, X, ExternalLink
} from 'lucide-react';
import PriceComparisonTable from '../components/PriceComparisonTable';
import { Product } from '../types';
import BookmarkButton from '../components/BookmarkButton';
import { useAuth } from '../context/AuthContext';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const ProductPage = () => {
  const { id } = useParams<{ id: string }>();
  const { isAuthenticated } = useAuth();
  
  // All hooks must be at the top level and in consistent order
  const [product, setProduct] = useState<Product | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<'description' | 'specifications'>('description');
  const [selectedImageIndex, setSelectedImageIndex] = useState(0);
  const [showImageModal, setShowImageModal] = useState(false);
  const [isImageClicked, setIsImageClicked] = useState(false);

  // Get all available images for the product
  const getAllImages = () => {
    if (!product || !product.imageUrl) return [];
    
    const images = [product.imageUrl]; // Start with main image
    if (product.photos && product.photos.length > 0) {
      // Add additional photos, avoiding duplicates
      product.photos.forEach(photo => {
        if (photo && photo !== product.imageUrl && !images.includes(photo)) {
          images.push(photo);
        }
      });
    }
    return images.filter(img => img); // Remove any null/undefined images
  };

  const allImages = getAllImages();
  const selectedImage = allImages.length > 0 ? allImages[selectedImageIndex] || product?.imageUrl : product?.imageUrl;

  // Reset image index when product changes
  useEffect(() => {
    setSelectedImageIndex(0);
  }, [product?.id]);

  // Main effect for fetching product and WebSocket
  useEffect(() => {
    if (!id) {
      setLoading(false);
      setError("No product ID provided.");
      return;
    }

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
              Object.keys(updatedProduct).forEach(key => {
                const typedKey = key as keyof Product;
                if (updatedProduct[typedKey] !== null && updatedProduct[typedKey] !== undefined) {
                  (newProductData as Record<string, unknown>)[key] = updatedProduct[typedKey];
                }
              });
              
              return newProductData;
            });
          }
        });
      },
    });

    const fetchProduct = async () => {
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
            
            // Start comparison if product lacks detailed info
            if (!productData.description || !productData.retailers || productData.retailers.length <= 1) {
              const compareResponse = await fetch(`/api/v1/products/${id}/compare`, { method: 'POST' });
              const taskId = await compareResponse.text();
              pollForComparison(taskId);
            }
          }
        }
      } catch (err) {
        if (isMounted) {
          setError(err instanceof Error ? err.message : 'An error occurred');
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
              setProduct(updatedProduct);
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

  // Keyboard navigation for image gallery
  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      // Only handle navigation if modal is open and there are multiple images
      if (!showImageModal || allImages.length <= 1) return;
      
      if (event.key === 'ArrowLeft') {
        event.preventDefault();
        setSelectedImageIndex(prev => 
          prev > 0 ? prev - 1 : allImages.length - 1
        );
      } else if (event.key === 'ArrowRight') {
        event.preventDefault();
        setSelectedImageIndex(prev => 
          prev < allImages.length - 1 ? prev + 1 : 0
        );
      } else if (event.key === 'Escape') {
        setShowImageModal(false);
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [showImageModal, allImages.length]);

  // Handle image click with animation
  const handleImageClick = () => {
    setIsImageClicked(true);
    // Reset animation state after animation completes
    setTimeout(() => setIsImageClicked(false), 150);
    // Slight delay before opening modal for better UX
    setTimeout(() => setShowImageModal(true), 100);
  };

  // Handle share click
  const handleShareClick = () => {
    if (navigator.share) {
      navigator.share({
        title: product?.name || '',
        text: `Check out this great deal on ${product?.name || ''}`,
        url: window.location.href,
      });
    } else {
      navigator.clipboard.writeText(window.location.href)
        .then(() => alert('Link copied to clipboard!'))
        .catch(err => console.error('Could not copy text: ', err));
    }
  };

  // Early returns after all hooks are called
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
          <Link to="/" className="btn bg-primary hover:bg-primary/90 text-white px-6 py-3 rounded-lg">
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
          <Link to="/search" className="btn bg-primary hover:bg-primary/90 text-white px-6 py-3 rounded-lg">
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

  return (
    <div className="container mx-auto px-4 py-6 md:py-10">
      {/* Breadcrumb */}
      <div className="flex items-center text-sm text-gray-500 dark:text-gray-400 mb-6">
        <Link to="/" className="hover:text-primary">Home</Link>
        <ChevronRight size={14} className="mx-2" />
        <Link to="/search" className="hover:text-primary">Search</Link>
        <ChevronRight size={14} className="mx-2" />
        <span className="text-gray-700 dark:text-gray-300 font-medium line-clamp-1">
          {product.name}
        </span>
      </div>
      
      {/* Back button */}
      <Link to="/search" className="inline-flex items-center text-secondary dark:text-gray-300 hover:text-primary mb-6">
        <ArrowLeft size={18} className="mr-2" />
        Back to search results
      </Link>
      
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-12">
        {/* Main Content */}
        <div className="lg:col-span-2">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
            {/* Product Image */}
            <div>
              <div 
                className="relative group cursor-pointer"
                onClick={handleImageClick}
              >
                <img
                  src={selectedImage || '/placeholder-image.jpg'}
                  alt={product.name}
                  className={`w-full h-96 object-contain bg-gray-50 dark:bg-gray-800 rounded-lg transition-all duration-200 hover:scale-105 active:scale-95 ${
                    isImageClicked ? 'scale-110 brightness-110' : ''
                  }`}
                  style={{
                    transitionDuration: isImageClicked ? '150ms' : '200ms'
                  }}
                />
                {allImages.length > 1 && (
                  <div className="absolute top-4 right-4 bg-black/50 text-white px-2 py-1 rounded text-sm">
                    {selectedImageIndex + 1} / {allImages.length}
                  </div>
                )}
                <div className={`absolute inset-0 bg-black/0 group-hover:bg-black/10 active:bg-black/20 transition-all duration-200 rounded-lg flex items-center justify-center ${
                  isImageClicked ? 'bg-black/30' : ''
                }`}>
                  <div className={`opacity-0 group-hover:opacity-100 transition-opacity duration-200 bg-white/90 dark:bg-gray-800/90 px-3 py-2 rounded-lg text-sm font-medium shadow-lg ${
                    isImageClicked ? 'opacity-100 scale-110' : ''
                  }`}>
                    Click to view full size
                  </div>
                </div>
              </div>
              
              {/* Thumbnail navigation if multiple images */}
              {allImages.length > 1 && (
                <div className="flex gap-2 mt-4 overflow-x-auto">
                  {allImages.map((image, index) => (
                    <button
                      key={index}
                      onClick={() => setSelectedImageIndex(index)}
                      className={`flex-shrink-0 w-16 h-16 rounded-lg border-2 overflow-hidden ${
                        index === selectedImageIndex
                          ? 'border-primary'
                          : 'border-gray-200 dark:border-gray-700'
                      }`}
                    >
                      <img
                        src={image}
                        alt={`${product.name} ${index + 1}`}
                        className="w-full h-full object-contain bg-gray-50 dark:bg-gray-800"
                      />
                    </button>
                  ))}
                </div>
              )}
            </div>

            {/* Product Info */}
            <div>
              <h1 className="text-3xl font-bold text-gray-900 dark:text-white mb-4">
                {product.name}
              </h1>
              
              <div className="flex items-center gap-4 mb-6">
                {product.rating > 0 && (
                  <div className="flex items-center">
                    <div className="flex items-center mr-2">
                      {[...Array(5)].map((_, i) => (
                        <Star
                          key={i}
                          size={16}
                          className={i < Math.floor(product.rating) ? 'text-yellow-400 fill-current' : 'text-gray-300 dark:text-gray-600'}
                        />
                      ))}
                    </div>
                    <span className="text-sm text-gray-600 dark:text-gray-400">
                      {product.rating.toFixed(1)} ({product.reviews} reviews)
                    </span>
                  </div>
                )}
              </div>

              <div className="flex gap-3 mb-6">
                {isAuthenticated && (
                  <BookmarkButton product={product} />
                )}
                <button
                  onClick={handleShareClick}
                  className="flex items-center gap-2 px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors"
                >
                  <Share2 size={16} />
                  Share
                </button>
              </div>
            </div>
          </div>

          {/* Tabs */}
          <div className="mt-10">
            <div className="border-b border-gray-200 dark:border-gray-700 mb-4">
              <nav className="-mb-px flex space-x-8">
                <button
                  onClick={() => setActiveTab('description')}
                  className={`py-2 px-1 border-b-2 font-medium text-sm ${
                    activeTab === 'description'
                      ? 'border-primary text-primary'
                      : 'border-transparent text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-300'
                  }`}
                >
                  Description
                </button>
                <button
                  onClick={() => setActiveTab('specifications')}
                  className={`py-2 px-1 border-b-2 font-medium text-sm ${
                    activeTab === 'specifications'
                      ? 'border-primary text-primary'
                      : 'border-transparent text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-300'
                  }`}
                >
                  Specifications
                </button>
              </nav>
            </div>
            
            <div className="text-gray-600 dark:text-gray-300">
              {activeTab === 'description' && (
                <div>
                  {product.description ? (
                    <p className="leading-relaxed">{product.description}</p>
                  ) : (
                    <p className="text-gray-500 dark:text-gray-400 italic">
                      Product description is being loaded...
                    </p>
                  )}
                </div>
              )}
              {activeTab === 'specifications' && (
                <div>
                  {product.specifications && product.specifications.length > 0 ? (
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                      {product.specifications.map((spec, index) => (
                        <div key={index} className="flex justify-between items-center py-2 border-b border-gray-200 dark:border-gray-700">
                          <span className="font-medium">{spec.name}</span>
                          <span>{spec.value}</span>
                        </div>
                      ))}
                    </div>
                  ) : (
                    <p className="text-gray-500 dark:text-gray-400 italic">
                      Product specifications are being loaded...
                    </p>
                  )}
                </div>
              )}
            </div>
          </div>
          
          {/* Price Comparison Table */}
          {hasRetailers && (
            <div className="mt-10">
              <h2 className="text-2xl font-bold mb-6">Price Comparison</h2>
              <PriceComparisonTable retailers={product.retailers} />
            </div>
          )}
        </div>

        {/* Sidebar */}
        <div className="lg:col-span-1">
          <div className="sticky top-6">
            {hasRetailers ? (
              <div className="bg-white dark:bg-gray-800 rounded-xl shadow-lg p-6 border border-gray-200 dark:border-gray-700">
                <div className="text-center mb-6">
                  <p className="text-sm text-gray-500 dark:text-gray-400 mb-2">Best Price</p>
                  <p className="text-3xl font-bold text-primary mb-1">
                    {lowestPrice.toFixed(2)} AED
                  </p>
                  {bestRetailer && (
                    <p className="text-sm text-gray-600 dark:text-gray-400">
                      from {bestRetailer.name}
                    </p>
                  )}
                </div>
                
                {bestRetailer && (
                  <a
                    href={bestRetailer.productUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="w-full btn bg-primary hover:bg-primary/90 text-white flex items-center justify-center gap-2 mb-4 px-4 py-3 rounded-lg font-medium transition-colors"
                  >
                    <ShoppingCart size={18} />
                    Buy Now
                    <ExternalLink size={14} />
                  </a>
                )}
                
                <div className="text-center">
                  <p className="text-xs text-gray-500 dark:text-gray-400">
                    Prices are updated regularly
                  </p>
                </div>
              </div>
            ) : (
              <div className="bg-white dark:bg-gray-800 rounded-xl shadow-lg p-6 border border-gray-200 dark:border-gray-700">
                <div className="text-center">
                  <p className="text-gray-600 dark:text-gray-400 mb-4">
                    We're finding the best prices for you...
                  </p>
                  <div className="w-8 h-8 border-2 border-primary border-t-transparent rounded-full animate-spin mx-auto"></div>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Image Modal */}
      {showImageModal && (
        <div 
          className="fixed inset-0 bg-black/90 z-50 flex items-center justify-center p-4"
          onClick={() => setShowImageModal(false)}
        >
          <div 
            className="relative max-w-4xl max-h-full"
            onClick={(e) => e.stopPropagation()}
          >
            {/* Close button */}
            <button
              onClick={() => setShowImageModal(false)}
              className="absolute top-4 right-4 z-10 bg-black/50 text-white p-2 rounded-full hover:bg-black/70 transition-colors"
              aria-label="Close image viewer"
            >
              <X size={24} />
            </button>

            {/* Navigation arrows for multiple images */}
            {allImages.length > 1 && (
              <>
                <button
                  onClick={() => setSelectedImageIndex(prev => 
                    prev > 0 ? prev - 1 : allImages.length - 1
                  )}
                  className="absolute left-4 top-1/2 transform -translate-y-1/2 bg-black/50 text-white p-2 rounded-full hover:bg-black/70 transition-colors"
                  aria-label="Previous image"
                >
                  <ChevronLeft size={24} />
                </button>
                <button
                  onClick={() => setSelectedImageIndex(prev => 
                    prev < allImages.length - 1 ? prev + 1 : 0
                  )}
                  className="absolute right-4 top-1/2 transform -translate-y-1/2 bg-black/50 text-white p-2 rounded-full hover:bg-black/70 transition-colors"
                  aria-label="Next image"
                >
                  <ChevronRight size={24} />
                </button>
              </>
            )}

            {/* Image */}
            <img
              src={selectedImage || '/placeholder-image.jpg'}
              alt={`${product.name} - Image ${selectedImageIndex + 1}`}
              className="max-w-full max-h-full object-contain"
            />

            {/* Image counter */}
            {allImages.length > 1 && (
              <div className="absolute bottom-4 left-1/2 transform -translate-x-1/2 bg-black/50 text-white px-3 py-1 rounded text-sm">
                {selectedImageIndex + 1} / {allImages.length}
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
};

export default ProductPage;
