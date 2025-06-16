import { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { Product } from '../types';
import { useAuth } from './AuthContext';

interface BookmarkContextType {
  bookmarks: Product[];
  addBookmark: (product: Product) => void;
  removeBookmark: (productId: string) => void;
  isBookmarked: (productId: string) => boolean;
}

const BookmarkContext = createContext<BookmarkContextType | undefined>(undefined);

export const BookmarkProvider = ({ children }: { children: ReactNode }) => {
  const [bookmarks, setBookmarks] = useState<Product[]>([]);
  const { user } = useAuth();

  useEffect(() => {
    const fetchBookmarks = async () => {
      if (user) {
        const response = await fetch(`/api/v1/products/bookmarks/${user.id}`);
        if (response.ok) {
          const data = await response.json();
          setBookmarks(data);
        }
      } else {
        setBookmarks([]);
      }
    };
    fetchBookmarks();
  }, [user]);


  const addBookmark = async (product: Product) => {
    if (user) {
      setBookmarks(prev => [...prev, product]);
      await fetch(`/api/v1/products/bookmarks/${user.id}/${product.id}`, { method: 'POST' });
    }
  };

  const removeBookmark = async (productId: string) => {
    if (user) {
      setBookmarks(prev => prev.filter(bookmark => bookmark.id !== productId));
      await fetch(`/api/v1/products/bookmarks/${user.id}/${productId}`, { method: 'DELETE' });
    }
  };

  const isBookmarked = (productId: string): boolean => {
    return bookmarks.some(bookmark => bookmark.id === productId);
  };

  return (
    <BookmarkContext.Provider value={{ bookmarks, addBookmark, removeBookmark, isBookmarked }}>
      {children}
    </BookmarkContext.Provider>
  );
};

export const useBookmarks = () => {
  const context = useContext(BookmarkContext);
  if (context === undefined) {
    throw new Error('useBookmarks must be used within a BookmarkProvider');
  }
  return context;
};