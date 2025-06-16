import { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { auth } from '../../firebase';
import { GoogleAuthProvider, signInWithPopup, User as FirebaseUser, signInWithCustomToken } from 'firebase/auth';

interface User {
  id: string;
  name: string;
  email: string;
  providerId?: string;
}

interface AuthContextType {
  user: User | null;
  isAuthenticated: boolean;
  login: (email: string, password: string, provider?: string) => Promise<void>;
  register: (name: string, email: string, password: string, provider?: string) => Promise<void>;
  logout: () => void;
  updateUser: (data: Partial<User>) => void;
  signInWithGoogle: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider = ({ children }: { children: ReactNode }) => {
  const [user, setUser] = useState<User | null>(null);
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  
  useEffect(() => {
    // Check for saved user in localStorage
    const savedUser = localStorage.getItem('user');
    if (savedUser) {
      setUser(JSON.parse(savedUser));
      setIsAuthenticated(true);
    }
  }, []);
  
  const login = async (email: string, password: string): Promise<void> => {
    const response = await fetch('http://localhost:8081/auth/login', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ email, password }),
    });

    if (response.ok) {
      const token = await response.text();
      const userCredential = await signInWithCustomToken(auth, token);
      const firebaseUser: FirebaseUser = userCredential.user;
      const newUser: User = {
        id: firebaseUser.uid,
        name: firebaseUser.displayName || email.split('@')[0],
        email: firebaseUser.email || email,
        providerId: 'password',
      };
      setUser(newUser);
      setIsAuthenticated(true);
      localStorage.setItem('user', JSON.stringify(newUser));
    } else {
      throw new Error('Login failed');
    }
  };

  const register = async (name: string, email: string, password: string): Promise<void> => {
    const response = await fetch('http://localhost:8081/auth/register', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ displayName: name, email, password }),
    });

    if (response.ok) {
      const userRecord = await response.json();
      const newUser = {
        id: userRecord.uid,
        name: userRecord.displayName,
        email: userRecord.email,
        providerId: 'password',
      };
      setUser(newUser);
      setIsAuthenticated(true);
      localStorage.setItem('user', JSON.stringify(newUser));
    } else {
      throw new Error('Registration failed');
    }
  };
  
  const logout = () => {
    setUser(null);
    setIsAuthenticated(false);
    localStorage.removeItem('user');
  };
  
  const updateUser = (data: Partial<User>) => {
    if (user) {
      const updatedUser = { ...user, ...data };
      setUser(updatedUser);
      localStorage.setItem('user', JSON.stringify(updatedUser));
    }
  };

  const signInWithGoogle = async () => {
    const provider = new GoogleAuthProvider();
    try {
      const result = await signInWithPopup(auth, provider);
      const firebaseUser: FirebaseUser = result.user;
      const newUser: User = {
        id: firebaseUser.uid,
        name: firebaseUser.displayName || 'Anonymous',
        email: firebaseUser.email || '',
        providerId: result.providerId || 'google.com',
      };
      setUser(newUser);
      setIsAuthenticated(true);
      localStorage.setItem('user', JSON.stringify(newUser));
    } catch (error) {
      console.error("Google sign-in error:", error);
      throw new Error('Failed to sign in with Google');
    }
  };
  
  return (
    <AuthContext.Provider value={{ user, isAuthenticated, login, register, logout, updateUser, signInWithGoogle }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
