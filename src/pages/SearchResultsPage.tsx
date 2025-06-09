import { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import { 
  Filter, SortAsc, SortDesc, Grid, List, 
  X, ChevronDown, Star, Check
} from 'lucide-react';
import ProductCard from '../components/ProductCard';
import { mockProductDetails } from '../data/mockData';
import { Product } from '../types';

type SortOption = 'price_asc' | 'price_desc' | 'rating' | 'popularity';
type ViewMode = 'grid' | 'list';

const SearchResultsPage = () => {
  const [searchParams] = useSearchParams();
  const query = searchParams.get('q') || '';
  
  const [products, setProducts] = useState<Product[]>([]);
  const [filteredProducts, setFilteredProducts] = useState<Product[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [sortBy, setSortBy] = useState<SortOption>('price_asc');
  const [viewMode, setViewMode] = useState<ViewMode>('grid');
  const [isMobileFilterOpen, setIsMobileFilterOpen] = useState(false);
  
  // Filters
  const [priceRange, setPriceRange] = useState<[number, number]>([0, 5000]);
  const [selectedRetailers, setSelectedRetailers] = useState<string[]>([]);
  const [selectedRating, setSelectedRating] = useState<number | null>(null);
  
  // Get unique retailers from all products
  const allRetailers = [...new Set(
    mockProductDetails.flatMap(product => 
      product.retailers.map(retailer => retailer.name)
    )
  )];
  
  useEffect(() => {
    // Simulate API call with search query
    const fetchProducts = async () => {
      setIsLoading(true);
      try {
        // In a real app, this would be an API call with the query
        // For demo, just filter the mock data if query exists
        let results = [...mockProductDetails];
        
        if (query) {
          results = results.filter(product => 
            product.name.toLowerCase().includes(query.toLowerCase()) ||
            product.description.toLowerCase().includes(query.toLowerCase())
          );
        }
        
        setProducts(results);
        setFilteredProducts(results);
        
        // Set price range based on available products
        if (results.length > 0) {
          const prices = results.flatMap(product => 
            product.retailers.map(retailer => retailer.currentPrice)
          );
          const min = Math.floor(Math.min(...prices));
          const max = Math.ceil(Math.max(...prices));
          setPriceRange([min, max]);
        }
      } catch (error) {
        console.error('Error fetching products:', error);
      } finally {
        setIsLoading(false);
      }
    };
    
    fetchProducts();
  }, [query]);
  
  // Apply filters
  useEffect(() => {
    let results = [...products];
    
    // Filter by price
    results = results.filter(product => {
      const lowestPrice = Math.min(...product.retailers.map(r => r.currentPrice));
      return lowestPrice >= priceRange[0] && lowestPrice <= priceRange[1];
    });
    
    // Filter by retailers
    if (selectedRetailers.length > 0) {
      results = results.filter(product => 
        product.retailers.some(retailer => 
          selectedRetailers.includes(retailer.name)
        )
      );
    }
    
    // Filter by rating
    if (selectedRating !== null) {
      results = results.filter(product => product.rating >= selectedRating);
    }
    
    // Apply sorting
    results = sortProducts(results, sortBy);
    
    setFilteredProducts(results);
  }, [products, sortBy, priceRange, selectedRetailers, selectedRating]);
  
  const sortProducts = (productsToSort: Product[], sortOption: SortOption) => {
    return [...productsToSort].sort((a, b) => {
      if (sortOption === 'price_asc') {
        return (
          Math.min(...a.retailers.map(r => r.currentPrice)) - 
          Math.min(...b.retailers.map(r => r.currentPrice))
        );
      } else if (sortOption === 'price_desc') {
        return (
          Math.min(...b.retailers.map(r => r.currentPrice)) - 
          Math.min(...a.retailers.map(r => r.currentPrice))
        );
      } else if (sortOption === 'rating') {
        return b.rating - a.rating;
      } else {
        // popularity (by number of reviews)
        return b.reviews - a.reviews;
      }
    });
  };
  
  const handleRetailerToggle = (retailer: string) => {
    setSelectedRetailers(prev => {
      if (prev.includes(retailer)) {
        return prev.filter(r => r !== retailer);
      } else {
        return [...prev, retailer];
      }
    });
  };
  
  const handleRatingSelect = (rating: number) => {
    setSelectedRating(prev => prev === rating ? null : rating);
  };
  
  const clearAllFilters = () => {
    setPriceRange([0, 5000]);
    setSelectedRetailers([]);
    setSelectedRating(null);
  };
  
  const toggleMobileFilter = () => {
    setIsMobileFilterOpen(!isMobileFilterOpen);
  };
  
  if (isLoading) {
    return (
      <div className="container mx-auto px-4 py-10 min-h-screen flex items-center justify-center">
        <div className="w-12 h-12 border-4 border-primary border-t-transparent rounded-full animate-spin"></div>
      </div>
    );
  }
  
  return (
    <div className="container mx-auto px-4 py-8">
      <div className="mb-6">
        <h1 className="text-3xl font-bold text-secondary">
          {query ? `Search Results for "${query}"` : 'All Products'}
        </h1>
        <p className="text-gray-600 mt-2">
          Found {filteredProducts.length} products
        </p>
      </div>
      
      <div className="flex flex-col lg:flex-row gap-8">
        {/* Filters - Desktop */}
        <div className="hidden lg:block w-64 flex-shrink-0">
          <div className="bg-white rounded-xl shadow-sm p-5">
            <div className="flex items-center justify-between mb-6">
              <h2 className="font-semibold text-lg">Filters</h2>
              <button 
                onClick={clearAllFilters}
                className="text-primary text-sm hover:underline"
              >
                Clear all
              </button>
            </div>
            
            {/* Price Range Filter */}
            <div className="mb-6">
              <h3 className="font-medium mb-3">Price Range</h3>
              <div className="flex justify-between mb-2">
                <span className="text-sm text-gray-600">{priceRange[0]} AED</span>
                <span className="text-sm text-gray-600">{priceRange[1]} AED</span>
              </div>
              <input
                type="range"
                min="0"
                max="5000"
                value={priceRange[1]}
                onChange={(e) => setPriceRange([priceRange[0], parseInt(e.target.value)])}
                className="w-full"
              />
            </div>
            
            {/* Retailers Filter */}
            <div className="mb-6">
              <h3 className="font-medium mb-3">Retailers</h3>
              <div className="space-y-2">
                {allRetailers.map((retailer) => (
                  <label key={retailer} className="flex items-center">
                    <input
                      type="checkbox"
                      checked={selectedRetailers.includes(retailer)}
                      onChange={() => handleRetailerToggle(retailer)}
                      className="rounded text-primary focus:ring-primary"
                    />
                    <span className="ml-2 text-sm">{retailer}</span>
                  </label>
                ))}
              </div>
            </div>
            
            {/* Rating Filter */}
            <div>
              <h3 className="font-medium mb-3">Rating</h3>
              <div className="space-y-2">
                {[4, 3, 2, 1].map((rating) => (
                  <button
                    key={rating}
                    onClick={() => handleRatingSelect(rating)}
                    className={`flex items-center w-full py-1 px-2 rounded ${
                      selectedRating === rating ? 'bg-primary/10 text-primary' : ''
                    }`}
                  >
                    <div className="flex items-center text-yellow-500">
                      {[...Array(5)].map((_, i) => (
                        <Star
                          key={i}
                          size={14}
                          fill={i < rating ? "currentColor" : "none"}
                          stroke={i < rating ? "currentColor" : "currentColor"}
                        />
                      ))}
                    </div>
                    <span className="ml-2 text-sm">& Up</span>
                    {selectedRating === rating && (
                      <Check size={14} className="ml-auto text-primary" />
                    )}
                  </button>
                ))}
              </div>
            </div>
          </div>
        </div>
        
        {/* Main Content */}
        <div className="flex-grow">
          {/* Toolbar */}
          <div className="bg-white rounded-xl shadow-sm p-4 mb-6 flex flex-wrap items-center justify-between gap-4">
            <div className="flex items-center">
              <button
                onClick={toggleMobileFilter}
                className="lg:hidden flex items-center text-secondary mr-4"
              >
                <Filter size={18} className="mr-1" />
                Filters
              </button>
              
              <div className="relative">
                <select
                  value={sortBy}
                  onChange={(e) => setSortBy(e.target.value as SortOption)}
                  className="appearance-none bg-gray-100 rounded-lg px-4 py-2 pr-8 text-sm font-medium focus:outline-none"
                >
                  <option value="price_asc">Price: Low to High</option>
                  <option value="price_desc">Price: High to Low</option>
                  <option value="rating">Rating</option>
                  <option value="popularity">Popularity</option>
                </select>
                <ChevronDown size={14} className="absolute right-3 top-1/2 transform -translate-y-1/2 pointer-events-none text-gray-500" />
              </div>
            </div>
            
            <div className="flex items-center space-x-2">
              <button
                onClick={() => setViewMode('grid')}
                className={`p-2 rounded ${viewMode === 'grid' ? 'bg-gray-100 text-primary' : 'text-gray-500'}`}
              >
                <Grid size={18} />
              </button>
              <button
                onClick={() => setViewMode('list')}
                className={`p-2 rounded ${viewMode === 'list' ? 'bg-gray-100 text-primary' : 'text-gray-500'}`}
              >
                <List size={18} />
              </button>
            </div>
          </div>
          
          {/* Applied Filters */}
          {(selectedRetailers.length > 0 || selectedRating !== null) && (
            <div className="flex flex-wrap gap-2 mb-6">
              {selectedRetailers.map(retailer => (
                <div key={retailer} className="bg-gray-100 rounded-full px-3 py-1 text-sm flex items-center">
                  {retailer}
                  <button 
                    onClick={() => handleRetailerToggle(retailer)}
                    className="ml-2 text-gray-500 hover:text-gray-700"
                  >
                    <X size={14} />
                  </button>
                </div>
              ))}
              
              {selectedRating !== null && (
                <div className="bg-gray-100 rounded-full px-3 py-1 text-sm flex items-center">
                  {selectedRating}+ Stars
                  <button 
                    onClick={() => setSelectedRating(null)}
                    className="ml-2 text-gray-500 hover:text-gray-700"
                  >
                    <X size={14} />
                  </button>
                </div>
              )}
              
              <button 
                onClick={clearAllFilters}
                className="text-primary text-sm hover:underline flex items-center"
              >
                Clear all
              </button>
            </div>
          )}
          
          {/* Products Grid */}
          {filteredProducts.length === 0 ? (
            <div className="bg-white rounded-xl shadow-sm p-8 text-center">
              <h3 className="text-xl font-semibold mb-2">No products found</h3>
              <p className="text-gray-600 mb-4">
                Try adjusting your filters or search term to find what you're looking for.
              </p>
              <button 
                onClick={clearAllFilters}
                className="btn btn-primary"
              >
                Clear Filters
              </button>
            </div>
          ) : (
            <div className={`
              ${viewMode === 'grid' 
                ? 'grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-2 xl:grid-cols-3 gap-6' 
                : 'space-y-6'
              }
            `}>
              {filteredProducts.map((product) => (
                <ProductCard key={product.id} product={product} />
              ))}
            </div>
          )}
        </div>
      </div>
      
      {/* Mobile Filters */}
      <div className={`
        fixed inset-0 bg-black/50 z-50 transition-opacity duration-300
        ${isMobileFilterOpen ? 'opacity-100' : 'opacity-0 pointer-events-none'}
      `}>
        <div className={`
          fixed bottom-0 left-0 right-0 bg-white rounded-t-xl p-5 transition-transform duration-300 transform
          ${isMobileFilterOpen ? 'translate-y-0' : 'translate-y-full'}
        `}>
          <div className="flex items-center justify-between mb-6">
            <h2 className="font-semibold text-lg">Filters</h2>
            <div className="flex items-center">
              <button 
                onClick={clearAllFilters}
                className="text-primary text-sm hover:underline mr-4"
              >
                Clear all
              </button>
              <button onClick={toggleMobileFilter}>
                <X size={20} />
              </button>
            </div>
          </div>
          
          <div className="max-h-[calc(80vh-6rem)] overflow-y-auto pb-20">
            {/* Price Range Filter */}
            <div className="mb-6">
              <h3 className="font-medium mb-3">Price Range</h3>
              <div className="flex justify-between mb-2">
                <span className="text-sm text-gray-600">{priceRange[0]} AED</span>
                <span className="text-sm text-gray-600">{priceRange[1]} AED</span>
              </div>
              <input
                type="range"
                min="0"
                max="5000"
                value={priceRange[1]}
                onChange={(e) => setPriceRange([priceRange[0], parseInt(e.target.value)])}
                className="w-full"
              />
            </div>
            
            {/* Retailers Filter */}
            <div className="mb-6">
              <h3 className="font-medium mb-3">Retailers</h3>
              <div className="grid grid-cols-2 gap-2">
                {allRetailers.map((retailer) => (
                  <label key={retailer} className="flex items-center">
                    <input
                      type="checkbox"
                      checked={selectedRetailers.includes(retailer)}
                      onChange={() => handleRetailerToggle(retailer)}
                      className="rounded text-primary focus:ring-primary"
                    />
                    <span className="ml-2 text-sm">{retailer}</span>
                  </label>
                ))}
              </div>
            </div>
            
            {/* Rating Filter */}
            <div>
              <h3 className="font-medium mb-3">Rating</h3>
              <div className="space-y-2">
                {[4, 3, 2, 1].map((rating) => (
                  <button
                    key={rating}
                    onClick={() => handleRatingSelect(rating)}
                    className={`flex items-center w-full py-1 px-2 rounded ${
                      selectedRating === rating ? 'bg-primary/10 text-primary' : ''
                    }`}
                  >
                    <div className="flex items-center text-yellow-500">
                      {[...Array(5)].map((_, i) => (
                        <Star
                          key={i}
                          size={14}
                          fill={i < rating ? "currentColor" : "none"}
                          stroke={i < rating ? "currentColor" : "currentColor"}
                        />
                      ))}
                    </div>
                    <span className="ml-2 text-sm">& Up</span>
                    {selectedRating === rating && (
                      <Check size={14} className="ml-auto text-primary" />
                    )}
                  </button>
                ))}
              </div>
            </div>
          </div>
          
          <div className="fixed bottom-0 left-0 right-0 p-4 bg-white border-t">
            <button 
              onClick={toggleMobileFilter}
              className="btn btn-primary w-full"
            >
              Apply Filters
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default SearchResultsPage;