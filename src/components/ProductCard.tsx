import { Link } from 'react-router-dom';
import { ChevronRight, Star } from 'lucide-react';
import { Product } from '../types';
import { useAuth } from '../context/AuthContext';
import BookmarkButton from './BookmarkButton';

interface ProductCardProps {
  product: Product;
}

const ProductCard = ({ product }: ProductCardProps) => {
  const { isAuthenticated } = useAuth();
  
  const lowestPrice = product.retailers && product.retailers.length > 0 
    ? Math.min(...product.retailers.map(r => r.currentPrice))
    : (product.price ? parseFloat(String(product.price).replace(/[^\\d.]/g, '')) : Infinity);

  const bestRetailer = product.retailers && product.retailers.length > 0
    ? product.retailers.find(r => r.currentPrice === lowestPrice)
    : undefined;
  
  
  return (
    <div className="card group h-full flex flex-col transition-smooth hover:scale-[1.02] hover:shadow-xl">
      {/* Image and alert button */}
      <div className="relative overflow-hidden">
        <Link to={`/product/${product.id}`}>
          <img
            src={product.imageUrl}
            alt={product.name}
            className="w-full h-48 object-contain p-4 transition-transform group-hover:scale-105"
          />
        </Link>
        
        {isAuthenticated && (
          <div className="absolute top-3 right-3">
            <BookmarkButton product={product} />
          </div>
        )}
      </div>
      
      {/* Content */}
      <div className="p-4 flex-1 flex flex-col">
        <Link to={`/product/${product.id}`}>
          <h3 className="font-medium text-lg line-clamp-2 mb-2 hover:text-primary transition-colors">
            {product.name}
          </h3>
        </Link>
        
        <div className="flex items-center mb-3">
          <div className="flex items-center text-yellow-500">
            {[...Array(5)].map((_, i) => (
              <Star
                key={i}
                size={14}
                fill={i < Math.floor(product.rating) ? "currentColor" : "none"}
                stroke={i < Math.floor(product.rating) ? "currentColor" : "currentColor"}
                className="mr-0.5"
              />
            ))}
          </div>
          <span className="ml-1 text-sm text-gray-500">({product.reviews})</span>
        </div>
        
        {/* Best price section */}
        <div className="mb-4 flex-1" style={{ minHeight: '52px' }}>
        {isFinite(lowestPrice) ? (
          bestRetailer ? (
            <>
              <div className="text-sm text-gray-500 mb-1">Best price from:</div>
              <div className="flex items-center justify-between">
                <div className="flex items-center">
                  <img
                    src={bestRetailer.logo}
                    alt={bestRetailer.name}
                    className="h-5 mr-2 dark:invert"
                  />
                  <span className="font-semibold text-secondary dark:text-gray-300">{bestRetailer.name}</span>
                </div>
                <div className="text-xl font-bold text-primary">
                  {lowestPrice.toFixed(2)} {product.currency === 'AE' ? 'AED' : product.currency}
                </div>
              </div>
            </>
          ) : (
            <>
              <div className="text-sm text-gray-500 mb-1">Price:</div>
              <div className="text-xl font-bold text-primary">
                {lowestPrice.toFixed(2)} {product.currency === 'AE' ? 'AED' : product.currency}
              </div>
            </>
          )
        ) : (
          <div className="flex items-center h-full">
            <p className="text-sm text-gray-500">No price information available.</p>
          </div>
        )}
        </div>
        
        {/* View details link */}
        <div className="mt-auto">
          <Link 
            to={`/product/${product.id}`}
            className="inline-flex items-center text-sm font-medium text-primary hover:underline transition-smooth hover:text-primary/80 group-hover:translate-x-1"
          >
            View full comparison
            <ChevronRight size={16} className="ml-1 transition-smooth group-hover:translate-x-1" />
          </Link>
        </div>
      </div>
    </div>
  );
};

export default ProductCard;
