import { useState, useEffect, useMemo, useCallback, useRef } from 'react';
import { useSearchParams } from 'react-router-dom';
import { 
  Filter, Grid, List, 
  X, ChevronDown, Star, Check, SortAsc, SortDesc, TrendingUp, Zap
} from 'lucide-react';
import ProductCard from '../components/ProductCard';
import { Product } from '../types';

type SortOption = 'price_asc' | 'price_desc' | 'rating' | 'popularity' | 'relevance';
type ViewMode = 'grid' | 'list';

interface SortOptionConfig {
  value: SortOption;
  label: string;
  icon: React.ReactNode;
  description: string;
}

const SearchResultsPage = () => {
  const [searchParams] = useSearchParams();
  const query = searchParams.get('q') || '';
  
  const [allProducts, setAllProducts] = useState<Product[]>([]);
  const [productsLoading, setProductsLoading] = useState(true);
  const [productsError, setProductsError] = useState<string | null>(null);
  const [filteredProducts, setFilteredProducts] = useState<Product[]>([]);
  const [sortBy, setSortBy] = useState<SortOption>(query ? 'relevance' : 'price_asc'); // Use relevance for search
  const [viewMode, setViewMode] = useState<ViewMode>('grid');
  const [isMobileFilterOpen, setIsMobileFilterOpen] = useState(false);
  const [isSortDropdownOpen, setIsSortDropdownOpen] = useState(false);
  
  // Filters
  const [priceRange, setPriceRange] = useState<[number, number]>([0, 5000]);
  const [tempPriceRange, setTempPriceRange] = useState<[number, number]>([0, 5000]);
  const [selectedRating, setSelectedRating] = useState<number | null>(null);

  // Sort options configuration
  const sortOptions: SortOptionConfig[] = useMemo(() => [
    ...(query ? [{
      value: 'relevance' as SortOption,
      label: 'Relevance',
      icon: <Zap size={16} className="text-orange-500" />,
      description: 'Most relevant results'
    }] : []),
    {
      value: 'price_asc' as SortOption,
      label: 'Price: Low to High',
      icon: <SortAsc size={16} className="text-green-500" />,
      description: 'Cheapest first'
    },
    {
      value: 'price_desc' as SortOption,
      label: 'Price: High to Low',
      icon: <SortDesc size={16} className="text-red-500" />,
      description: 'Most expensive first'
    },
    {
      value: 'rating' as SortOption,
      label: 'Rating',
      icon: <Star size={16} className="text-yellow-500" />,
      description: 'Highest rated first'
    },
    {
      value: 'popularity' as SortOption,
      label: 'Popularity',
      icon: <TrendingUp size={16} className="text-blue-500" />,
      description: 'Most popular first'
    }
  ], [query]);

  const currentSortOption = sortOptions.find(option => option.value === sortBy);

  // Reset sort to relevance when search query changes
  useEffect(() => {
    if (query) {
      setSortBy('relevance');
    } else {
      setSortBy('price_asc');
    }
  }, [query]);

  useEffect(() => {
    const fetchSearchResults = async () => {
      setProductsLoading(true);
      setProductsError(null);
      try {
        const url = query ? `/api/v1/products/search?q=${encodeURIComponent(query)}` : '/api/v1/products';
        const response = await fetch(url);
        if (!response.ok) {
          throw new Error('Failed to fetch products');
        }
        const data: Product[] = await response.json();
        setAllProducts(data);

        if (data.length > 0) {
            const prices = data
              .flatMap(p => (p.retailers || []).map(r => r.currentPrice))
              .filter(price => typeof price === 'number' && isFinite(price));

            if (prices.length > 0) {
              const minPrice = Math.floor(Math.min(...prices));
              const maxPrice = Math.ceil(Math.max(...prices));
              setPriceRange([minPrice, maxPrice]);
            } else {
              setPriceRange([0, 5000]);
            }
          } else {
            setPriceRange([0, 5000]);
          }
        } catch (error) {
          setProductsError(error instanceof Error ? error.message : 'An error occurred');
        } finally {
          setProductsLoading(false);
        }
      };

    fetchSearchResults();
  }, [query]);
  
  const sortProducts = useCallback((productsToSort: Product[], sortOption: SortOption) => {
    return [...productsToSort].sort((a, b) => {
      const getMinPrice = (p: Product) => {
        const prices = (p.retailers || []).map(r => r.currentPrice).filter(price => price != null);
        if (prices.length === 0) return sortOption === 'price_asc' ? Infinity : -Infinity;
        return Math.min(...prices);
      };

      if (sortOption === 'price_asc') {
        return getMinPrice(a) - getMinPrice(b);
      } else if (sortOption === 'price_desc') {
        return getMinPrice(b) - getMinPrice(a);
      } else if (sortOption === 'rating') {
        return b.rating - a.rating;
      } else if (sortOption === 'popularity') {
        return b.reviews - a.reviews;
      } else { // relevance - maintain backend order
        return 0; // Don't re-sort, backend already sorted by relevance
      }
    });
  }, []);

  useEffect(() => {
    let results = [...allProducts];
    
    results = results.filter(product => {
      const productPrices = (product.retailers || []).map(r => r.currentPrice).filter(p => p != null);
      if (productPrices.length === 0) {
        return true;
      }
      const lowestPrice = Math.min(...productPrices);
      return lowestPrice >= priceRange[0] && lowestPrice <= priceRange[1];
    });
    
    if (selectedRating !== null) {
      results = results.filter(product => product.rating >= selectedRating);
    }
    
    const sortedResults = sortProducts(results, sortBy);
    
    setFilteredProducts(sortedResults);
  }, [allProducts, sortBy, priceRange, selectedRating, sortProducts]);
  
  const handleRatingSelect = (rating: number) => {
    setSelectedRating(prev => prev === rating ? null : rating);
  };
  
  const clearAllFilters = () => {
    setPriceRange([0, 5000]);
    setTempPriceRange([0, 5000]);
    setSelectedRating(null);
  };
  
  const toggleMobileFilter = () => {
    setIsMobileFilterOpen(!isMobileFilterOpen);
  };
  
  // Debounced price range update for smoother performance
  const debounceTimeoutRef = useRef<NodeJS.Timeout | null>(null);
  
  const updatePriceRangeDebounced = useCallback((newRange: [number, number]) => {
    setTempPriceRange(newRange);
    
    if (debounceTimeoutRef.current) {
      clearTimeout(debounceTimeoutRef.current);
    }
    
    debounceTimeoutRef.current = setTimeout(() => {
      setPriceRange(newRange);
    }, 150); // 150ms debounce for smooth dragging
  }, []);

  // Sync temp range with actual range when filters reset
  useEffect(() => {
    setTempPriceRange(priceRange);
  }, [priceRange]);

  // Cleanup timeout on unmount
  useEffect(() => {
    return () => {
      if (debounceTimeoutRef.current) {
        clearTimeout(debounceTimeoutRef.current);
      }
    };
  }, []);

  // Close dropdowns when clicking outside
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      const target = event.target as Element;
      // Only close if clicking outside the dropdown container
      if (!target.closest('[data-dropdown="sort"]')) {
        setIsSortDropdownOpen(false);
      }
    };
    
    if (isSortDropdownOpen) {
      document.addEventListener('click', handleClickOutside);
      return () => document.removeEventListener('click', handleClickOutside);
    }
  }, [isSortDropdownOpen]);
  
  if (productsLoading) {
    return (
      <div className="container mx-auto px-4 py-10 min-h-screen flex items-center justify-center">
        <div className="w-12 h-12 border-4 border-primary border-t-transparent rounded-full animate-spin"></div>
      </div>
    );
  }

  if (productsError) {
    return (
      <div className="container mx-auto px-4 py-10 min-h-screen flex items-center justify-center">
        <div className="text-center text-red-500">
          <h2 className="text-2xl font-bold mb-4">Error</h2>
          <p>{productsError}</p>
        </div>
      </div>
    );
  }
  
  return (
    <div className="container mx-auto px-4 py-8">
      <div className="mb-6">
        <h1 className="text-3xl font-bold text-secondary">
          {query ? `Search Results for "${query}"` : 'All Products'}
        </h1>
        <p className="text-gray-600 dark:text-gray-400 mt-2">
          Found {filteredProducts.length} products
        </p>
      </div>
      
      <div className="flex flex-col lg:flex-row gap-8">
        <aside className="hidden lg:block w-64 flex-shrink-0">
          <div className="bg-white dark:bg-gray-800 rounded-xl shadow-sm p-5">
            <div className="flex items-center justify-between mb-6">
              <h2 className="font-semibold text-lg dark:text-white">Filters</h2>
              <button onClick={clearAllFilters} className="text-primary text-sm hover:underline">
                Clear all
              </button>
            </div>
            
            {/* Enhanced Price Range Filter */}
            <div className="mb-6">
              <h3 className="font-medium mb-4 dark:text-white flex items-center">
                <span className="w-3 h-3 bg-primary rounded-full mr-2"></span>
                Price Range
              </h3>
              
              {/* Price Display Cards */}
              <div className="grid grid-cols-2 gap-3 mb-4">
                <div className="bg-gray-50 dark:bg-gray-700 rounded-lg p-3 text-center">
                  <p className="text-xs text-gray-500 dark:text-gray-400 mb-1">From</p>
                  <p className="font-semibold text-primary">{tempPriceRange[0]} AED</p>
                </div>
                <div className="bg-gray-50 dark:bg-gray-700 rounded-lg p-3 text-center">
                  <p className="text-xs text-gray-500 dark:text-gray-400 mb-1">To</p>
                  <p className="font-semibold text-primary">{tempPriceRange[1]} AED</p>
                </div>
              </div>
              
              {/* Styled Range Sliders */}
              <div className="space-y-4">
                <div className="relative">
                  <label className="text-xs text-gray-500 dark:text-gray-400 mb-2 block">Minimum Price</label>
                  <input
                    type="range"
                    min="0"
                    max="5000"
                    value={tempPriceRange[0]}
                    onChange={(e) => updatePriceRangeDebounced([parseInt(e.target.value), tempPriceRange[1]])}
                    className="w-full h-2 bg-gray-200 dark:bg-gray-600 rounded-lg appearance-none cursor-pointer accent-primary slider-thumb"
                    style={{
                      background: `linear-gradient(to right, #FF4B6E 0%, #FF4B6E ${(tempPriceRange[0] / 5000) * 100}%, #e5e7eb ${(tempPriceRange[0] / 5000) * 100}%, #e5e7eb 100%)`,
                      transition: 'none'
                    }}
                  />
                </div>
                <div className="relative">
                  <label className="text-xs text-gray-500 dark:text-gray-400 mb-2 block">Maximum Price</label>
                  <input
                    type="range"
                    min="0"
                    max="5000"
                    value={tempPriceRange[1]}
                    onChange={(e) => updatePriceRangeDebounced([tempPriceRange[0], parseInt(e.target.value)])}
                    className="w-full h-2 bg-gray-200 dark:bg-gray-600 rounded-lg appearance-none cursor-pointer accent-primary slider-thumb"
                    style={{
                      background: `linear-gradient(to right, #FF4B6E 0%, #FF4B6E ${(tempPriceRange[1] / 5000) * 100}%, #e5e7eb ${(tempPriceRange[1] / 5000) * 100}%, #e5e7eb 100%)`,
                      transition: 'none'
                    }}
                  />
                </div>
              </div>
              
              {/* Quick Price Preset Buttons */}
              <div className="mt-4 grid grid-cols-2 gap-2">
                <button
                  onClick={() => setPriceRange([0, 500])}
                  className="text-xs py-2 px-3 bg-primary/10 text-primary rounded-lg hover:bg-primary/20 transition-colors"
                >
                  Under 500
                </button>
                <button
                  onClick={() => setPriceRange([500, 1500])}
                  className="text-xs py-2 px-3 bg-primary/10 text-primary rounded-lg hover:bg-primary/20 transition-colors"
                >
                  500 - 1500
                </button>
                <button
                  onClick={() => setPriceRange([1500, 3000])}
                  className="text-xs py-2 px-3 bg-primary/10 text-primary rounded-lg hover:bg-primary/20 transition-colors"
                >
                  1500 - 3000
                </button>
                <button
                  onClick={() => setPriceRange([3000, 5000])}
                  className="text-xs py-2 px-3 bg-primary/10 text-primary rounded-lg hover:bg-primary/20 transition-colors"
                >
                  Over 3000
                </button>
              </div>
            </div>
            
            <div>
              <h3 className="font-medium mb-3 dark:text-white flex items-center">
                <span className="w-3 h-3 bg-yellow-500 rounded-full mr-2"></span>
                Rating
              </h3>
              <div className="space-y-2">
                {[4, 3, 2, 1].map((rating) => (
                  <button
                    key={rating}
                    onClick={() => handleRatingSelect(rating)}
                    className={`flex items-center w-full py-2 px-3 rounded-lg transition-all duration-200 ${
                      selectedRating === rating 
                        ? 'bg-primary/10 text-primary border border-primary/20' 
                        : 'hover:bg-gray-50 dark:hover:bg-gray-700'
                    }`}
                  >
                    <div className="flex items-center text-yellow-500">
                      {[...Array(5)].map((_, i) => (
                        <Star key={i} size={14} fill={i < rating ? "currentColor" : "none"} stroke="currentColor" />
                      ))}
                    </div>
                    <span className="ml-2 text-sm">& Up</span>
                    {selectedRating === rating && <Check size={14} className="ml-auto text-primary" />}
                  </button>
                ))}
              </div>
            </div>
          </div>
        </aside>
        
        <main className="flex-grow">
          <div className="bg-white dark:bg-gray-800 rounded-xl shadow-sm p-4 mb-6">
            {/* Mobile and Desktop Controls */}
            <div className="flex flex-col gap-4">
              {/* Mobile Layout - All controls in one row */}
              <div className="flex items-center justify-between gap-2 sm:hidden">
                {/* Left side - Filter and Sort buttons */}
                <div className="flex items-center gap-2 flex-1 min-w-0">
                  <button 
                    onClick={toggleMobileFilter} 
                    className="flex items-center bg-white dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-lg px-3 py-2 text-sm font-medium hover:bg-gray-50 dark:hover:bg-gray-600 transition-all duration-200 shadow-sm hover:shadow-md flex-shrink-0"
                  >
                    <Filter size={16} className="mr-1 text-primary" />
                    <span className="hidden xs:inline">Filters</span>
                  </button>
                  
                  <div className="relative flex-1 min-w-0" data-dropdown="sort">
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        setIsSortDropdownOpen(!isSortDropdownOpen);
                      }}
                      className="flex items-center bg-white dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-lg px-2 py-2 text-sm font-medium hover:bg-gray-50 dark:hover:bg-gray-600 transition-all duration-200 w-full shadow-sm hover:shadow-md"
                    >
                      {currentSortOption?.icon}
                      <span className="ml-1 flex-grow text-left truncate text-xs sm:text-sm">{currentSortOption?.label}</span>
                      <ChevronDown 
                        size={14} 
                        className={`ml-1 transition-transform duration-200 flex-shrink-0 ${isSortDropdownOpen ? 'rotate-180' : ''}`} 
                      />
                    </button>
                    
                    {isSortDropdownOpen && (
                      <>
                        {/* Backdrop */}
                        <div 
                          className="fixed inset-0 z-40" 
                          onClick={(e) => {
                            e.stopPropagation();
                            setIsSortDropdownOpen(false);
                          }}
                        />
                        
                        {/* Mobile-optimized Dropdown */}
                        <div className="absolute left-0 right-0 mt-2 bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-xl shadow-xl z-50 overflow-hidden animate-fadeIn max-w-sm">
                          <div className="py-2">
                            {sortOptions.map((option) => (
                              <button
                                key={option.value}
                                onClick={(e) => {
                                  e.stopPropagation();
                                  setSortBy(option.value);
                                  setIsSortDropdownOpen(false);
                                }}
                                className={`w-full px-3 py-2.5 text-left hover:bg-gray-50 dark:hover:bg-gray-700 transition-all duration-200 flex items-center group ${
                                  sortBy === option.value ? 'bg-primary/5 border-r-2 border-primary' : ''
                                }`}
                              >
                                <div className="flex items-center justify-center w-6 h-6 rounded-lg bg-gray-100 dark:bg-gray-700 group-hover:scale-110 transition-transform duration-200 flex-shrink-0">
                                  {option.icon}
                                </div>
                                <div className="ml-2 flex-grow min-w-0">
                                  <div className={`font-medium text-sm ${sortBy === option.value ? 'text-primary' : 'text-gray-900 dark:text-white'} truncate`}>
                                    {option.label}
                                  </div>
                                  <div className="text-xs text-gray-500 dark:text-gray-400 truncate">
                                    {option.description}
                                  </div>
                                </div>
                                {sortBy === option.value && (
                                  <Check size={14} className="text-primary ml-2 flex-shrink-0" />
                                )}
                              </button>
                            ))}
                          </div>
                        </div>
                      </>
                    )}
                  </div>
                </div>
                
                {/* Right side - View mode controls with proper containment */}
                <div className="flex items-center bg-gray-100 dark:bg-gray-700 rounded-lg p-1 border border-gray-200 dark:border-gray-600 flex-shrink-0">
                  <button 
                    onClick={() => setViewMode('grid')} 
                    className={`p-1.5 rounded-md transition-all duration-200 ${
                      viewMode === 'grid' 
                        ? 'bg-white dark:bg-gray-600 text-primary shadow-sm' 
                        : 'text-gray-500 hover:text-gray-700 dark:hover:text-gray-300'
                    }`}
                  >
                    <Grid size={14} />
                  </button>
                  <button 
                    onClick={() => setViewMode('list')} 
                    className={`p-1.5 rounded-md transition-all duration-200 ${
                      viewMode === 'list' 
                        ? 'bg-white dark:bg-gray-600 text-primary shadow-sm' 
                        : 'text-gray-500 hover:text-gray-700 dark:hover:text-gray-300'
                    }`}
                  >
                    <List size={14} />
                  </button>
                </div>
              </div>
              
              {/* Desktop Layout - Original layout */}
              <div className="hidden sm:flex sm:items-center justify-between">
                {/* Left side controls */}
                <div className="flex items-center gap-3">
                  <div className="relative" data-dropdown="sort">
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        setIsSortDropdownOpen(!isSortDropdownOpen);
                      }}
                      className="flex items-center bg-white dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-lg px-4 py-2.5 text-sm font-medium hover:bg-gray-50 dark:hover:bg-gray-600 transition-all duration-200 min-w-[200px] shadow-sm hover:shadow-md"
                    >
                      {currentSortOption?.icon}
                      <span className="ml-2 flex-grow text-left">{currentSortOption?.label}</span>
                      <ChevronDown 
                        size={16} 
                        className={`ml-2 transition-transform duration-200 ${isSortDropdownOpen ? 'rotate-180' : ''}`} 
                      />
                    </button>
                    
                    {isSortDropdownOpen && (
                      <>
                        {/* Backdrop */}
                        <div 
                          className="fixed inset-0 z-40" 
                          onClick={(e) => {
                            e.stopPropagation();
                            setIsSortDropdownOpen(false);
                          }}
                        />
                        
                        {/* Dropdown */}
                        <div className="absolute left-0 mt-2 w-72 bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-xl shadow-xl z-50 overflow-hidden animate-fadeIn">
                          <div className="py-2">
                            {sortOptions.map((option) => (
                              <button
                                key={option.value}
                                onClick={(e) => {
                                  e.stopPropagation();
                                  setSortBy(option.value);
                                  setIsSortDropdownOpen(false);
                                }}
                                className={`w-full px-4 py-3 text-left hover:bg-gray-50 dark:hover:bg-gray-700 transition-all duration-200 flex items-center group ${
                                  sortBy === option.value ? 'bg-primary/5 border-r-2 border-primary' : ''
                                }`}
                              >
                                <div className="flex items-center justify-center w-8 h-8 rounded-lg bg-gray-100 dark:bg-gray-700 group-hover:scale-110 transition-transform duration-200">
                                  {option.icon}
                                </div>
                                <div className="ml-3 flex-grow">
                                  <div className={`font-medium text-sm ${sortBy === option.value ? 'text-primary' : 'text-gray-900 dark:text-white'}`}>
                                    {option.label}
                                  </div>
                                  <div className="text-xs text-gray-500 dark:text-gray-400 mt-0.5">
                                    {option.description}
                                  </div>
                                </div>
                                {sortBy === option.value && (
                                  <Check size={16} className="text-primary ml-2" />
                                )}
                              </button>
                            ))}
                          </div>
                        </div>
                      </>
                    )}
                  </div>
                </div>
                
                {/* Right side - View mode controls */}
                <div className="flex items-center bg-gray-100 dark:bg-gray-700 rounded-lg p-1 border border-gray-200 dark:border-gray-600">
                  <button 
                    onClick={() => setViewMode('grid')} 
                    className={`p-2 rounded-md transition-all duration-200 ${
                      viewMode === 'grid' 
                        ? 'bg-white dark:bg-gray-600 text-primary shadow-sm' 
                        : 'text-gray-500 hover:text-gray-700 dark:hover:text-gray-300'
                    }`}
                  >
                    <Grid size={16} />
                  </button>
                  <button 
                    onClick={() => setViewMode('list')} 
                    className={`p-2 rounded-md transition-all duration-200 ${
                      viewMode === 'list' 
                        ? 'bg-white dark:bg-gray-600 text-primary shadow-sm' 
                        : 'text-gray-500 hover:text-gray-700 dark:hover:text-gray-300'
                    }`}
                  >
                    <List size={16} />
                  </button>
                </div>
              </div>
            </div>
          </div>
          
          {selectedRating !== null && (
            <div className="flex flex-wrap gap-2 mb-6">
              <div className="bg-gradient-to-r from-primary/10 to-primary/5 border border-primary/20 rounded-full px-4 py-2 text-sm flex items-center">
                <span className="w-2 h-2 bg-yellow-500 rounded-full mr-2"></span>
                {selectedRating}+ Stars
                <button onClick={() => setSelectedRating(null)} className="ml-2 text-primary hover:text-primary/80 transition-colors">
                  <X size={14} />
                </button>
              </div>
              <button onClick={clearAllFilters} className="text-primary text-sm hover:underline flex items-center font-medium">
                Clear all filters
              </button>
            </div>
          )}
          
          {filteredProducts.length === 0 ? (
            <div className="bg-white dark:bg-gray-800 rounded-xl shadow-sm p-8 text-center">
              <h3 className="text-xl font-semibold mb-2 dark:text-white">No products found</h3>
              <p className="text-gray-600 dark:text-gray-400 mb-4">Try adjusting your filters or search term.</p>
              <button onClick={clearAllFilters} className="btn btn-primary">
                Clear Filters
              </button>
            </div>
          ) : (
            <div className={`${viewMode === 'grid' ? 'grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-2 xl:grid-cols-3 gap-6' : 'space-y-6'}`}>
              {filteredProducts.map((product) => (
                <ProductCard key={product.id} product={product} />
              ))}
            </div>
          )}
        </main>
      </div>
      
      {/* Mobile Filter Modal - Fixed Version */}
      {isMobileFilterOpen && (
        <div 
          className="fixed top-0 left-0 w-full h-full z-[999999]"
          style={{ 
            position: 'fixed',
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            zIndex: 999999,
            backgroundColor: 'rgba(0, 0, 0, 0.5)'
          }}
          onClick={toggleMobileFilter}
        >
          {/* Modal Content */}
          <div 
            className="absolute bottom-0 left-0 right-0"
            style={{
              position: 'absolute',
              bottom: 0,
              left: 0,
              right: 0,
              backgroundColor: '#ffffff',
              borderTopLeftRadius: '24px',
              borderTopRightRadius: '24px',
              maxHeight: '90vh',
              display: 'flex',
              flexDirection: 'column',
              boxShadow: '0 -10px 25px rgba(0, 0, 0, 0.3)',
              border: '2px solid #ff0000'
            }}
            onClick={(e) => e.stopPropagation()}
          >
            {/* Header */}
            <div 
              style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                padding: '24px',
                borderBottom: '1px solid #e5e7eb',
                backgroundColor: '#ffffff',
                borderTopLeftRadius: '24px',
                borderTopRightRadius: '24px'
              }}
            >
              <h2 style={{ 
                fontSize: '20px', 
                fontWeight: '600', 
                color: '#111827',
                margin: 0 
              }}>
                🔍 Filters (FIXED)
              </h2>
              <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
                <button 
                  onClick={clearAllFilters} 
                  style={{
                    color: '#FF4B6E',
                    fontSize: '14px',
                    fontWeight: '500',
                    background: 'none',
                    border: 'none',
                    textDecoration: 'underline',
                    cursor: 'pointer'
                  }}
                >
                  Clear all
                </button>
                <button 
                  onClick={toggleMobileFilter}
                  style={{
                    padding: '8px',
                    backgroundColor: '#ff0000',
                    color: 'white',
                    border: 'none',
                    borderRadius: '50%',
                    cursor: 'pointer',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center'
                  }}
                >
                  <X size={24} />
                </button>
              </div>
            </div>
            
            {/* Scrollable Content */}
            <div 
              style={{
                flex: 1,
                overflowY: 'auto',
                padding: '0 24px',
                backgroundColor: '#ffffff'
              }}
            >
              {/* Price Range Filter */}
              <div style={{ paddingTop: '24px', paddingBottom: '24px', borderBottom: '1px solid #f3f4f6' }}>
                <h3 style={{ 
                  fontSize: '18px', 
                  fontWeight: '600', 
                  marginBottom: '16px', 
                  display: 'flex', 
                  alignItems: 'center',
                  color: '#111827'
                }}>
                  <span style={{ 
                    width: '16px', 
                    height: '16px', 
                    backgroundColor: '#FF4B6E', 
                    borderRadius: '50%', 
                    marginRight: '12px' 
                  }}></span>
                  Price Range
                </h3>
                
                {/* Price Display Cards */}
                <div style={{ 
                  display: 'grid', 
                  gridTemplateColumns: '1fr 1fr', 
                  gap: '16px', 
                  marginBottom: '24px' 
                }}>
                  <div style={{ 
                    backgroundColor: '#f9fafb', 
                    borderRadius: '12px', 
                    padding: '16px', 
                    textAlign: 'center' 
                  }}>
                    <p style={{ fontSize: '14px', color: '#6b7280', marginBottom: '4px', margin: 0 }}>From</p>
                    <p style={{ fontSize: '18px', fontWeight: '700', color: '#FF4B6E', margin: 0 }}>
                      {priceRange[0]} AED
                    </p>
                  </div>
                  <div style={{ 
                    backgroundColor: '#f9fafb', 
                    borderRadius: '12px', 
                    padding: '16px', 
                    textAlign: 'center' 
                  }}>
                    <p style={{ fontSize: '14px', color: '#6b7280', marginBottom: '4px', margin: 0 }}>To</p>
                    <p style={{ fontSize: '18px', fontWeight: '700', color: '#FF4B6E', margin: 0 }}>
                      {priceRange[1]} AED
                    </p>
                  </div>
                </div>
                
                {/* Range Sliders */}
                <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
                  <div>
                    <label style={{ 
                      fontSize: '14px', 
                      fontWeight: '500', 
                      color: '#374151', 
                      marginBottom: '12px', 
                      display: 'block' 
                    }}>
                      Minimum Price
                    </label>
                    <input
                      type="range"
                      min="0"
                      max="5000"
                      value={priceRange[0]}
                      onChange={(e) => updatePriceRangeDebounced([parseInt(e.target.value), priceRange[1]])}
                      className="w-full h-3 bg-gray-200 rounded-lg appearance-none cursor-pointer slider-thumb"
                      style={{
                        width: '100%',
                        height: '12px',
                        borderRadius: '8px',
                        background: `linear-gradient(to right, #FF4B6E 0%, #FF4B6E ${(priceRange[0] / 5000) * 100}%, #e5e7eb ${(priceRange[0] / 5000) * 100}%, #e5e7eb 100%)`,
                        appearance: 'none',
                        cursor: 'pointer'
                      }}
                    />
                  </div>
                  <div>
                    <label style={{ 
                      fontSize: '14px', 
                      fontWeight: '500', 
                      color: '#374151', 
                      marginBottom: '12px', 
                      display: 'block' 
                    }}>
                      Maximum Price
                    </label>
                    <input
                      type="range"
                      min="0"
                      max="5000"
                      value={priceRange[1]}
                      onChange={(e) => updatePriceRangeDebounced([priceRange[0], parseInt(e.target.value)])}
                      className="w-full h-3 bg-gray-200 rounded-lg appearance-none cursor-pointer slider-thumb"
                      style={{
                        width: '100%',
                        height: '12px',
                        borderRadius: '8px',
                        background: `linear-gradient(to right, #FF4B6E 0%, #FF4B6E ${(priceRange[1] / 5000) * 100}%, #e5e7eb ${(priceRange[1] / 5000) * 100}%, #e5e7eb 100%)`,
                        appearance: 'none',
                        cursor: 'pointer'
                      }}
                    />
                  </div>
                </div>
                
                {/* Quick Preset Buttons */}
                <div style={{ 
                  marginTop: '24px', 
                  display: 'grid', 
                  gridTemplateColumns: '1fr 1fr', 
                  gap: '12px' 
                }}>
                  <button
                    onClick={() => setPriceRange([0, 500])}
                    style={{
                      fontSize: '14px',
                      padding: '12px 16px',
                      backgroundColor: 'rgba(255, 75, 110, 0.1)',
                      color: '#FF4B6E',
                      border: 'none',
                      borderRadius: '12px',
                      fontWeight: '500',
                      cursor: 'pointer'
                    }}
                  >
                    Under 500
                  </button>
                  <button
                    onClick={() => setPriceRange([500, 1500])}
                    style={{
                      fontSize: '14px',
                      padding: '12px 16px',
                      backgroundColor: 'rgba(255, 75, 110, 0.1)',
                      color: '#FF4B6E',
                      border: 'none',
                      borderRadius: '12px',
                      fontWeight: '500',
                      cursor: 'pointer'
                    }}
                  >
                    500 - 1500
                  </button>
                  <button
                    onClick={() => setPriceRange([1500, 3000])}
                    style={{
                      fontSize: '14px',
                      padding: '12px 16px',
                      backgroundColor: 'rgba(255, 75, 110, 0.1)',
                      color: '#FF4B6E',
                      border: 'none',
                      borderRadius: '12px',
                      fontWeight: '500',
                      cursor: 'pointer'
                    }}
                  >
                    1500 - 3000
                  </button>
                  <button
                    onClick={() => setPriceRange([3000, 5000])}
                    style={{
                      fontSize: '14px',
                      padding: '12px 16px',
                      backgroundColor: 'rgba(255, 75, 110, 0.1)',
                      color: '#FF4B6E',
                      border: 'none',
                      borderRadius: '12px',
                      fontWeight: '500',
                      cursor: 'pointer'
                    }}
                  >
                    Over 3000
                  </button>
                </div>
              </div>
              
              {/* Rating Filter */}
              <div style={{ paddingTop: '24px', paddingBottom: '24px' }}>
                <h3 style={{ 
                  fontSize: '18px', 
                  fontWeight: '600', 
                  marginBottom: '16px', 
                  display: 'flex', 
                  alignItems: 'center',
                  color: '#111827'
                }}>
                  <span style={{ 
                    width: '16px', 
                    height: '16px', 
                    backgroundColor: '#eab308', 
                    borderRadius: '50%', 
                    marginRight: '12px' 
                  }}></span>
                  Rating
                </h3>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                  {[4, 3, 2, 1].map((rating) => (
                    <button 
                      key={rating} 
                      onClick={() => handleRatingSelect(rating)} 
                      style={{
                        display: 'flex',
                        alignItems: 'center',
                        width: '100%',
                        padding: '16px',
                        borderRadius: '12px',
                        border: selectedRating === rating ? '2px solid rgba(255, 75, 110, 0.2)' : '2px solid transparent',
                        backgroundColor: selectedRating === rating ? 'rgba(255, 75, 110, 0.1)' : '#f9fafb',
                        color: selectedRating === rating ? '#FF4B6E' : '#374151',
                        cursor: 'pointer',
                        fontSize: '16px',
                        fontWeight: '500'
                      }}
                    >
                      <div style={{ display: 'flex', alignItems: 'center', color: '#eab308' }}>
                        {[...Array(5)].map((_, i) => (
                          <Star key={i} size={18} fill={i < rating ? "currentColor" : "none"} stroke="currentColor" />
                        ))}
                      </div>
                      <span style={{ marginLeft: '12px' }}>& Up</span>
                      {selectedRating === rating && <Check size={20} style={{ marginLeft: 'auto', color: '#FF4B6E' }} />}
                    </button>
                  ))}
                </div>
              </div>
            </div>
            
            {/* Footer */}
            <div style={{
              padding: '24px',
              borderTop: '1px solid #e5e7eb',
              backgroundColor: '#ffffff'
            }}>
              <button 
                onClick={toggleMobileFilter} 
                style={{
                  width: '100%',
                  backgroundColor: '#FF4B6E',
                  color: 'white',
                  fontWeight: '600',
                  padding: '16px 24px',
                  borderRadius: '12px',
                  border: 'none',
                  fontSize: '18px',
                  cursor: 'pointer'
                }}
              >
                Apply Filters
              </button>
            </div>
          </div>
        </div>
      )}
      
      {/* Mobile Filter Modal - Beautiful Version */}
      {isMobileFilterOpen && (
        <div style={{
          position: 'fixed',
          top: 0,
          left: 0,
          width: '100vw',
          height: '100vh',
          zIndex: 9999999,
          backgroundColor: 'rgba(0, 0, 0, 0.6)',
          display: 'flex',
          alignItems: 'flex-end',
          justifyContent: 'center'
        }}>
          {/* Backdrop */}
          <div 
            style={{
              position: 'absolute',
              top: 0,
              left: 0,
              right: 0,
              bottom: 0
            }}
            onClick={toggleMobileFilter}
          />
          
          {/* Modal Content */}
          <div style={{
            position: 'relative',
            width: '100%',
            maxHeight: '65vh', // Reduced from 85vh
            backgroundColor: '#ffffff',
            borderRadius: '24px 24px 0 0',
            display: 'flex',
            flexDirection: 'column',
            boxShadow: '0 -10px 25px rgba(0, 0, 0, 0.15)',
            overflow: 'hidden'
          }}>
            {/* Header */}
            <div style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              padding: '16px 20px',
              borderBottom: '1px solid #f1f5f9',
              backgroundColor: '#ffffff',
              position: 'sticky',
              top: 0,
              zIndex: 10
            }}>
              <h2 style={{
                fontSize: '20px',
                fontWeight: '600',
                color: '#1e293b',
                margin: 0,
                display: 'flex',
                alignItems: 'center'
              }}>
                <span style={{
                  marginRight: '8px',
                  fontSize: '24px'
                }}>🔍</span>
                Filters
              </h2>
              <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
                <button 
                  onClick={clearAllFilters}
                  style={{
                    background: 'none',
                    border: 'none',
                    color: '#FF4B6E',
                    fontSize: '14px',
                    fontWeight: '500',
                    cursor: 'pointer',
                    padding: '4px 8px',
                    borderRadius: '6px',
                    transition: 'background-color 0.2s'
                  }}
                  onMouseOver={(e) => (e.target as HTMLElement).style.backgroundColor = '#fef2f2'}
                  onMouseOut={(e) => (e.target as HTMLElement).style.backgroundColor = 'transparent'}
                >
                  Clear all
                </button>
                <button 
                  onClick={toggleMobileFilter}
                  style={{
                    width: '36px',
                    height: '36px',
                    borderRadius: '50%',
                    border: 'none',
                    backgroundColor: '#f8fafc',
                    color: '#64748b',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    cursor: 'pointer',
                    fontSize: '18px',
                    transition: 'all 0.2s'
                  }}
                  onMouseOver={(e) => {
                    (e.target as HTMLElement).style.backgroundColor = '#e2e8f0';
                    (e.target as HTMLElement).style.color = '#475569';
                  }}
                  onMouseOut={(e) => {
                    (e.target as HTMLElement).style.backgroundColor = '#f8fafc';
                    (e.target as HTMLElement).style.color = '#64748b';
                  }}
                >
                  ✕
                </button>
              </div>
            </div>
            
            {/* Content */}
            <div style={{
              flex: 1,
              overflowY: 'auto',
              padding: '16px 20px 20px',
              backgroundColor: '#ffffff'
            }}>
              {/* Price Range Filter */}
              <div style={{ 
                paddingTop: '24px',
                paddingBottom: '24px',
                borderBottom: '1px solid #f1f5f9'
              }}>
                <h3 style={{
                  fontSize: '18px',
                  fontWeight: '600',
                  color: '#1e293b',
                  marginBottom: '16px',
                  display: 'flex',
                  alignItems: 'center',
                  margin: '0 0 16px 0'
                }}>
                  <span style={{
                    width: '8px',
                    height: '8px',
                    backgroundColor: '#FF4B6E',
                    borderRadius: '50%',
                    marginRight: '12px'
                  }}></span>
                  Price Range
                </h3>
                
                {/* Price Display Cards */}
                <div style={{
                  display: 'grid',
                  gridTemplateColumns: '1fr 1fr',
                  gap: '12px',
                  marginBottom: '20px'
                }}>
                  <div style={{
                    backgroundColor: '#f8fafc',
                    borderRadius: '12px',
                    padding: '16px',
                    textAlign: 'center',
                    border: '1px solid #e2e8f0'
                  }}>
                    <p style={{
                      fontSize: '12px',
                      color: '#64748b',
                      margin: '0 0 4px 0',
                      fontWeight: '500'
                    }}>From</p>
                    <p style={{
                      fontSize: '18px',
                      fontWeight: '700',
                      color: '#FF4B6E',
                      margin: 0
                    }}>{tempPriceRange[0]} AED</p>
                  </div>
                  <div style={{
                    backgroundColor: '#f8fafc',
                    borderRadius: '12px',
                    padding: '16px',
                    textAlign: 'center',
                    border: '1px solid #e2e8f0'
                  }}>
                    <p style={{
                      fontSize: '12px',
                      color: '#64748b',
                      margin: '0 0 4px 0',
                      fontWeight: '500'
                    }}>To</p>
                    <p style={{
                      fontSize: '18px',
                      fontWeight: '700',
                      color: '#FF4B6E',
                      margin: 0
                    }}>{tempPriceRange[1]} AED</p>
                  </div>
                </div>
                
                {/* Range Sliders */}
                <div style={{ marginBottom: '20px' }}>
                  <label style={{
                    display: 'block',
                    fontSize: '14px',
                    fontWeight: '500',
                    color: '#475569',
                    marginBottom: '8px'
                  }}>
                    Minimum Price
                  </label>
                  <input
                    type="range"
                    min="0"
                    max="5000"
                    value={tempPriceRange[0]}
                    onChange={(e) => updatePriceRangeDebounced([parseInt(e.target.value), tempPriceRange[1]])}
                    className="slider-thumb"
                    style={{
                      width: '100%',
                      height: '8px',
                      borderRadius: '4px',
                      outline: 'none',
                      cursor: 'pointer',
                      background: `linear-gradient(to right, #FF4B6E 0%, #FF4B6E ${(tempPriceRange[0] / 5000) * 100}%, #e2e8f0 ${(tempPriceRange[0] / 5000) * 100}%, #e2e8f0 100%)`,
                      WebkitAppearance: 'none',
                      transition: 'none'
                    }}
                  />
                </div>
                
                <div style={{ marginBottom: '20px' }}>
                  <label style={{
                    display: 'block',
                    fontSize: '14px',
                    fontWeight: '500',
                    color: '#475569',
                    marginBottom: '8px'
                  }}>
                    Maximum Price
                  </label>
                  <input
                    type="range"
                    min="0"
                    max="5000"
                    value={tempPriceRange[1]}
                    onChange={(e) => updatePriceRangeDebounced([tempPriceRange[0], parseInt(e.target.value)])}
                    className="slider-thumb"
                    style={{
                      width: '100%',
                      height: '8px',
                      borderRadius: '4px',
                      outline: 'none',
                      cursor: 'pointer',
                      background: `linear-gradient(to right, #FF4B6E 0%, #FF4B6E ${(tempPriceRange[1] / 5000) * 100}%, #e2e8f0 ${(tempPriceRange[1] / 5000) * 100}%, #e2e8f0 100%)`,
                      WebkitAppearance: 'none',
                      transition: 'none'
                    }}
                  />
                </div>
                
                {/* Quick Preset Buttons */}
                <div style={{
                  display: 'grid',
                  gridTemplateColumns: '1fr 1fr',
                  gap: '8px',
                  marginTop: '16px'
                }}>
                  {[
                    { range: [0, 500], label: 'Under 500' },
                    { range: [500, 1500], label: '500 - 1500' },
                    { range: [1500, 3000], label: '1500 - 3000' },
                    { range: [3000, 5000], label: 'Over 3000' }
                  ].map((preset, index) => (
                    <button
                      key={index}
                      onClick={() => setPriceRange(preset.range as [number, number])}
                      style={{
                        padding: '8px 12px',
                        fontSize: '12px',
                        fontWeight: '500',
                        backgroundColor: '#fef2f2',
                        color: '#FF4B6E',
                        border: '1px solid #fecaca',
                        borderRadius: '8px',
                        cursor: 'pointer',
                        transition: 'all 0.2s'
                      }}
                      onMouseOver={(e) => {
                        (e.target as HTMLElement).style.backgroundColor = '#fee2e2';
                        (e.target as HTMLElement).style.borderColor = '#fca5a5';
                      }}
                      onMouseOut={(e) => {
                        (e.target as HTMLElement).style.backgroundColor = '#fef2f2';
                        (e.target as HTMLElement).style.borderColor = '#fecaca';
                      }}
                    >
                      {preset.label}
                    </button>
                  ))}
                </div>
              </div>
              
              {/* Rating Filter */}
              <div style={{ paddingTop: '24px' }}>
                <h3 style={{
                  fontSize: '18px',
                  fontWeight: '600',
                  color: '#1e293b',
                  marginBottom: '16px',
                  display: 'flex',
                  alignItems: 'center',
                  margin: '0 0 16px 0'
                }}>
                  <span style={{
                    width: '8px',
                    height: '8px',
                    backgroundColor: '#fbbf24',
                    borderRadius: '50%',
                    marginRight: '12px'
                  }}></span>
                  Rating
                </h3>
                
                <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                  {[4, 3, 2, 1].map((rating) => (
                    <button 
                      key={rating} 
                      onClick={() => handleRatingSelect(rating)} 
                      style={{
                        display: 'flex',
                        alignItems: 'center',
                        width: '100%',
                        padding: '12px 16px',
                        backgroundColor: selectedRating === rating ? '#fef2f2' : '#ffffff',
                        border: selectedRating === rating ? '2px solid #FF4B6E' : '2px solid #e2e8f0',
                        borderRadius: '12px',
                        fontSize: '14px',
                        fontWeight: '500',
                        color: selectedRating === rating ? '#FF4B6E' : '#475569',
                        cursor: 'pointer',
                        transition: 'all 0.2s'
                      }}
                      onMouseOver={(e) => {
                        if (selectedRating !== rating) {
                          (e.target as HTMLElement).style.backgroundColor = '#f8fafc';
                          (e.target as HTMLElement).style.borderColor = '#cbd5e1';
                        }
                      }}
                      onMouseOut={(e) => {
                        if (selectedRating !== rating) {
                          (e.target as HTMLElement).style.backgroundColor = '#ffffff';
                          (e.target as HTMLElement).style.borderColor = '#e2e8f0';
                        }
                      }}
                    >
                      <div style={{
                        display: 'flex',
                        alignItems: 'center',
                        color: '#fbbf24',
                        marginRight: '8px'
                      }}>
                        {[...Array(5)].map((_, i) => (
                          <span 
                            key={i} 
                            style={{
                              fontSize: '14px',
                              marginRight: '1px'
                            }}
                          >
                            {i < rating ? '★' : '☆'}
                          </span>
                        ))}
                      </div>
                      <span>{rating}+ Stars</span>
                      {selectedRating === rating && (
                        <span style={{ 
                          marginLeft: 'auto', 
                          color: '#FF4B6E',
                          fontWeight: '600'
                        }}>
                          ✓
                        </span>
                      )}
                    </button>
                  ))}
                </div>
              </div>
            </div>
            
            {/* Footer */}
            <div style={{
              padding: '16px 20px',
              borderTop: '1px solid #f1f5f9',
              backgroundColor: '#ffffff',
              position: 'sticky',
              bottom: 0
            }}>
              <button 
                onClick={toggleMobileFilter}
                style={{
                  width: '100%',
                  backgroundColor: '#FF4B6E',
                  color: '#ffffff',
                  fontWeight: '600',
                  padding: '14px 24px',
                  borderRadius: '12px',
                  border: 'none',
                  fontSize: '16px',
                  cursor: 'pointer',
                  transition: 'all 0.2s',
                  boxShadow: '0 4px 12px rgba(255, 75, 110, 0.3)'
                }}
                onMouseOver={(e) => {
                  (e.target as HTMLElement).style.backgroundColor = '#e63950';
                  (e.target as HTMLElement).style.transform = 'translateY(-1px)';
                  (e.target as HTMLElement).style.boxShadow = '0 6px 16px rgba(255, 75, 110, 0.4)';
                }}
                onMouseOut={(e) => {
                  (e.target as HTMLElement).style.backgroundColor = '#FF4B6E';
                  (e.target as HTMLElement).style.transform = 'translateY(0)';
                  (e.target as HTMLElement).style.boxShadow = '0 4px 12px rgba(255, 75, 110, 0.3)';
                }}
              >
                Apply Filters
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default SearchResultsPage;