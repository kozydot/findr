import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowRight, TrendingUp, ShieldCheck, Zap } from 'lucide-react';
import { motion } from 'framer-motion';
import { useInView } from 'react-intersection-observer';
import SearchBar from '../components/SearchBar';
import ProductCard from '../components/ProductCard';
import { useProducts } from '../context/ProductContext';

const HomePage = () => {
  const navigate = useNavigate();
  const [searchQuery, setSearchQuery] = useState('');
  const { featuredProducts, loading, error } = useProducts();
  const [categories, setCategories] = useState<string[]>([]);
  
  // Intersection observer hooks for animation
  const [heroRef, heroInView] = useInView({ triggerOnce: true, threshold: 0.1 });
  const [featuredRef, featuredInView] = useInView({ triggerOnce: true, threshold: 0.1 });
  const [benefitsRef, benefitsInView] = useInView({ triggerOnce: true, threshold: 0.1 });
  const [categoriesRef, categoriesInView] = useInView({ triggerOnce: true, threshold: 0.1 });

  useEffect(() => {
    const fetchCategories = async () => {
      try {
        const response = await fetch('/api/v1/products/categories');
        const data = await response.json();
        setCategories(data);
      } catch (error) {
        console.error('Error fetching categories:', error);
      }
    };

    fetchCategories();
  }, []);
  
  // Handle search
  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    if (searchQuery.trim()) {
      navigate(`/search?q=${encodeURIComponent(searchQuery.trim())}`);
    }
  };
  
  const containerVariants = {
    hidden: { opacity: 0 },
    visible: {
      opacity: 1,
      transition: {
        staggerChildren: 0.1
      }
    }
  };
  
  const itemVariants = {
    hidden: { y: 20, opacity: 0 },
    visible: {
      y: 0,
      opacity: 1,
      transition: { duration: 0.5 }
    }
  };
  
  const benefitItems = [
    {
      icon: <TrendingUp size={24} className="text-primary" />,
      title: "Real-time Price Tracking",
      description: "Monitor prices across major UAE retailers including Amazon.ae, Noon.com, and LuLu Hypermarket."
    },
    {
      icon: <ShieldCheck size={24} className="text-primary" />,
      title: "Price Drop Alerts",
      description: "Get notified instantly when prices drop below your target, ensuring you never miss a deal."
    },
    {
      icon: <Zap size={24} className="text-primary" />,
      title: "Price History Charts",
      description: "View historical price data to make informed purchasing decisions and identify trends."
    }
  ];
  
  return (
    <div className="flex flex-col min-h-screen">
      {/* Hero Section */}
      <section 
        ref={heroRef}
        className="relative bg-gradient-to-br from-primary to-primary-light dark:from-primary/80 dark:to-primary-light/80 py-20 md:py-32"
      >
        <div className="container mx-auto px-4 md:px-6">
          <motion.div
            className="max-w-3xl mx-auto text-center"
            initial="hidden"
            animate={heroInView ? "visible" : "hidden"}
            variants={containerVariants}
          >
            <motion.h1 
              className="text-white font-bold mb-6"
              variants={itemVariants}
            >
              Find the Best Deals Across UAE's Top Retailers
            </motion.h1>
            
            <motion.p 
              className="text-white/90 text-lg md:text-xl mb-10"
              variants={itemVariants}
            >
              Compare prices, track price history, and get alerts when prices drop on your favorite products.
            </motion.p>
            
            <motion.div variants={itemVariants}>
              <form onSubmit={handleSearch} className="max-w-2xl mx-auto">
                <div className="relative">
                  <input
                    type="text"
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    placeholder="Search for products, brands, or categories..."
                    className="w-full px-6 py-4 pr-12 rounded-full text-base shadow-lg focus:outline-none focus:ring-2 focus:ring-white/50 dark:bg-gray-800 dark:text-white dark:placeholder-gray-400"
                  />
                  <button
                    type="submit"
                    className="absolute right-2 top-1/2 transform -translate-y-1/2 bg-primary text-white p-2 rounded-full hover:bg-primary/90 transition-colors"
                  >
                    <ArrowRight size={20} />
                  </button>
                </div>
              </form>
            </motion.div>
            
            <motion.div 
              className="mt-6 text-white/80 text-sm"
              variants={itemVariants}
            >
              Popular searches: iPhone, Samsung TV, PlayStation 5, Air Fryer
            </motion.div>
          </motion.div>
        </div>
        
        {/* Wave effect */}
        <div className="absolute bottom-0 left-0 right-0 h-16 md:h-24">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1440 320" className="w-full h-full">
            <path
              fill="currentColor"
              className="text-white dark:text-background-dark"
              fillOpacity="1"
              d="M0,192L48,202.7C96,213,192,235,288,224C384,213,480,171,576,165.3C672,160,768,192,864,197.3C960,203,1056,181,1152,160C1248,139,1344,117,1392,106.7L1440,96L1440,320L1392,320C1344,320,1248,320,1152,320C1056,320,960,320,864,320C768,320,672,320,576,320C480,320,384,320,288,320C192,320,96,320,48,320L0,320Z"
            ></path>
          </svg>
        </div>
      </section>
      
      {/* Featured Products */}
      <section 
        ref={featuredRef}
        className="py-16 bg-white dark:bg-background-dark"
      >
        <div className="container mx-auto px-4 md:px-6">
          <motion.div
            className="text-center mb-12"
            initial="hidden"
            animate={featuredInView ? "visible" : "hidden"}
            variants={containerVariants}
          >
            <motion.h2 
              className="text-3xl font-bold text-secondary mb-4"
              variants={itemVariants}
            >
              Best Deals Today
            </motion.h2>
            <motion.p 
              className="text-gray-600 dark:text-gray-400 max-w-2xl mx-auto"
              variants={itemVariants}
            >
              We've found the best price drops and deals across UAE's top online retailers.
            </motion.p>
          </motion.div>
          
          <motion.div 
            className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6"
            initial="hidden"
            animate={featuredInView ? "visible" : "hidden"}
            variants={containerVariants}
          >
            {loading && <p>Loading products...</p>}
            {error && <p className="text-red-500">{error}</p>}
            {featuredProducts.map((product) => (
              <motion.div key={product.id} variants={itemVariants}>
                <ProductCard product={product} />
              </motion.div>
            ))}
          </motion.div>
          
          <motion.div 
            className="text-center mt-10"
            initial="hidden"
            animate={featuredInView ? "visible" : "hidden"}
            variants={containerVariants}
          >
            <motion.button
              className="btn btn-primary inline-flex items-center"
              onClick={() => navigate('/search')}
              variants={itemVariants}
            >
              View More Deals
              <ArrowRight size={16} className="ml-2" />
            </motion.button>
          </motion.div>
        </div>
      </section>
      
      {/* Categories Section */}
      <section 
        ref={categoriesRef}
        className="py-16 bg-gray-50 dark:bg-gray-900"
      >
        <div className="container mx-auto px-4 md:px-6">
          <motion.div
            className="text-center mb-12"
            initial="hidden"
            animate={categoriesInView ? "visible" : "hidden"}
            variants={containerVariants}
          >
            <motion.h2 
              className="text-3xl font-bold text-secondary mb-4"
              variants={itemVariants}
            >
              Shop by Category
            </motion.h2>
            <motion.p 
              className="text-gray-600 dark:text-gray-400 max-w-2xl mx-auto"
              variants={itemVariants}
            >
              Browse our most popular categories.
            </motion.p>
          </motion.div>
          
          <motion.div 
            className="grid grid-cols-2 md:grid-cols-4 gap-4"
            initial="hidden"
            animate={categoriesInView ? "visible" : "hidden"}
            variants={containerVariants}
          >
            {categories.map((category, index) => (
              <motion.div 
                key={index}
                className="bg-white dark:bg-gray-800 p-6 rounded-xl shadow-sm hover:shadow-md transition-all text-center cursor-pointer"
                variants={itemVariants}
                onClick={() => navigate(`/search?q=${encodeURIComponent(category)}`)}
              >
                <h3 className="text-xl font-semibold">{category}</h3>
              </motion.div>
            ))}
          </motion.div>
        </div>
      </section>

      {/* Benefits Section */}
      <section 
        ref={benefitsRef}
        className="py-16 bg-white dark:bg-background-dark"
      >
        <div className="container mx-auto px-4 md:px-6">
          <motion.div
            className="text-center mb-12"
            initial="hidden"
            animate={benefitsInView ? "visible" : "hidden"}
            variants={containerVariants}
          >
            <motion.h2 
              className="text-3xl font-bold text-secondary mb-4"
              variants={itemVariants}
            >
              Why Choose Findr
            </motion.h2>
            <motion.p 
              className="text-gray-600 dark:text-gray-400 max-w-2xl mx-auto"
              variants={itemVariants}
            >
              We help you save time and money by finding the best prices across UAE's top online retailers.
            </motion.p>
          </motion.div>
          
          <motion.div 
            className="grid grid-cols-1 md:grid-cols-3 gap-8"
            initial="hidden"
            animate={benefitsInView ? "visible" : "hidden"}
            variants={containerVariants}
          >
            {benefitItems.map((item, index) => (
              <motion.div 
                key={index}
                className="bg-white dark:bg-gray-800 p-6 rounded-xl shadow-sm hover:shadow-md transition-all"
                variants={itemVariants}
              >
                <div className="w-12 h-12 bg-primary/10 rounded-full flex items-center justify-center mb-4">
                  {item.icon}
                </div>
                <h3 className="text-xl font-semibold mb-3">{item.title}</h3>
                <p className="text-gray-600 dark:text-gray-400">{item.description}</p>
              </motion.div>
            ))}
          </motion.div>
        </div>
      </section>
      
      {/* CTA Section */}
      <section className="bg-primary py-16">
        <div className="container mx-auto px-4 md:px-6 text-center">
          <h2 className="text-3xl font-bold text-white mb-6">
            Start Saving Today
          </h2>
          <p className="text-white/90 text-lg max-w-2xl mx-auto mb-8">
            Join thousands of smart shoppers in UAE who use Findr to find the best deals.
          </p>
          <button 
            onClick={() => navigate('/register')}
            className="btn bg-white text-primary hover:bg-white/90"
          >
            Create Free Account
          </button>
        </div>
      </section>
    </div>
  );
};

export default HomePage;
