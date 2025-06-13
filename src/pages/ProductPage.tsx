import { useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useProducts } from '../context/ProductContext';
import {
  Share2, Star, ChevronRight, ArrowLeft,
  ShoppingCart, Heart, Bell
} from 'lucide-react';
import PriceHistoryChart from '../components/PriceHistoryChart';
import PriceComparisonTable from '../components/PriceComparisonTable';
import AlertForm from '../components/AlertForm';
import { Product } from '../types';
import { useAuth } from '../context/AuthContext';

const ProductPage = () => {
  const { id } = useParams<{ id: string }>();
  const { getProductById, loading, error } = useProducts();
  const [activeTab, setActiveTab] = useState<'description' | 'specifications'>('description');
  const [isFavorite, setIsFavorite] = useState(false);
  const { isAuthenticated } = useAuth();

  const product = id ? getProductById(id) : undefined;

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
  
  // Get best price and retailer
  const lowestPrice = product.retailers && product.retailers.length > 0 
    ? Math.min(...product.retailers.map(retailer => retailer.currentPrice))
    : 0;
  
  const bestRetailer = product.retailers && product.retailers.length > 0
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
      // Fallback
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
      {/* Breadcrumbs */}
      <div className="flex items-center text-sm text-gray-500 mb-6">
        <Link to="/" className="hover:text-primary">Home</Link>
        <ChevronRight size={14} className="mx-2" />
        <Link to="/search" className="hover:text-primary">Search</Link>
        <ChevronRight size={14} className="mx-2" />
        <span className="text-gray-700 font-medium line-clamp-1">
          {product.name}
        </span>
      </div>
      
      {/* Back button */}
      <Link to="/search" className="inline-flex items-center text-secondary hover:text-primary mb-6">
        <ArrowLeft size={18} className="mr-2" />
        Back to search results
      </Link>
      
      {/* Product details */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Left column: Product image */}
        <div className="lg:col-span-1">
          <div className="bg-white rounded-xl shadow-sm p-6 flex items-center justify-center">
            <img 
              src={product.imageUrl} 
              alt={product.name}
              className="max-w-full max-h-80 object-contain" 
            />
          </div>
          
          {/* Image gallery (thumbnails) */}
          <div className="flex justify-center mt-4 space-x-2">
            {[1, 2, 3].map((_, index) => (
              <button 
                key={index}
                className={`w-16 h-16 border rounded-lg overflow-hidden ${index === 0 ? 'border-primary' : 'border-gray-200'}`}
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
        
        {/* Middle column: Product info */}
        <div className="lg:col-span-1">
          <h1 className="text-2xl md:text-3xl font-bold text-secondary mb-4">
            {product.name}
          </h1>
          
          <div className="flex items-center mb-4">
            <div className="flex items-center text-yellow-500">
              {[...Array(5)].map((_, i) => (
                <Star
                  key={i}
                  size={16}
                  fill={i < Math.floor(product.rating) ? "currentColor" : "none"}
                  stroke={i < Math.floor(product.rating) ? "currentColor" : "currentColor"}
                  className="mr-0.5"
                />
              ))}
            </div>
            <span className="ml-2 text-sm text-gray-600">{product.rating} ({product.reviews} reviews)</span>
          </div>
          
          <div className="mb-6">
            <div className="text-sm text-gray-500 mb-1">Best price from:</div>
            <div className="flex items-center justify-between">
              <div className="flex items-center">
                <img 
                  src={bestRetailer?.logo} 
                  alt={bestRetailer?.name}
                  className="h-6 mr-2" 
                />
                <span className="font-medium text-secondary">{bestRetailer?.name}</span>
              </div>
              <div className="text-2xl font-bold text-primary">
                {lowestPrice.toFixed(2)} AED
              </div>
            </div>
          </div>
          
          <div className="flex space-x-3 mb-6">
            <a 
              href={bestRetailer?.productUrl} 
              target="_blank" 
              rel="noopener noreferrer"
              className="btn btn-primary flex-grow flex items-center justify-center"
            >
              <ShoppingCart size={18} className="mr-2" />
              Buy Now
            </a>
            
            <button 
              onClick={handleShareClick}
              className="btn bg-gray-100 text-secondary hover:bg-gray-200 p-3"
              aria-label="Share"
            >
              <Share2 size={18} />
            </button>
            
            {isAuthenticated && (
              <button 
                onClick={toggleFavorite}
                className={`btn p-3 ${
                  isFavorite 
                    ? 'bg-primary/10 text-primary hover:bg-primary/20' 
                    : 'bg-gray-100 text-secondary hover:bg-gray-200'
                }`}
                aria-label="Add to favorites"
              >
                <Heart size={18} fill={isFavorite ? 'currentColor' : 'none'} />
              </button>
            )}
          </div>
          
          {/* Tabs */}
          <div className="border-b border-gray-200 mb-4">
            <div className="flex">
              <button
                onClick={() => setActiveTab('description')}
                className={`py-3 px-4 text-sm font-medium border-b-2 ${
                  activeTab === 'description'
                    ? 'border-primary text-primary'
                    : 'border-transparent text-gray-500 hover:text-gray-700'
                }`}
              >
                Description
              </button>
              <button
                onClick={() => setActiveTab('specifications')}
                className={`py-3 px-4 text-sm font-medium border-b-2 ${
                  activeTab === 'specifications'
                    ? 'border-primary text-primary'
                    : 'border-transparent text-gray-500 hover:text-gray-700'
                }`}
              >
                Specifications
              </button>
            </div>
          </div>
          
          {/* Tab content */}
          <div className="text-gray-600">
            {activeTab === 'description' ? (
              <div className="prose prose-sm max-w-none">
                <p>{product.description}</p>
              </div>
            ) : (
              <div className="space-y-2">
                {product.specifications && product.specifications.map((spec, index) => (
                  <div key={index} className="grid grid-cols-2 py-2 border-b border-gray-100 text-sm">
                    <div className="font-medium text-gray-700">{spec.name}</div>
                    <div>{spec.value}</div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
        
        {/* Right column: Price alert */}
        <div className="lg:col-span-1">
          {isAuthenticated ? (
            <AlertForm product={product} />
          ) : (
            <div className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden p-4">
              <div className="text-center p-4">
                <Bell size={24} className="mx-auto text-primary mb-3" />
                <h3 className="font-semibold text-lg mb-2">Get Price Drop Alerts</h3>
                <p className="text-gray-600 text-sm mb-4">
                  Sign in to set up price alerts and we'll notify you when prices drop.
                </p>
                <Link to="/login" className="btn btn-primary text-sm">
                  Log In to Set Alert
                </Link>
              </div>
            </div>
          )}
          
          <div className="mt-6">
            <PriceComparisonTable retailers={product.retailers} />
          </div>
        </div>
      </div>
      
      {/* Price history chart */}
      <div className="mt-10">
        <PriceHistoryChart retailers={product.retailers} productName={product.name} />
      </div>
      
      {/* Similar products */}
      {/* <div className="mt-12">
        <h2 className="text-2xl font-bold mb-6">Similar Products</h2>
        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-6">
          {mockProductDetails.slice(0, 4).map((similarProduct) => (
            <div key={similarProduct.id} className="card">
              <Link to={`/product/${similarProduct.id}`}>
                <div className="p-4">
                  <img 
                    src={similarProduct.imageUrl} 
                    alt={similarProduct.name}
                    className="w-full h-40 object-contain mb-4" 
                  />
                  <h3 className="font-medium text-secondary line-clamp-2 hover:text-primary transition-colors">
                    {similarProduct.name}
                  </h3>
                  <div className="flex items-center mt-2">
                    <div className="flex items-center text-yellow-500">
                      {[...Array(5)].map((_, i) => (
                        <Star
                          key={i}
                          size={12}
                          fill={i < Math.floor(similarProduct.rating) ? "currentColor" : "none"}
                          stroke={i < Math.floor(similarProduct.rating) ? "currentColor" : "currentColor"}
                          className="mr-0.5"
                        />
                      ))}
                    </div>
                    <span className="ml-1 text-xs text-gray-500">({similarProduct.reviews})</span>
                  </div>
                  <div className="mt-2 font-bold text-primary">
                    {Math.min(...similarProduct.retailers.map(r => r.currentPrice)).toFixed(2)} AED
                  </div>
                </div>
              </Link>
            </div>
          ))}
        </div>
      </div> */}
    </div>
  );
};

export default ProductPage;
