import React, { useState, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { 
  LayoutDashboard, 
  FileText, 
  GitBranch, 
  FileCheck, 
  Settings,
  Cloud,
  ChevronLeft,
  ChevronRight,
  Upload,
  Camera,
  LogOut,
  User,
  Shield,
  Crown,
  BarChart3  // ✅ ADDED: Analytics icon
} from 'lucide-react';
import { Avatar, AvatarImage, AvatarFallback } from '../ui/avatar';
import { Button } from '../ui/button';
import { Badge } from '../ui/badge';
import userService from '../../services/userService';

interface UserProfile {
  id: number;
  fullName: string;
  username: string;
  email: string;
  role: 'USER' | 'MANAGER' | 'ADMIN';
  profilePicture?: string;
}

const Sidebar: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const [isCollapsed, setIsCollapsed] = useState(false);
  const [user, setUser] = useState<UserProfile | null>(null);
  const [showProfileMenu, setShowProfileMenu] = useState(false);
  const [uploadingAvatar, setUploadingAvatar] = useState(false);

  // ✅ UPDATED: Added Analytics to sidebar navigation items
  const sidebarItems = [
    { name: 'Dashboard', icon: LayoutDashboard, href: '/dashboard' },
    { name: 'Analytics', icon: BarChart3, href: '/analytics' },  // ✅ NEW: Analytics Dashboard
    { name: 'Documents', icon: FileText, href: '/documents' },
    { name: 'Workflow', icon: GitBranch, href: '/workflow' },
    { name: 'Audit Trail', icon: FileCheck, href: '/audit-trail' },
    { name: 'Settings', icon: Settings, href: '/settings' }
  ];

  // Load user profile on component mount
  useEffect(() => {
    loadUserProfile();
  }, []);

  const loadUserProfile = async () => {
    try {
      const userProfile = await userService.getCurrentUser();
      setUser(userProfile);
    } catch (error) {
      console.error('Failed to load user profile:', error);
    }
  };

  const handleSidebarNavigation = (href: string) => {
    navigate(href);
  };

  const getProfileImageUrl = (profilePicture?: string) => {
    if (!profilePicture) {
      return '/default-avatar.png';
    }
    
    const baseUrl = process.env.REACT_APP_BACKEND_URL || 'http://localhost:8080';
    const imageUrl = `${baseUrl}/api/users/profile/picture/${profilePicture}`;
    
    // Add cache busting to force fresh image load
    const cacheBuster = `?cb=${Date.now()}`;
    
    console.log('🔍 Sidebar Image URL:', imageUrl + cacheBuster);
    return imageUrl + cacheBuster;
  };

  // ✅ ENHANCED: Upload handler that refreshes all images
  const handleProfilePictureUpload = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;

    if (!file.type.startsWith('image/')) {
      alert('Please select an image file');
      return;
    }

    if (file.size > 5 * 1024 * 1024) {
      alert('File size must be less than 5MB');
      return;
    }

    try {
      setUploadingAvatar(true);
      const updatedUser = await userService.uploadProfilePicture(file);
      setUser(updatedUser);
      setShowProfileMenu(false);

      // ✅ CRITICAL: Force refresh all profile images across the app
      setTimeout(() => {
        const profileImages = document.querySelectorAll('img[src*="profile/picture"]');
        profileImages.forEach((img) => {
          const imageElement = img as HTMLImageElement;
          const baseSrc = imageElement.src.split('?')[0];
          imageElement.src = `${baseSrc}?cb=${Date.now()}`;
        });
      }, 500);

    } catch (error) {
      console.error('Failed to upload profile picture:', error);
      alert('Failed to upload profile picture');
    } finally {
      setUploadingAvatar(false);
    }
  };

  const handleLogout = async () => {
    try {
      await userService.logout();
      navigate('/login');
    } catch (error) {
      console.error('Logout failed:', error);
    }
  };

  // ✅ UPDATED: Enhanced route matching to include analytics
  const isActiveRoute = (href: string) => {
    if (href === '/documents') {
      return location.pathname === '/documents' || location.pathname.startsWith('/documents/');
    }
    if (href === '/analytics') {
      return location.pathname === '/analytics' || location.pathname.startsWith('/analytics');
    }
    if (href === '/audit-trail') {
      return location.pathname === '/audit-trail' || location.pathname === '/audit';
    }
    return location.pathname === href;
  };

  const getRoleIcon = (role: string) => {
    switch (role) {
      case 'ADMIN': return <Crown className="w-4 h-4 text-yellow-400" />;
      case 'MANAGER': return <Shield className="w-4 h-4 text-blue-400" />;
      default: return <User className="w-4 h-4 text-gray-400" />;
    }
  };

  const getRoleBadge = (role: string) => {
    const configs = {
      ADMIN: { color: 'bg-yellow-500 text-white', label: 'Admin' },
      MANAGER: { color: 'bg-blue-500 text-white', label: 'Manager' },
      USER: { color: 'bg-gray-500 text-white', label: 'User' }
    };
    const config = configs[role as keyof typeof configs] || configs.USER;
    
    return (
      <Badge className={`${config.color} text-xs px-2 py-1 font-medium`}>
        {config.label}
      </Badge>
    );
  };

  const sidebarWidth = isCollapsed ? 'w-20' : 'w-72';
  const sidebarTransition = 'transition-all duration-300 ease-in-out';

  return (
    <aside className={`${sidebarWidth} ${sidebarTransition} bg-gradient-to-b from-blue-900 via-blue-800 to-blue-900 text-white flex flex-col shadow-2xl relative`}>
      
      {/* Collapse Toggle Button */}
      <button
        onClick={() => setIsCollapsed(!isCollapsed)}
        className="absolute -right-3 top-6 bg-white text-blue-900 rounded-full p-1.5 shadow-lg hover:shadow-xl transition-all duration-200 hover:scale-110 z-10 border-2 border-blue-100"
      >
        {isCollapsed ? (
          <ChevronRight className="w-4 h-4" />
        ) : (
          <ChevronLeft className="w-4 h-4" />
        )}
      </button>

      {/* Logo */}
      <div className={`p-6 border-b border-blue-700/50 ${isCollapsed ? 'px-4' : ''}`}>
        <div className={`flex items-center ${isCollapsed ? 'justify-center' : ''}`}>
          <div className="bg-white/10 p-2 rounded-lg backdrop-blur-sm">
            <Cloud className="w-6 h-6 text-white" />
          </div>
          {!isCollapsed && (
            <h1 className="text-xl font-semibold text-white ml-3">CloudDocs</h1>
          )}
        </div>
      </div>

      {/* Navigation */}
      <nav className="flex-1 p-4">
        <ul className="space-y-2">
          {sidebarItems.map((item) => {
            const IconComponent = item.icon;
            const isActive = isActiveRoute(item.href);
            return (
              <li key={item.name}>
                <button
                  onClick={() => handleSidebarNavigation(item.href)}
                  className={`w-full flex items-center px-4 py-3 rounded-xl transition-all duration-200 text-left group relative ${
                    isActive
                      ? 'bg-white/20 text-white shadow-lg backdrop-blur-sm'
                      : 'text-blue-100 hover:bg-white/10 hover:text-white'
                  }`}
                  title={isCollapsed ? item.name : undefined}
                >
                  <IconComponent className={`w-5 h-5 flex-shrink-0 ${isCollapsed ? 'mx-auto' : 'mr-3'}`} />
                  {!isCollapsed && (
                    <span className="text-base font-medium">{item.name}</span>
                  )}
                  
                  {/* Active indicator */}
                  {isActive && (
                    <div className="absolute right-0 top-1/2 transform -translate-y-1/2 w-1 h-8 bg-white rounded-l-full"></div>
                  )}
                  
                  {/* Tooltip for collapsed state */}
                  {isCollapsed && (
                    <div className="absolute left-full ml-3 px-3 py-2 bg-gray-900 text-white text-sm rounded-lg opacity-0 group-hover:opacity-100 transition-opacity duration-200 pointer-events-none whitespace-nowrap z-50">
                      {item.name}
                    </div>
                  )}
                </button>
              </li>
            );
          })}
        </ul>
      </nav>

      {/* User Profile Section */}
      <div className="p-4 border-t border-blue-700/50">
        <div className={`relative ${isCollapsed ? 'flex justify-center' : ''}`}>
          
          {/* Profile Button */}
          <button
            onClick={() => setShowProfileMenu(!showProfileMenu)}
            className={`w-full flex items-center space-x-3 p-3 rounded-xl hover:bg-white/10 transition-all duration-200 group ${
              isCollapsed ? 'justify-center p-2' : ''
            }`}
          >
            {/* Avatar with Upload Indicator */}
            <div className="relative">
              <Avatar className={`${isCollapsed ? 'w-10 h-10' : 'w-12 h-12'} ring-2 ring-white/30 shadow-lg`}>
                 <AvatarImage 
                  src={getProfileImageUrl(user?.profilePicture)}
                  alt={user?.fullName}
                  onLoad={() => console.log('✅ Sidebar image loaded')}
                  onError={(e) => {
                    console.error('❌ Sidebar image failed:', e.currentTarget.src);
                    e.currentTarget.src = '/default-avatar.png';
                  }}
                />
                <AvatarFallback className="bg-gradient-to-br from-blue-600 to-blue-700 text-white font-semibold">
                  {user?.fullName?.split(' ').map(n => n[0]).join('').toUpperCase() || 'U'}
                </AvatarFallback>
              </Avatar>
              
              {/* Upload indicator */}
              <div className="absolute -bottom-1 -right-1 bg-green-500 rounded-full p-1">
                <Camera className="w-3 h-3 text-white" />
              </div>
            </div>

            {/* User Info */}
            {!isCollapsed && (
              <div className="flex-1 text-left">
                <div className="flex items-center justify-between">
                  <p className="font-semibold text-white text-sm truncate">
                    {user?.fullName || 'Loading...'}
                  </p>
                  {user?.role && getRoleIcon(user.role)}
                </div>
                <div className="flex items-center space-x-2 mt-1">
                  {user?.role && getRoleBadge(user.role)}
                </div>
              </div>
            )}
            
            {/* Tooltip for collapsed state */}
            {isCollapsed && user && (
              <div className="absolute left-full ml-3 px-3 py-2 bg-gray-900 text-white text-sm rounded-lg opacity-0 group-hover:opacity-100 transition-opacity duration-200 pointer-events-none whitespace-nowrap z-50">
                <div className="font-semibold">{user.fullName}</div>
                <div className="text-xs text-gray-300">{user.role}</div>
              </div>
            )}
          </button>

          {/* Profile Dropdown Menu */}
          {showProfileMenu && !isCollapsed && (
            <div className="absolute bottom-full left-0 right-0 mb-2 bg-white rounded-xl shadow-2xl border border-gray-200 overflow-hidden z-50">
              
              {/* User Info Header */}
              <div className="bg-gradient-to-r from-blue-600 to-blue-700 p-4 text-white">
                <div className="flex items-center space-x-3">
                  <Avatar className="w-12 h-12 ring-2 ring-white/30">
                    <AvatarImage 
                      src={getProfileImageUrl(user?.profilePicture)}
                      alt={user?.fullName}
                    />
                    <AvatarFallback className="bg-white/20 text-white">
                      {user?.fullName?.split(' ').map(n => n[0]).join('').toUpperCase() || 'U'}
                    </AvatarFallback>
                  </Avatar>
                  <div>
                    <p className="font-semibold">{user?.fullName}</p>
                    <p className="text-sm text-blue-100">{user?.email}</p>
                  </div>
                </div>
              </div>

              {/* Menu Options */}
              <div className="p-2">
                
                {/* Upload Profile Picture */}
                <label className="flex items-center space-x-3 px-3 py-2 hover:bg-gray-50 rounded-lg cursor-pointer transition-colors duration-200">
                  <input
                    type="file"
                    accept="image/*"
                    onChange={handleProfilePictureUpload}
                    className="hidden"
                    disabled={uploadingAvatar}
                  />
                  {uploadingAvatar ? (
                    <div className="animate-spin rounded-full h-5 w-5 border-b-2 border-blue-600"></div>
                  ) : (
                    <Upload className="w-5 h-5 text-gray-600" />
                  )}
                  <span className="text-sm font-medium text-gray-700">
                    {uploadingAvatar ? 'Uploading...' : 'Change Photo'}
                  </span>
                </label>

                {/* View Profile */}
                <button
                  onClick={() => {
                    navigate('/profile');
                    setShowProfileMenu(false);
                  }}
                  className="w-full flex items-center space-x-3 px-3 py-2 hover:bg-gray-50 rounded-lg transition-colors duration-200"
                >
                  <User className="w-5 h-5 text-gray-600" />
                  <span className="text-sm font-medium text-gray-700">View Profile</span>
                </button>

                {/* Settings */}
                <button
                  onClick={() => {
                    navigate('/settings');
                    setShowProfileMenu(false);
                  }}
                  className="w-full flex items-center space-x-3 px-3 py-2 hover:bg-gray-50 rounded-lg transition-colors duration-200"
                >
                  <Settings className="w-5 h-5 text-gray-600" />
                  <span className="text-sm font-medium text-gray-700">Settings</span>
                </button>

                {/* Divider */}
                <div className="border-t border-gray-200 my-2"></div>

                {/* Logout */}
                <button
                  onClick={handleLogout}
                  className="w-full flex items-center space-x-3 px-3 py-2 hover:bg-red-50 text-red-600 hover:text-red-700 rounded-lg transition-colors duration-200"
                >
                  <LogOut className="w-5 h-5" />
                  <span className="text-sm font-medium">Sign Out</span>
                </button>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Click outside to close menu */}
      {showProfileMenu && (
        <div
          className="fixed inset-0 z-40"
          onClick={() => setShowProfileMenu(false)}
        />
      )}
    </aside>
  );
};

export default Sidebar;
