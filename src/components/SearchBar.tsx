import { useState, useRef, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Search, X } from 'lucide-react';
import { mockSuggestions } from '../data/mockData';

interface SearchBarProps {
  minimal?: boolean;
}

const SearchBar = ({ minimal = false }: SearchBarProps) => {
  const [query, setQuery] = useState('');
  const [suggestions, setSuggestions] = useState<string[]>([]);
  const [isFocused, setIsFocused] = useState(false);
  const navigate = useNavigate();
  const inputRef = useRef<HTMLInputElement>(null);
  const wrapperRef = useRef<HTMLDivElement>(null);
  
  // Handle click outside to close suggestions
  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (wrapperRef.current && !wrapperRef.current.contains(event.target as Node)) {
        setIsFocused(false);
      }
    }
    
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);
  
  // Handle suggestions
  useEffect(() => {
    if (query.length > 1) {
      // Filter mock suggestions based on query
      const filtered = mockSuggestions.filter(item => 
        item.toLowerCase().includes(query.toLowerCase())
      ).slice(0, 5);
      
      setSuggestions(filtered);
    } else {
      setSuggestions([]);
    }
  }, [query]);
  
  const handleSearch = (e?: React.FormEvent) => {
    e?.preventDefault();
    if (query.trim()) {
      navigate(`/search?q=${encodeURIComponent(query.trim())}`);
      setIsFocused(false);
    }
  };
  
  const handleSuggestionClick = (suggestion: string) => {
    setQuery(suggestion);
    navigate(`/search?q=${encodeURIComponent(suggestion)}`);
    setIsFocused(false);
  };
  
  const clearSearch = () => {
    setQuery('');
    if (inputRef.current) {
      inputRef.current.focus();
    }
  };
  
  return (
    <div 
      ref={wrapperRef}
      className={`relative ${minimal ? '' : 'w-full max-w-2xl mx-auto'}`}
    >
      <form onSubmit={handleSearch} className="relative">
        <div className={`
          relative flex items-center 
          ${minimal 
            ? 'bg-gray-100 rounded-lg' 
            : 'bg-white rounded-full shadow-lg floating-search'
          }`}
        >
          <Search 
            size={18} 
            className={`absolute left-4 text-gray-400 ${minimal ? 'opacity-60' : ''}`} 
          />
          <input
            ref={inputRef}
            type="text"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            onFocus={() => setIsFocused(true)}
            placeholder="Search for products, brands, or categories..."
            className={`
              w-full bg-transparent border-none focus:outline-none focus:ring-0
              ${minimal 
                ? 'pl-10 pr-10 py-2 text-sm' 
                : 'pl-12 pr-12 py-4 text-base'
              }
            `}
          />
          {query && (
            <button 
              type="button"
              onClick={clearSearch}
              className="absolute right-4 text-gray-400 hover:text-gray-600"
            >
              <X size={16} />
            </button>
          )}
        </div>
      </form>
      
      {/* Suggestions dropdown */}
      {isFocused && suggestions.length > 0 && (
        <div className="absolute z-50 w-full mt-2 bg-white rounded-lg shadow-lg border border-gray-100 overflow-hidden animate-fadeIn">
          <ul>
            {suggestions.map((suggestion, index) => (
              <li key={index}>
                <button
                  onClick={() => handleSuggestionClick(suggestion)}
                  className="w-full px-4 py-3 text-left hover:bg-gray-50 flex items-center text-sm"
                >
                  <Search size={14} className="mr-2 text-gray-400" />
                  {suggestion}
                </button>
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
};

export default SearchBar;