import { useState, useEffect } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Menu, X, Bookmark, User } from 'lucide-react';
import SearchBar from './SearchBar';
import ThemeToggle from './ThemeToggle';

const Header = () => {
  const [isMenuOpen, setIsMenuOpen] = useState(false);
  const [isScrolled, setIsScrolled] = useState(false);
  const { isAuthenticated, user, logout } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  
  // Handle scroll effect
  useEffect(() => {
    const handleScroll = () => {
      if (window.scrollY > 10) {
        setIsScrolled(true);
      } else {
        setIsScrolled(false);
      }
    };
    
    window.addEventListener('scroll', handleScroll);
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);
  
  // Close mobile menu on navigation
  useEffect(() => {
    setIsMenuOpen(false);
  }, [location.pathname]);
  
  const toggleMenu = () => setIsMenuOpen(!isMenuOpen);
  
  const handleLogout = () => {
    logout();
    navigate('/');
  };
  
  const isHomePage = location.pathname === '/';
  
  return (
    <header className={`sticky top-0 z-50 transition-all duration-300 ${isScrolled || !isHomePage ? 'bg-white dark:bg-gray-800 shadow-sm' : 'bg-transparent'}`}>
      <div className="container mx-auto px-4 md:px-6">
        <div className="flex items-center justify-between h-16 md:h-20">
          {/* Logo */}
          <Link to="/" className="flex items-center space-x-2">
            <span className="text-primary text-xl font-bold">Findr</span>
          </Link>
          
          {/* Desktop Navigation */}
          <nav className="hidden md:flex items-center space-x-8">
            <Link
              to="/"
              className={`text-sm font-medium transition-smooth hover:text-primary hover:scale-105 relative ${location.pathname === '/' ? 'text-primary' : 'text-secondary dark:text-gray-300'}`}
            >
              Home
              {location.pathname === '/' && (
                <span className="absolute -bottom-1 left-0 w-full h-0.5 bg-primary rounded-full"></span>
              )}
            </Link>
            <Link
              to="/search"
              className={`text-sm font-medium transition-smooth hover:text-primary hover:scale-105 relative ${location.pathname === '/search' ? 'text-primary' : 'text-secondary dark:text-gray-300'}`}
            >
              Compare
              {location.pathname === '/search' && (
                <span className="absolute -bottom-1 left-0 w-full h-0.5 bg-primary rounded-full"></span>
              )}
            </Link>
            {isAuthenticated && (
              <Link
                to="/bookmarks"
                className={`text-sm font-medium transition-smooth hover:text-primary hover:scale-105 relative ${location.pathname === '/bookmarks' ? 'text-primary' : 'text-secondary dark:text-gray-300'}`}
              >
                My Bookmarks
                {location.pathname === '/bookmarks' && (
                  <span className="absolute -bottom-1 left-0 w-full h-0.5 bg-primary rounded-full"></span>
                )}
              </Link>
            )}
          </nav>
          
          {/* Right Section: Search & Auth */}
          <div className="hidden md:flex items-center space-x-4">
            {!isHomePage && (
              <div className="relative w-64">
                <SearchBar minimal />
              </div>
            )}
            
            {isAuthenticated ? (
              <div className="flex items-center space-x-4">
                <ThemeToggle />
                <Link to="/bookmarks" className="text-secondary dark:text-gray-300 hover:text-primary transition-smooth hover:scale-110">
                  <Bookmark size={20} />
                </Link>
                <div className="relative group">
                  <button className="flex items-center space-x-2 focus:outline-none transition-smooth hover:scale-105">
                    <div className="w-8 h-8 rounded-full bg-primary/10 flex items-center justify-center text-primary transition-smooth group-hover:bg-primary/20">
                      {user?.name?.charAt(0) || <User size={16} />}
                    </div>
                  </button>
                  <div className="absolute right-0 mt-2 w-48 bg-white dark:bg-gray-800 rounded-lg shadow-lg overflow-hidden z-20 opacity-0 invisible group-hover:opacity-100 group-hover:visible transition-all duration-300 border dark:border-gray-700 transform translate-y-2 group-hover:translate-y-0">
                    <div className="py-2">
                      <Link to="/profile" className="block px-4 py-2 text-sm text-secondary dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 transition-smooth hover:translate-x-1">Profile</Link>
                      <Link to="/bookmarks" className="block px-4 py-2 text-sm text-secondary dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 transition-smooth hover:translate-x-1">My Bookmarks</Link>
                      <button
                        onClick={handleLogout}
                        className="block w-full text-left px-4 py-2 text-sm text-secondary dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 transition-smooth hover:translate-x-1"
                      >
                        Logout
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            ) : (
              <div className="flex items-center space-x-4">
                <ThemeToggle />
                <Link to="/login" className="text-sm font-medium text-secondary dark:text-gray-300 hover:text-primary transition-smooth hover:scale-105">
                  Login
                </Link>
                <Link to="/register" className="btn btn-primary text-sm py-2 transition-bounce hover:scale-105 hover:shadow-lg">
                  Sign Up
                </Link>
              </div>
            )}
          </div>
          
          {/* Mobile Menu Button */}
          <button 
            onClick={toggleMenu}
            className="md:hidden flex items-center text-secondary dark:text-gray-300 focus:outline-none transition-smooth hover:scale-110 hover:text-primary"
          >
            {isMenuOpen ? <X size={24} /> : <Menu size={24} />}
          </button>
        </div>
      </div>
      
      {/* Mobile Menu */}
      <div className={`md:hidden overflow-hidden transition-all duration-300 ease-in-out bg-white dark:bg-gray-800 ${isMenuOpen ? 'max-h-screen py-4' : 'max-h-0'}`}>
        <div className="container mx-auto px-4 space-y-4">
          <div className="flex justify-between items-center">
            <span className="text-lg font-semibold dark:text-white">Menu</span>
            <ThemeToggle />
          </div>
          {!isHomePage && (
            <SearchBar />
          )}
          
          <nav className="flex flex-col space-y-4">
            <Link to="/" className="text-secondary dark:text-gray-300 hover:text-primary py-2">Home</Link>
            <Link to="/search" className="text-secondary dark:text-gray-300 hover:text-primary py-2">Compare</Link>
            {isAuthenticated && (
              <Link to="/bookmarks" className="text-secondary dark:text-gray-300 hover:text-primary py-2">My Bookmarks</Link>
            )}
            
            {isAuthenticated ? (
              <>
                <Link to="/profile" className="text-secondary dark:text-gray-300 hover:text-primary py-2">Profile</Link>
                <button
                  onClick={handleLogout}
                  className="text-left text-secondary dark:text-gray-300 hover:text-primary py-2"
                >
                  Logout
                </button>
              </>
            ) : (
              <div className="flex flex-col space-y-2 pt-2">
                <Link to="/login" className="text-secondary dark:text-gray-300 hover:text-primary py-2">Login</Link>
                <Link to="/register" className="btn btn-primary">Sign Up</Link>
              </div>
            )}
          </nav>
        </div>
      </div>
    </header>
  );
};

export default Header;