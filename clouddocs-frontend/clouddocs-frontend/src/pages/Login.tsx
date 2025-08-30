import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { Cloud, AlertCircle, CheckCircle } from 'lucide-react';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import { Label } from '../components/ui/label';
import { Card, CardContent } from '../components/ui/card';
import api from '../services/api'; // Import API directly for better control

export default function Login() {
  const [credentials, setCredentials] = useState({
    username: '',
    password: ''
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const navigate = useNavigate();

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setCredentials(prev => ({
      ...prev,
      [e.target.name]: e.target.value
    }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!credentials.username || !credentials.password) {
      setError('Please fill in all fields');
      return;
    }

    setLoading(true);
    setError('');
    setSuccess('');

    // Debug logging
    console.log('Login attempt with:', credentials);
    console.log('Username:', credentials.username);
    console.log('Password length:', credentials.password.length);

    try {
      // FIXED: Call the correct endpoint that matches your backend (/login)
      const response = await api.post('/auth/signin', {
        username: credentials.username,
        password: credentials.password
      });

      console.log('Login response:', response.data);

      const { accessToken, username, email, roles, firstName, lastName } = response.data;

      if (!accessToken) {
        throw new Error('No token received from server');
      }

      // FIXED: Extract primary role from roles array
      const primaryRole = roles && roles.length > 0 ? 
        roles[0].replace('ROLE_', '') : 'USER';
      
      // Store authentication data
      localStorage.setItem('token', accessToken);
      localStorage.setItem('userData', JSON.stringify({
        username,
        email,
        firstName,
        lastName,
        role: primaryRole,
        roles: roles,
        expiresAt: Date.now() + (24 * 60 * 60 * 1000) // 24 hours
      }));

      setSuccess('Login successful! Redirecting...');
      
      // Debug: Verify storage
      console.log('Stored token:', localStorage.getItem('token'));
      console.log('Stored user data:', localStorage.getItem('userData'));
      
      setTimeout(() => {
        // FIXED: Navigate to workflow instead of documents
        navigate('/dashboard', { replace: true });
      }, 1000);
      
    } catch (error: any) {
      console.error('Login error:', error);
      
      if (error.response?.status === 401) {
        setError('Invalid username or password');
      } else if (error.response?.status === 400) {
        setError('Please check your login credentials');
      } else {
        setError(error.response?.data?.message || error.message || 'Login failed. Please try again.');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-gray-100 flex items-center justify-center p-6">
      <Card className="w-full max-w-xl bg-white shadow-lg border-0 rounded-2xl">
        <CardContent className="p-14">
          
          <div className="flex items-center justify-center mb-8">
            <Cloud className="w-10 h-10 text-blue-600 mr-3" />
            <span className="text-2xl text-gray-900 font-medium">CloudDocs</span>
          </div>

          <div className="text-center mb-10">
            <h1 className="text-2xl text-gray-900 font-medium mb-3">Welcome Back</h1>
            <p className="text-gray-500 text-lg">Sign in to your account to continue</p>
          </div>

          {error && (
            <div className="mb-6 p-4 bg-red-50 border border-red-200 rounded-lg flex items-center">
              <AlertCircle className="w-5 h-5 text-red-500 mr-2" />
              <span className="text-red-700">{error}</span>
            </div>
          )}

          {success && (
            <div className="mb-6 p-4 bg-green-50 border border-green-200 rounded-lg flex items-center">
              <CheckCircle className="w-5 h-5 text-green-500 mr-2" />
              <span className="text-green-700">{success}</span>
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-8">
            <div className="space-y-2">
              <Label htmlFor="username" className="text-gray-700 text-lg">Username or Email</Label>
              <Input
                id="username"
                name="username"
                type="text"
                value={credentials.username}
                onChange={handleChange}
                className="w-full px-5 py-4 border border-gray-200 rounded-lg bg-white text-gray-900 placeholder-gray-400 focus:border-blue-500 focus:ring-1 focus:ring-blue-500 text-lg"
                placeholder="Enter your username or email"
                required
                disabled={loading}
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="password" className="text-gray-700 text-lg">Password</Label>
              <Input
                id="password"
                name="password"
                type="password"
                value={credentials.password}
                onChange={handleChange}
                className="w-full px-5 py-4 border border-gray-200 rounded-lg bg-white text-gray-900 placeholder-gray-400 focus:border-blue-500 focus:ring-1 focus:ring-blue-500 text-lg"
                placeholder="Enter your password"
                required
                disabled={loading}
              />
            </div>

            <Button 
              type="submit"
              disabled={loading}
              className="w-full bg-blue-600 hover:bg-blue-700 disabled:bg-blue-400 text-white py-4 text-lg rounded-lg transition-colors"
            >
              {loading ? 'Signing In...' : 'Sign In'}
            </Button>

            <div className="text-center text-gray-500">
              Don't have an account?{' '}
              <Link 
                to="/register" 
                className="text-blue-600 hover:text-blue-700 transition-colors font-medium"
              >
                Sign Up
              </Link>
            </div>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
