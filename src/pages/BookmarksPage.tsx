import { useBookmarks } from '../context/BookmarkContext';
import ProductCard from '../components/ProductCard';
import { Link } from 'react-router-dom';

const BookmarksPage = () => {
  const { bookmarks } = useBookmarks();

  return (
    <div className="container mx-auto px-4 py-8">
      <h1 className="text-3xl font-bold text-secondary dark:text-white mb-8">My Bookmarks</h1>
      {bookmarks.length > 0 ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-6">
          {bookmarks.map((product, index) => (
            <div key={product.id} className="animate-fadeIn" style={{ animationDelay: `${index * 100}ms` }}>
              <ProductCard product={product} />
            </div>
          ))}
        </div>
      ) : (
        <div className="text-center py-20">
          <h2 className="text-2xl font-semibold text-secondary dark:text-white mb-4">You haven't bookmarked any items yet.</h2>
          <p className="text-gray-500 dark:text-gray-400 mb-8">
            Click the bookmark icon on any product to save it for later.
          </p>
          <Link to="/" className="btn btn-primary">
            Browse Products
          </Link>
        </div>
      )}
    </div>
  );
};

export default BookmarksPage;