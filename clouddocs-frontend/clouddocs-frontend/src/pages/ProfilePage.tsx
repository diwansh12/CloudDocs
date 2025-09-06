import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { 
  User, 
  Mail, 
  Shield, 
  Calendar, 
  Camera,
  Save,
  X,
  Edit3,
  ArrowLeft,
  Settings,
  Check,
  Sparkles
} from 'lucide-react';
import { Button } from '../components/ui/button';
import { Card, CardContent } from '../components/ui/card';
import { Avatar, AvatarImage, AvatarFallback } from '../components/ui/avatar';
import { Badge } from '../components/ui/badge';
import AuthenticatedImage from '../components/AuthenticatedImage'; // ✅ ADDED
import userService from '../services/userService';

interface UserProfile {
  id: number;
  fullName: string;
  username: string;
  email: string;
  role: 'USER' | 'MANAGER' | 'ADMIN';
  profilePicture?: string;
  createdAt: string;
  lastLoginAt?: string;
  active: boolean;
}

export default function ProfilePage() {
  const navigate = useNavigate();
  const [user, setUser] = useState<UserProfile | null>(null);
  const [loading, setLoading] = useState(true);
  const [uploading, setUploading] = useState(false);
  const [editing, setEditing] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  // Edit form state
  const [editForm, setEditForm] = useState({
    fullName: '',
    email: ''
  });

  // ✅ ADDED: Get token for authenticated images
  const token = localStorage.getItem('token');

  useEffect(() => {
    loadUserProfile();
  }, []);

  const loadUserProfile = async () => {
    try {
      setLoading(true);
      const profileData = await userService.getCurrentUser();
      setUser(profileData);
      setEditForm({
        fullName: profileData.fullName || '',
        email: profileData.email || ''
      });
    } catch (err: any) {
      setError('Failed to load profile. Please login again.');
      console.error('Profile load error:', err);
    } finally {
      setLoading(false);
    }
  };

  // ✅ UPDATED: Build authenticated image URL
  const getAuthenticatedImageUrl = (profilePicture?: string) => {
    if (!profilePicture) return undefined;
    
    const baseUrl = process.env.REACT_APP_BACKEND_URL || 'https://clouddocs.onrender.com';
    return `${baseUrl}/api/users/profile/picture/${profilePicture}`;
  };

  const handleProfilePictureUpload = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;

    if (!file.type.startsWith('image/')) {
      setError('Please select an image file');
      return;
    }

    if (file.size > 5 * 1024 * 1024) {
      setError('Image must be less than 5MB');
      return;
    }

    try {
      setUploading(true);
      setError('');
      
      console.log('📸 Uploading file:', file.name);
      
      const response = await userService.uploadProfilePicture(file);
      console.log('📥 Upload response:', response);
      
      const updatedUser = response;
      
      setUser(updatedUser);
      setSuccess('Profile picture updated successfully!');
      setTimeout(() => setSuccess(''), 3000);
      
    } catch (err: any) {
      console.error('❌ Upload failed:', err);
      setError(err.message || 'Failed to upload profile picture');
    } finally {
      setUploading(false);
    }
  };

  const handleUpdateProfile = async () => {
    try {
      setError('');
      const updatedUser = await userService.updateProfile(editForm);
      setUser(updatedUser);
      setEditing(false);
      setSuccess('Profile updated successfully!');
      setTimeout(() => setSuccess(''), 3000);
    } catch (err: any) {
      setError(err.message || 'Failed to update profile');
    }
  };

  const getRoleBadge = (role: string) => {
    const configs = {
      ADMIN: { 
        gradient: 'from-rose-500 to-pink-600', 
        icon: Shield, 
        label: 'Administrator',
        glow: 'shadow-rose-200'
      },
      MANAGER: { 
        gradient: 'from-blue-500 to-indigo-600', 
        icon: User, 
        label: 'Manager',
        glow: 'shadow-blue-200'
      },
      USER: { 
        gradient: 'from-emerald-500 to-teal-600', 
        icon: User, 
        label: 'User',
        glow: 'shadow-emerald-200'
      }
    };
    const config = configs[role as keyof typeof configs] || configs.USER;
    const IconComponent = config.icon;
    
    return (
      <div className={`inline-flex items-center px-4 py-2 rounded-full bg-gradient-to-r ${config.gradient} text-white shadow-lg ${config.glow} backdrop-blur-sm`}>
        <IconComponent className="w-4 h-4 mr-2" />
        <span className="font-semibold text-sm">{config.label}</span>
      </div>
    );
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    });
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-slate-50 via-blue-50 to-indigo-100 flex items-center justify-center">
        <div className="text-center">
          <div className="relative">
            <div className="animate-spin rounded-full h-16 w-16 border-4 border-transparent bg-gradient-to-r from-blue-500 to-purple-500 bg-clip-border"></div>
            <div className="absolute inset-2 bg-white rounded-full"></div>
          </div>
          <p className="mt-6 text-slate-600 font-medium">Loading your profile...</p>
        </div>
      </div>
    );
  }

  if (!user) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-slate-50 via-blue-50 to-indigo-100 flex items-center justify-center">
        <Card className="w-96 shadow-2xl border-0 bg-white/80 backdrop-blur-lg">
          <CardContent className="p-8 text-center">
            <p className="text-red-600 mb-6 font-medium">{error || 'Profile not found'}</p>
            <Button 
              onClick={() => navigate('/login')}
              className="bg-gradient-to-r from-blue-600 to-purple-600 hover:from-blue-700 hover:to-purple-700 text-white font-semibold px-8 py-3 rounded-xl shadow-lg transition-all duration-300"
            >
              Go to Login
            </Button>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 via-blue-50 to-indigo-100">
      {/* Decorative Background Elements */}
      <div className="absolute inset-0 overflow-hidden pointer-events-none">
        <div className="absolute -top-40 -right-40 w-80 h-80 bg-gradient-to-br from-blue-400/20 to-purple-600/20 rounded-full blur-3xl"></div>
        <div className="absolute -bottom-40 -left-40 w-80 h-80 bg-gradient-to-tr from-indigo-400/20 to-pink-600/20 rounded-full blur-3xl"></div>
      </div>

      <div className="relative z-10 py-8 px-4 sm:px-6 lg:px-8">
        <div className="max-w-7xl mx-auto">
          
          {/* Enhanced Header */}
          <div className="mb-10">
            <div className="flex items-center justify-between">
              <div className="flex items-center space-x-4">
                <Button 
                  variant="ghost" 
                  onClick={() => navigate('/dashboard')}
                  className="p-2 hover:bg-white/60 rounded-xl transition-all duration-200"
                >
                  <ArrowLeft className="w-5 h-5 text-slate-600" />
                </Button>
                <div>
                  <h1 className="text-4xl font-bold bg-gradient-to-r from-slate-800 to-slate-600 bg-clip-text text-transparent">
                    My Profile
                  </h1>
                  <p className="text-slate-500 mt-1 font-medium">Manage your account information</p>
                </div>
              </div>
              <Button 
                onClick={() => navigate('/settings')}
                className="bg-white/80 hover:bg-white text-slate-700 border border-slate-200 shadow-lg backdrop-blur-sm px-6 py-3 rounded-xl font-semibold transition-all duration-300 hover:shadow-xl"
              >
                <Settings className="w-4 h-4 mr-2" />
                Account Settings
              </Button>
            </div>
          </div>

          {/* Enhanced Alerts */}
          {error && (
            <div className="mb-8 bg-red-50/80 backdrop-blur-sm border border-red-200 rounded-2xl p-4 shadow-lg">
              <div className="flex items-center">
                <div className="w-2 h-2 bg-red-500 rounded-full mr-3"></div>
                <p className="text-red-800 font-medium">{error}</p>
              </div>
            </div>
          )}

          {success && (
            <div className="mb-8 bg-emerald-50/80 backdrop-blur-sm border border-emerald-200 rounded-2xl p-4 shadow-lg">
              <div className="flex items-center">
                <Check className="w-5 h-5 text-emerald-600 mr-3" />
                <p className="text-emerald-800 font-medium">{success}</p>
              </div>
            </div>
          )}

          <div className="grid grid-cols-1 xl:grid-cols-5 gap-8">
            
            {/* Enhanced Profile Card */}
            <div className="xl:col-span-2">
              <Card className="border-0 shadow-2xl bg-white/70 backdrop-blur-xl rounded-3xl overflow-hidden">
                <CardContent className="p-0">
                  {/* Cover Background */}
                  <div className="h-32 bg-gradient-to-r from-blue-600 via-purple-600 to-indigo-600 relative">
                    <div className="absolute inset-0 bg-black/10"></div>
                    <Sparkles className="absolute top-4 right-4 w-6 h-6 text-white/60" />
                  </div>
                  
                  <div className="px-8 pb-8 -mt-16 relative z-10">
                    {/* ✅ UPDATED: Enhanced Profile Picture with AuthenticatedImage */}
                    <div className="relative inline-block mb-6">
                      <div className="w-32 h-32 ring-6 ring-white shadow-2xl rounded-full overflow-hidden bg-gradient-to-br from-blue-600 to-purple-600">
                        {user.profilePicture && token ? (
                          <AuthenticatedImage
                            src={getAuthenticatedImageUrl(user.profilePicture)!}
                            alt={user.fullName}
                            token={token}
                            className="w-full h-full object-cover"
                            fallbackSrc="/default-avatar.png"
                          />
                        ) : (
                          <div className="w-full h-full flex items-center justify-center text-white text-3xl font-bold">
                            {user.fullName?.split(' ').map(n => n[0]).join('').toUpperCase() || 'U'}
                          </div>
                        )}
                      </div>
                      
                      {/* Enhanced Upload Button */}
                      <label 
                        htmlFor="profile-picture" 
                        className="absolute -bottom-2 -right-2 bg-gradient-to-r from-blue-600 to-purple-600 hover:from-blue-700 hover:to-purple-700 text-white p-3 rounded-full cursor-pointer shadow-xl transition-all duration-300 hover:scale-110 hover:shadow-2xl"
                      >
                        {uploading ? (
                          <div className="animate-spin rounded-full h-5 w-5 border-2 border-white border-t-transparent"></div>
                        ) : (
                          <Camera className="w-5 h-5" />
                        )}
                      </label>
                      <input
                        id="profile-picture"
                        type="file"
                        accept="image/*"
                        onChange={handleProfilePictureUpload}
                        className="hidden"
                        disabled={uploading}
                      />
                    </div>

                    {/* Enhanced Basic Info */}
                    <div className="text-center">
                      <h2 className="text-3xl font-bold text-slate-800 mb-2">
                        {user.fullName}
                      </h2>
                      <p className="text-slate-500 mb-6 font-medium text-lg">@{user.username}</p>
                      
                      {/* Enhanced Role Badge */}
                      <div className="flex justify-center mb-6">
                        {getRoleBadge(user.role)}
                      </div>

                      {/* Enhanced Status */}
                      <div className="flex justify-center">
                        <div className={`inline-flex items-center px-4 py-2 rounded-full font-semibold text-sm ${
                          user.active 
                            ? 'bg-emerald-100 text-emerald-800 shadow-emerald-200' 
                            : 'bg-red-100 text-red-800 shadow-red-200'
                        } shadow-lg`}>
                          <div className={`w-2 h-2 rounded-full mr-2 ${
                            user.active ? 'bg-emerald-500' : 'bg-red-500'
                          }`}></div>
                          {user.active ? 'Active Account' : 'Inactive Account'}
                        </div>
                      </div>
                    </div>
                  </div>
                </CardContent>
              </Card>
            </div>

            {/* Enhanced Information Card */}
            <div className="xl:col-span-3">
              <Card className="border-0 shadow-2xl bg-white/70 backdrop-blur-xl rounded-3xl">
                <CardContent className="p-8">
                  <div className="flex items-center justify-between mb-8">
                    <div className="flex items-center space-x-3">
                      <div className="w-1 h-8 bg-gradient-to-b from-blue-600 to-purple-600 rounded-full"></div>
                      <h3 className="text-2xl font-bold text-slate-800">Profile Information</h3>
                    </div>
                    {!editing ? (
                      <Button 
                        onClick={() => setEditing(true)}
                        className="bg-gradient-to-r from-blue-600 to-purple-600 hover:from-blue-700 hover:to-purple-700 text-white px-6 py-3 rounded-xl font-semibold shadow-lg transition-all duration-300 hover:shadow-xl"
                      >
                        <Edit3 className="w-4 h-4 mr-2" />
                        Edit Profile
                      </Button>
                    ) : (
                      <div className="flex space-x-3">
                        <Button 
                          onClick={handleUpdateProfile}
                          className="bg-gradient-to-r from-emerald-600 to-teal-600 hover:from-emerald-700 hover:to-teal-700 text-white px-6 py-3 rounded-xl font-semibold shadow-lg transition-all duration-300"
                        >
                          <Save className="w-4 h-4 mr-2" />
                          Save Changes
                        </Button>
                        <Button 
                          onClick={() => {
                            setEditing(false);
                            setEditForm({
                              fullName: user?.fullName || '',
                              email: user?.email || ''
                            });
                          }}
                          className="bg-white hover:bg-gray-50 text-slate-700 border border-slate-200 px-6 py-3 rounded-xl font-semibold shadow-lg transition-all duration-300"
                        >
                          <X className="w-4 h-4 mr-2" />
                          Cancel
                        </Button>
                      </div>
                    )}
                  </div>

                  <div className="space-y-8">
                    
                    {/* Enhanced Full Name Field */}
                    <div className="group">
                      <div className="flex items-start space-x-4">
                        <div className="bg-gradient-to-br from-blue-100 to-blue-200 p-4 rounded-2xl group-hover:shadow-lg transition-all duration-300">
                          <User className="w-6 h-6 text-blue-600" />
                        </div>
                        <div className="flex-1">
                          <label className="block text-sm font-bold text-slate-700 mb-3 uppercase tracking-wide">
                            Full Name
                          </label>
                          {editing ? (
                            <input
                              type="text"
                              value={editForm.fullName}
                              onChange={(e) => setEditForm({ ...editForm, fullName: e.target.value })}
                              className="w-full px-4 py-4 border-2 border-slate-200 rounded-xl focus:ring-4 focus:ring-blue-100 focus:border-blue-500 bg-white text-slate-800 font-medium text-lg transition-all duration-300"
                              placeholder="Enter your full name"
                            />
                          ) : (
                            <div className="bg-slate-50 px-4 py-4 rounded-xl">
                              <p className="text-xl font-semibold text-slate-800">{user.fullName}</p>
                            </div>
                          )}
                        </div>
                      </div>
                    </div>

                    {/* Enhanced Email Field */}
                    <div className="group">
                      <div className="flex items-start space-x-4">
                        <div className="bg-gradient-to-br from-emerald-100 to-emerald-200 p-4 rounded-2xl group-hover:shadow-lg transition-all duration-300">
                          <Mail className="w-6 h-6 text-emerald-600" />
                        </div>
                        <div className="flex-1">
                          <label className="block text-sm font-bold text-slate-700 mb-3 uppercase tracking-wide">
                            Email Address
                          </label>
                          {editing ? (
                            <input
                              type="email"
                              value={editForm.email}
                              onChange={(e) => setEditForm({ ...editForm, email: e.target.value })}
                              className="w-full px-4 py-4 border-2 border-slate-200 rounded-xl focus:ring-4 focus:ring-emerald-100 focus:border-emerald-500 bg-white text-slate-800 font-medium text-lg transition-all duration-300"
                              placeholder="Enter your email"
                            />
                          ) : (
                            <div className="bg-slate-50 px-4 py-4 rounded-xl">
                              <p className="text-xl font-semibold text-slate-800">{user.email}</p>
                            </div>
                          )}
                        </div>
                      </div>
                    </div>

                    {/* Enhanced Username Field */}
                    <div className="group">
                      <div className="flex items-start space-x-4">
                        <div className="bg-gradient-to-br from-purple-100 to-purple-200 p-4 rounded-2xl group-hover:shadow-lg transition-all duration-300">
                          <User className="w-6 h-6 text-purple-600" />
                        </div>
                        <div className="flex-1">
                          <label className="block text-sm font-bold text-slate-700 mb-3 uppercase tracking-wide">
                            Username
                          </label>
                          <div className="bg-slate-50 px-4 py-4 rounded-xl border-2 border-dashed border-slate-200">
                            <p className="text-xl font-semibold text-slate-800">@{user.username}</p>
                            <p className="text-sm text-slate-500 mt-1 font-medium">Username cannot be changed</p>
                          </div>
                        </div>
                      </div>
                    </div>

                    {/* Enhanced Member Since Field */}
                    <div className="group">
                      <div className="flex items-start space-x-4">
                        <div className="bg-gradient-to-br from-amber-100 to-amber-200 p-4 rounded-2xl group-hover:shadow-lg transition-all duration-300">
                          <Calendar className="w-6 h-6 text-amber-600" />
                        </div>
                        <div className="flex-1">
                          <label className="block text-sm font-bold text-slate-700 mb-3 uppercase tracking-wide">
                            Member Since
                          </label>
                          <div className="bg-slate-50 px-4 py-4 rounded-xl">
                            <p className="text-xl font-semibold text-slate-800">{formatDate(user.createdAt)}</p>
                          </div>
                        </div>
                      </div>
                    </div>

                    {/* Enhanced Last Login Field */}
                    {user.lastLoginAt && (
                      <div className="group">
                        <div className="flex items-start space-x-4">
                          <div className="bg-gradient-to-br from-slate-100 to-slate-200 p-4 rounded-2xl group-hover:shadow-lg transition-all duration-300">
                            <Calendar className="w-6 h-6 text-slate-600" />
                          </div>
                          <div className="flex-1">
                            <label className="block text-sm font-bold text-slate-700 mb-3 uppercase tracking-wide">
                              Last Login
                            </label>
                            <div className="bg-slate-50 px-4 py-4 rounded-xl">
                              <p className="text-xl font-semibold text-slate-800">{formatDate(user.lastLoginAt)}</p>
                            </div>
                          </div>
                        </div>
                      </div>
                    )}
                  </div>
                </CardContent>
              </Card>
            </div>
          </div>

          {/* Enhanced Action Buttons */}
          <div className="mt-12 flex justify-center space-x-6">
            <Button 
              onClick={() => navigate('/dashboard')}
              className="bg-white/80 hover:bg-white text-slate-700 border border-slate-200 shadow-lg backdrop-blur-sm px-8 py-4 rounded-xl font-semibold transition-all duration-300 hover:shadow-xl"
            >
              <ArrowLeft className="w-5 h-5 mr-2" />
              Back to Dashboard
            </Button>
            <Button 
              onClick={() => navigate('/settings')}
              className="bg-gradient-to-r from-blue-600 to-purple-600 hover:from-blue-700 hover:to-purple-700 text-white px-8 py-4 rounded-xl font-semibold shadow-lg transition-all duration-300 hover:shadow-xl"
            >
              <Settings className="w-5 h-5 mr-2" />
              Advanced Settings
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
}
