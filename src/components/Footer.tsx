import { Link } from 'react-router-dom';
import { Github, Twitter, Facebook, Instagram, Mail } from 'lucide-react';

const Footer = () => {
  return (
    <footer className="bg-background-dark text-white py-12 mt-auto">
      <div className="container mx-auto px-4 md:px-6">
        <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
          <div className="col-span-1 md:col-span-2">
            <Link to="/" className="inline-flex items-center space-x-2">
              <span className="text-primary text-xl font-bold">Findr</span>
            </Link>
            <p className="mt-4 text-gray-400 text-sm">
              Findr is an intelligent price comparison platform that automatically finds the best deals across multiple UAE retailers.
              Using advanced product matching algorithms and real-time price monitoring, we help you save money and time on every purchase.
            </p>
          </div>
          
          <div>
            <h4 className="text-white font-medium mb-4">Quick Links</h4>
            <ul className="space-y-2">
              <li><Link to="/" className="text-gray-400 hover:text-primary transition-colors text-sm">Home</Link></li>
              <li><Link to="/search" className="text-gray-400 hover:text-primary transition-colors text-sm">Compare Prices</Link></li>
              <li><Link to="/bookmarks" className="text-gray-400 hover:text-primary transition-colors text-sm">My Bookmarks</Link></li>
              <li><Link to="/login" className="text-gray-400 hover:text-primary transition-colors text-sm">Login</Link></li>
            </ul>
          </div>
        </div>
        
        <div className="border-t border-gray-800 mt-12 pt-8 flex flex-col md:flex-row justify-between items-center">
          <p className="text-gray-400 text-sm">© {new Date().getFullYear()} Findr. All rights reserved.</p>
          <div className="flex space-x-6 mt-4 md:mt-0">
            <a href="mailto:support@findr.ae" className="text-gray-400 hover:text-primary transition-colors text-sm flex items-center">
              <Mail size={14} className="mr-1" /> Contact
            </a>
          </div>
        </div>
      </div>
    </footer>
  );
};

export default Footer;