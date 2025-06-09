import { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { Bell, User, Mail, Lock, LogOut, Save, AlertTriangle } from 'lucide-react';
import { useAlerts } from '../context/AlertContext';

const ProfilePage = () => {
  const { user, logout, updateUser } = useAuth();
  const { alerts } = useAlerts();
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
  
  return (
    <div className="container mx-auto px-4 py-8">
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-secondary">My Account</h1>
        <p className="text-gray-600 mt-2">
          Manage your profile and preferences
        </p>
      </div>
      
      <div className="grid grid-cols-1 lg:grid-cols-4 gap-8">
        {/* Sidebar */}
        <div className="lg:col-span-1">
          <div className="bg-white rounded-xl shadow-sm overflow-hidden">
            <div className="p-6 border-b border-gray-100">
              <div className="flex items-center">
                <div className="w-12 h-12 rounded-full bg-primary/10 flex items-center justify-center text-primary text-xl font-bold">
                  {user?.name?.charAt(0) || <User size={24} />}
                </div>
                <div className="ml-3">
                  <h3 className="font-medium">{user?.name}</h3>
                  <p className="text-gray-500 text-sm">{user?.email}</p>
                </div>
              </div>
            </div>
            
            <nav className="p-2">
              <button
                onClick={() => setActiveTab('account')}
                className={`flex items-center w-full text-left px-4 py-3 rounded-lg transition-colors ${
                  activeTab === 'account' 
                    ? 'bg-primary/10 text-primary' 
                    : 'text-gray-700 hover:bg-gray-50'
                }`}
              >
                <User size={18} className="mr-3" />
                Account Information
              </button>
              
              <button
                onClick={() => setActiveTab('notifications')}
                className={`flex items-center w-full text-left px-4 py-3 rounded-lg transition-colors ${
                  activeTab === 'notifications' 
                    ? 'bg-primary/10 text-primary' 
                    : 'text-gray-700 hover:bg-gray-50'
                }`}
              >
                <Bell size={18} className="mr-3" />
                Notifications
                {alerts.length > 0 && (
                  <span className="ml-auto bg-primary text-white text-xs rounded-full w-5 h-5 flex items-center justify-center">
                    {alerts.length}
                  </span>
                )}
              </button>
              
              <button
                onClick={() => setActiveTab('security')}
                className={`flex items-center w-full text-left px-4 py-3 rounded-lg transition-colors ${
                  activeTab === 'security' 
                    ? 'bg-primary/10 text-primary' 
                    : 'text-gray-700 hover:bg-gray-50'
                }`}
              >
                <Lock size={18} className="mr-3" />
                Security
              </button>
              
              <button
                onClick={() => setShowConfirmLogout(true)}
                className="flex items-center w-full text-left px-4 py-3 rounded-lg text-gray-700 hover:bg-gray-50 transition-colors"
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
            <div className="bg-white rounded-xl shadow-sm p-6">
              <div className="flex justify-between items-center mb-6">
                <h2 className="text-xl font-semibold">Account Information</h2>
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
                    <label htmlFor="name" className="block text-sm font-medium text-gray-700 mb-1">
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
                    <label htmlFor="email" className="block text-sm font-medium text-gray-700 mb-1">
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
              
              <div className="mt-8 pt-6 border-t border-gray-100">
                <h3 className="font-medium mb-4">Connected Accounts</h3>
                <div className="space-y-3">
                  <div className="flex items-center justify-between p-3 border border-gray-200 rounded-lg">
                    <div className="flex items-center">
                      <img 
                        src="https://www.gstatic.com/firebasejs/ui/2.0.0/images/auth/google.svg" 
                        alt="Google"
                        className="w-5 h-5 mr-3"
                      />
                      <span>Google</span>
                    </div>
                    <span className="text-sm text-green-600">Connected</span>
                  </div>
                  
                  <div className="flex items-center justify-between p-3 border border-gray-200 rounded-lg">
                    <div className="flex items-center">
                      <svg viewBox="0 0 24 24" className="w-5 h-5 mr-3 text-[#1877F2]" fill="currentColor">
                        <path d="M9.19795 21.5H13.198V13.4901H16.8021L17.198 9.50977H13.198V7.5C13.198 6.94772 13.6457 6.5 14.198 6.5H17.198V2.5H14.198C11.4365 2.5 9.19795 4.73858 9.19795 7.5V9.50977H7.19795L6.80206 13.4901H9.19795V21.5Z" />
                      </svg>
                      <span>Facebook</span>
                    </div>
                    <button className="text-sm text-primary hover:underline">Connect</button>
                  </div>
                </div>
              </div>
            </div>
          )}
          
          {/* Notifications Tab */}
          {activeTab === 'notifications' && (
            <div className="bg-white rounded-xl shadow-sm p-6">
              <h2 className="text-xl font-semibold mb-6">Notification Settings</h2>
              
              <div className="space-y-6">
                <div>
                  <h3 className="font-medium mb-3">Price Alerts</h3>
                  <div className="space-y-3">
                    <label className="flex items-center justify-between p-3 border border-gray-200 rounded-lg">
                      <div>
                        <span className="font-medium">Price Drop Alerts</span>
                        <p className="text-sm text-gray-500">Get notified when a product price drops below your target</p>
                      </div>
                      <div className="relative">
                        <input
                          type="checkbox"
                          checked={notificationSettings.priceAlerts}
                          onChange={() => handleToggleNotification('priceAlerts')}
                          className="sr-only"
                        />
                        <div
                          className={`w-12 h-6 rounded-full transition ${
                            notificationSettings.priceAlerts ? 'bg-primary' : 'bg-gray-300'
                          }`}
                        >
                          <div
                            className={`transform transition w-6 h-6 rounded-full bg-white shadow-md ${
                              notificationSettings.priceAlerts ? 'translate-x-6' : 'translate-x-0'
                            }`}
                          ></div>
                        </div>
                      </div>
                    </label>
                    
                    <label className="flex items-center justify-between p-3 border border-gray-200 rounded-lg">
                      <div>
                        <span className="font-medium">Significant Price Drops</span>
                        <p className="text-sm text-gray-500">Get notified when prices drop significantly ({'>'}20%)</p>
                      </div>
                      <div className="relative">
                        <input
                          type="checkbox"
                          checked={notificationSettings.priceDrops}
                          onChange={() => handleToggleNotification('priceDrops')}
                          className="sr-only"
                        />
                        <div
                          className={`w-12 h-6 rounded-full transition ${
                            notificationSettings.priceDrops ? 'bg-primary' : 'bg-gray-300'
                          }`}
                        >
                          <div
                            className={`transform transition w-6 h-6 rounded-full bg-white shadow-md ${
                              notificationSettings.priceDrops ? 'translate-x-6' : 'translate-x-0'
                            }`}
                          ></div>
                        </div>
                      </div>
                    </label>
                  </div>
                </div>
                
                <div>
                  <h3 className="font-medium mb-3">Email Preferences</h3>
                  <div className="space-y-3">
                    <label className="flex items-center justify-between p-3 border border-gray-200 rounded-lg">
                      <div>
                        <span className="font-medium">Deal of the Day</span>
                        <p className="text-sm text-gray-500">Receive a daily email with the best deal of the day</p>
                      </div>
                      <div className="relative">
                        <input
                          type="checkbox"
                          checked={notificationSettings.dealOfTheDay}
                          onChange={() => handleToggleNotification('dealOfTheDay')}
                          className="sr-only"
                        />
                        <div
                          className={`w-12 h-6 rounded-full transition ${
                            notificationSettings.dealOfTheDay ? 'bg-primary' : 'bg-gray-300'
                          }`}
                        >
                          <div
                            className={`transform transition w-6 h-6 rounded-full bg-white shadow-md ${
                              notificationSettings.dealOfTheDay ? 'translate-x-6' : 'translate-x-0'
                            }`}
                          ></div>
                        </div>
                      </div>
                    </label>
                    
                    <label className="flex items-center justify-between p-3 border border-gray-200 rounded-lg">
                      <div>
                        <span className="font-medium">Weekly Newsletter</span>
                        <p className="text-sm text-gray-500">Weekly summary of the best deals and price drops</p>
                      </div>
                      <div className="relative">
                        <input
                          type="checkbox"
                          checked={notificationSettings.newsletter}
                          onChange={() => handleToggleNotification('newsletter')}
                          className="sr-only"
                        />
                        <div
                          className={`w-12 h-6 rounded-full transition ${
                            notificationSettings.newsletter ? 'bg-primary' : 'bg-gray-300'
                          }`}
                        >
                          <div
                            className={`transform transition w-6 h-6 rounded-full bg-white shadow-md ${
                              notificationSettings.newsletter ? 'translate-x-6' : 'translate-x-0'
                            }`}
                          ></div>
                        </div>
                      </div>
                    </label>
                  </div>
                </div>
                
                <div className="flex justify-end">
                  <button
                    className="btn btn-primary flex items-center"
                  >
                    <Save size={18} className="mr-2" />
                    Save Preferences
                  </button>
                </div>
              </div>
            </div>
          )}
          
          {/* Security Tab */}
          {activeTab === 'security' && (
            <div className="bg-white rounded-xl shadow-sm p-6">
              <h2 className="text-xl font-semibold mb-6">Security Settings</h2>
              
              <div className="space-y-6">
                <div>
                  <h3 className="font-medium mb-3">Change Password</h3>
                  <form className="space-y-4">
                    <div>
                      <label htmlFor="currentPassword" className="block text-sm font-medium text-gray-700 mb-1">
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
                      <label htmlFor="newPassword" className="block text-sm font-medium text-gray-700 mb-1">
                        New Password
                      </label>
                      <input
                        id="newPassword"
                        type="password"
                        className="input"
                        placeholder="Enter your new password"
                      />
                      <p className="mt-1 text-xs text-gray-500">
                        Must be at least 8 characters with a mix of letters, numbers, and symbols
                      </p>
                    </div>
                    
                    <div>
                      <label htmlFor="confirmPassword" className="block text-sm font-medium text-gray-700 mb-1">
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
                
                <div className="pt-6 border-t border-gray-100">
                  <h3 className="font-medium mb-3">Two-Factor Authentication</h3>
                  <div className="flex items-center justify-between p-4 bg-gray-50 rounded-lg">
                    <div>
                      <p className="font-medium">Enhance your account security</p>
                      <p className="text-sm text-gray-600 mt-1">
                        Add an extra layer of security to your account by enabling two-factor authentication.
                      </p>
                    </div>
                    <button className="btn btn-outline text-sm">
                      Enable
                    </button>
                  </div>
                </div>
                
                <div className="pt-6 border-t border-gray-100">
                  <h3 className="font-medium text-error mb-3">Danger Zone</h3>
                  <div className="p-4 border border-error/20 rounded-lg bg-error/5">
                    <h4 className="font-medium">Delete Account</h4>
                    <p className="text-sm text-gray-600 mt-1 mb-3">
                      Once you delete your account, there is no going back. This action cannot be undone.
                    </p>
                    <button className="btn bg-white border border-error text-error hover:bg-error/10 text-sm">
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