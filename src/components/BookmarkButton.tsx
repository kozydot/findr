import { useState, useEffect } from 'react';
import { Bookmark } from 'lucide-react';
import { useBookmarks } from '../context/BookmarkContext';
import { Product } from '../types';

interface BookmarkButtonProps {
  product: Product;
}

const BookmarkButton = ({ product }: BookmarkButtonProps) => {
  const { addBookmark, removeBookmark, isBookmarked } = useBookmarks();
  const [isBookmarkedState, setIsBookmarkedState] = useState(isBookmarked(product.id));

  useEffect(() => {
    setIsBookmarkedState(isBookmarked(product.id));
  }, [isBookmarked, product.id]);

  const toggleBookmark = () => {
    if (isBookmarkedState) {
      removeBookmark(product.id);
    } else {
      addBookmark(product);
    }
  };

  return (
    <button
      onClick={toggleBookmark}
      className={`p-2 rounded-full transition-colors ${
        isBookmarkedState
          ? 'bg-primary text-white'
          : 'bg-gray-200/50 dark:bg-gray-700/50 text-gray-600 dark:text-gray-300 hover:text-primary dark:hover:text-primary'
      }`}
    >
      <Bookmark size={18} fill={isBookmarkedState ? 'currentColor' : 'none'} />
    </button>
  );
};

export default BookmarkButton;