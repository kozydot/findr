import { Link } from 'react-router-dom';
import { Home, Search } from 'lucide-react';

const NotFoundPage = () => {
  return (
    <div className="container mx-auto px-4 py-16 min-h-screen flex items-center justify-center">
      <div className="text-center max-w-md">
        <div className="text-9xl font-bold text-primary mb-6">404</div>
        <h1 className="text-3xl font-bold text-secondary mb-4">Page Not Found</h1>
        <p className="text-gray-600 mb-8">
          The page you are looking for might have been removed, had its name changed, or is temporarily unavailable.
        </p>
        <div className="flex flex-col sm:flex-row justify-center gap-4">
          <Link to="/" className="btn btn-primary flex items-center justify-center">
            <Home size={18} className="mr-2" />
            Back to Home
          </Link>
          <Link to="/search" className="btn btn-outline flex items-center justify-center">
            <Search size={18} className="mr-2" />
            Search Products
          </Link>
        </div>
      </div>
    </div>
  );
};

export default NotFoundPage;