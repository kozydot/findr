import { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { Bell, User, Mail, Lock, LogOut, Save, AlertTriangle } from 'lucide-react';

const ProfilePage = () => {
  const { user, logout, updateUser } = useAuth();
  const [activeTab, setActiveTab] = useState<'account' | 'notifications' | 'security'>('account');
  const [isEditing, setIsEditing] = useState(false);
  const [showConfirmLogout, setShowConfirmLogout] = useState(false);
  
  // Form states
  const [name, setName] = useState(user?.name || '');
  const [email, setEmail] = useState(user?.email || '');
  
  // Notification settings
  const [notificationSettings, setNotificationSettings] = useState({
    priceAlerts: true,
    priceDrops: true,
    dealOfTheDay: true,
    newsletter: false,
  });
  
  const handleUpdateProfile = (e: React.FormEvent) => {
    e.preventDefault();
    updateUser({ name, email });
    setIsEditing(false);
  };
  
  const handleToggleNotification = (key: keyof typeof notificationSettings) => {
    setNotificationSettings(prev => ({
      ...prev,
      [key]: !prev[key]
    }));
  };
  
  const confirmLogout = () => {
    logout();
    setShowConfirmLogout(false);
  };

  const handleChangePassword = async (e: React.FormEvent) => {
    e.preventDefault();
    const form = e.target as HTMLFormElement;
    const newPassword = (form.elements.namedItem('newPassword') as HTMLInputElement).value;
    const confirmPassword = (form.elements.namedItem('confirmPassword') as HTMLInputElement).value;

    if (newPassword !== confirmPassword) {
      alert("Passwords do not match");
      return;
    }

    if (user) {
      try {
        await fetch(`/auth/password/${user.id}`, {
          method: 'PUT',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ password: newPassword }),
        });
        alert("Password updated successfully");
      } catch (error) {
        alert("Failed to update password");
      }
    }
  };

  const handleDeleteAccount = async () => {
    if (user && window.confirm("Are you sure you want to delete your account? This action cannot be undone.")) {
      try {
        await fetch(`http://localhost:8081/auth/${user.id}`, { method: 'DELETE' });
        logout();
        alert("Account deleted successfully");
        window.location.href = "/";
      } catch (error) {
        alert("Failed to delete account");
      }
    }
  };
  
  return (
    <div className="container mx-auto px-4 py-8">
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-secondary dark:text-white">My Account</h1>
        <p className="text-gray-600 dark:text-gray-400 mt-2">
          Manage your profile and preferences
        </p>
      </div>
      
      <div className="grid grid-cols-1 lg:grid-cols-4 gap-8">
        {/* Sidebar */}
        <div className="lg:col-span-1">
          <div className="bg-white dark:bg-gray-800 rounded-xl shadow-sm overflow-hidden">
            <div className="p-6 border-b border-gray-100 dark:border-gray-700">
              <div className="flex items-center">
                <div className="w-12 h-12 rounded-full bg-primary/10 flex items-center justify-center text-primary text-xl font-bold">
                  {user?.name?.charAt(0) || <User size={24} />}
                </div>
                <div className="ml-3">
                  <h3 className="font-medium dark:text-white">{user?.name}</h3>
                  <p className="text-gray-500 dark:text-gray-400 text-sm">{user?.email}</p>
                </div>
              </div>
            </div>
            
            <nav className="p-2">
              <button
                onClick={() => setActiveTab('account')}
                className={`flex items-center w-full text-left px-4 py-3 rounded-lg transition-colors ${
                  activeTab === 'account'
                    ? 'bg-primary/10 text-primary'
                    : 'text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700'
                }`}
              >
                <User size={18} className="mr-3" />
                Account Information
              </button>
              
              
              <button
                onClick={() => setActiveTab('security')}
                className={`flex items-center w-full text-left px-4 py-3 rounded-lg transition-colors ${
                  activeTab === 'security'
                    ? 'bg-primary/10 text-primary'
                    : 'text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700'
                }`}
              >
                <Lock size={18} className="mr-3" />
                Security
              </button>
              
              <button
                onClick={() => setShowConfirmLogout(true)}
                className="flex items-center w-full text-left px-4 py-3 rounded-lg text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors"
              >
                <LogOut size={18} className="mr-3" />
                Logout
              </button>
            </nav>
          </div>
        </div>
        
        {/* Main content */}
        <div className="lg:col-span-3">
          {/* Account Information Tab */}
          {activeTab === 'account' && (
            <div className="bg-white dark:bg-gray-800 rounded-xl shadow-sm p-6">
              <div className="flex justify-between items-center mb-6">
                <h2 className="text-xl font-semibold dark:text-white">Account Information</h2>
                <button
                  onClick={() => setIsEditing(!isEditing)}
                  className="text-sm text-primary hover:underline"
                >
                  {isEditing ? 'Cancel' : 'Edit'}
                </button>
              </div>
              
              <form onSubmit={handleUpdateProfile}>
                <div className="space-y-5">
                  <div>
                    <label htmlFor="name" className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                      Full Name
                    </label>
                    <div className="relative">
                      <User size={18} className="absolute top-1/2 transform -translate-y-1/2 left-3 text-gray-400" />
                      <input
                        id="name"
                        type="text"
                        value={name}
                        onChange={(e) => setName(e.target.value)}
                        className="input pl-10"
                        disabled={!isEditing}
                      />
                    </div>
                  </div>
                  
                  <div>
                    <label htmlFor="email" className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                      Email Address
                    </label>
                    <div className="relative">
                      <Mail size={18} className="absolute top-1/2 transform -translate-y-1/2 left-3 text-gray-400" />
                      <input
                        id="email"
                        type="email"
                        value={email}
                        onChange={(e) => setEmail(e.target.value)}
                        className="input pl-10"
                        disabled={!isEditing}
                      />
                    </div>
                  </div>
                  
                  {isEditing && (
                    <div className="flex justify-end">
                      <button
                        type="submit"
                        className="btn btn-primary flex items-center"
                      >
                        <Save size={18} className="mr-2" />
                        Save Changes
                      </button>
                    </div>
                  )}
                </div>
              </form>
              
              <div className="mt-8 pt-6 border-t border-gray-100 dark:border-gray-700">
                <h3 className="font-medium mb-4 dark:text-white">Connected Accounts</h3>
                <div className="space-y-3">
                  <div className="flex items-center justify-between p-3 border border-gray-200 dark:border-gray-700 rounded-lg">
                    <div className="flex items-center">
                      <img
                        src="https://www.gstatic.com/firebasejs/ui/2.0.0/images/auth/google.svg"
                        alt="Google"
                        className="w-5 h-5 mr-3"
                      />
                      <span className="dark:text-gray-300">Google</span>
                    </div>
                    <span className={`text-sm ${user?.providerId === 'google.com' ? 'text-green-600' : 'text-red-600'}`}>
                      {user?.providerId === 'google.com' ? 'Connected' : 'Connect'}
                    </span>
                  </div>
                </div>
              </div>
            </div>
          )}
          
          {/* Notifications Tab */}
          
          {/* Security Tab */}
          {activeTab === 'security' && (
            <div className="bg-white dark:bg-gray-800 rounded-xl shadow-sm p-6">
              <h2 className="text-xl font-semibold mb-6 dark:text-white">Security Settings</h2>
              
              <div className="space-y-6">
                {user?.providerId !== 'google.com' && (
                  <div>
                    <h3 className="font-medium mb-3 dark:text-white">Change Password</h3>
                    <form className="space-y-4" onSubmit={handleChangePassword}>
                      <div>
                        <label htmlFor="currentPassword" className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                          Current Password
                        </label>
                        <input
                          id="currentPassword"
                          type="password"
                          className="input"
                          placeholder="Enter your current password"
                        />
                      </div>
                      
                      <div>
                        <label htmlFor="newPassword" className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                          New Password
                        </label>
                        <input
                          id="newPassword"
                          type="password"
                          className="input"
                          placeholder="Enter your new password"
                        />
                        <p className="mt-1 text-xs text-gray-500 dark:text-gray-400">
                          Must be at least 8 characters with a mix of letters, numbers, and symbols
                        </p>
                      </div>
                      
                      <div>
                        <label htmlFor="confirmPassword" className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                          Confirm New Password
                        </label>
                        <input
                          id="confirmPassword"
                          type="password"
                          className="input"
                          placeholder="Confirm your new password"
                        />
                      </div>
                      
                      <div className="flex justify-end">
                        <button
                          type="submit"
                          className="btn btn-primary"
                        >
                          Update Password
                        </button>
                      </div>
                    </form>
                  </div>
                )}
                
                <div className="pt-6 border-t border-gray-100 dark:border-gray-700">
                  <h3 className="font-medium text-error mb-3">Danger Zone</h3>
                  <div className="p-4 border border-error/20 rounded-lg bg-error/5">
                    <h4 className="font-medium dark:text-white">Delete Account</h4>
                    <p className="text-sm text-gray-600 dark:text-gray-400 mt-1 mb-3">
                      Once you delete your account, there is no going back. This action cannot be undone.
                    </p>
                    <button className="btn bg-white dark:bg-transparent border border-error text-error hover:bg-error/10 text-sm" onClick={handleDeleteAccount}>
                      Delete My Account
                    </button>
                  </div>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>
      
      {/* Confirmation modal for logout */}
      {showConfirmLogout && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-xl p-6 max-w-md w-full">
            <div className="flex items-start mb-4">
              <AlertTriangle size={24} className="text-warning mr-3 flex-shrink-0" />
              <div>
                <h3 className="text-lg font-semibold mb-2">Confirm Logout</h3>
                <p className="text-gray-600 text-sm">
                  Are you sure you want to log out of your account?
                </p>
              </div>
            </div>
            
            <div className="flex justify-end space-x-3 mt-6">
              <button 
                onClick={() => setShowConfirmLogout(false)}
                className="btn bg-gray-100 text-secondary hover:bg-gray-200"
              >
                Cancel
              </button>
              <button 
                onClick={confirmLogout}
                className="btn btn-primary"
              >
                Yes, Log Out
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default ProfilePage;