/* pages/Settings.tsx */
'use client';

import React, { useEffect, useState } from 'react';
import { ChevronDown, LogOut, Bell, TestTube, CheckCircle } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import Sidebar from '../components/layout/Sidebar';
import settingsService, {
  GeneralSettings, SecuritySettings, NotificationSettings, UserSettingsDTO
} from '../services/settingsService';
import authService from '../services/auth';

/* ---------- CustomSelect Component ---------- */
interface CustomSelectProps {
  value: string;
  options: string[];
  isOpen: boolean;
  setIsOpen: (v: boolean) => void;
  onChange: (v: string) => void;
  placeholder: string;
}

const CustomSelect: React.FC<CustomSelectProps> = ({
  value, options, isOpen, setIsOpen, onChange, placeholder
}) => (
  <div className="relative">
    <button
      type="button"
      onClick={() => setIsOpen(!isOpen)}
      className="w-full h-11 px-4 py-2 text-left bg-white border border-gray-300 rounded-lg shadow-sm hover:border-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500 transition-colors"
    >
      <div className="flex items-center justify-between">
        <span className="text-sm text-gray-900 truncate">{value || placeholder}</span>
        <ChevronDown className={`w-4 h-4 text-gray-500 transition-transform ${isOpen ? 'rotate-180' : ''}`} />
      </div>
    </button>

    {isOpen && (
      <div className="absolute z-50 w-full mt-1 bg-white border border-gray-300 rounded-lg shadow-lg max-h-60 overflow-auto">
        {options.map(opt => (
          <button
            key={opt}
            type="button"
            onClick={() => { onChange(opt); setIsOpen(false); }}
            className={`w-full px-4 py-2.5 text-left text-sm hover:bg-gray-50 ${
              value === opt ? 'bg-blue-50 text-blue-700 font-medium' : 'text-gray-900'
            }`}
          >
            {opt}
          </button>
        ))}
      </div>
    )}
  </div>
);

/* ---------- Main Settings Component ---------- */
const Settings: React.FC = () => {
  const navigate = useNavigate();

  /* ✅ FIXED: State variables using correct property names */
  const [appName, setAppName] = useState('CloudDocs');
  const [timezone, setTimezone] = useState('UTC');
  const [language, setLanguage] = useState('English');
  const [twoFactor, setTwoFactor] = useState(false);
  const [passwordPolicy, setPasswordPolicy] = useState('');
  
  // ✅ FIXED: Using correct notification property names
  const [notifEmail, setNotifEmail] = useState(true);
  const [notifSms, setNotifSms] = useState(false);
  const [notifPush, setNotifPush] = useState(true);
  
  // ✅ ADDED: Loading and success states
  const [loading, setLoading] = useState(false);
  const [testNotificationLoading, setTestNotificationLoading] = useState(false);
  const [saveStatus, setSaveStatus] = useState<{type: 'success' | 'error' | null, message: string}>({type: null, message: ''});

  /* dropdown open states */
  const [open, setOpen] = useState<{ tz: boolean; lang: boolean; pwd: boolean }>({
    tz: false, lang: false, pwd: false
  });

  /* ✅ FIXED: Fetch current settings with proper error handling */
  useEffect(() => {
    const loadSettings = async () => {
      try {
        setLoading(true);
        const res: UserSettingsDTO = await settingsService.fetch();
        
        // General settings
        setAppName(res.general.appName);
        setTimezone(res.general.timezone);
        setLanguage(res.general.language);
        
        // Security settings
        setTwoFactor(res.security.twoFactorEnabled);
        setPasswordPolicy(res.security.passwordPolicy);
        
        // ✅ FIXED: Using backward compatible properties
        setNotifEmail(res.notifications.emailApproval ?? res.notifications.emailEnabled);
        setNotifSms(res.notifications.sms ?? res.notifications.smsEnabled);
        setNotifPush(res.notifications.push ?? res.notifications.pushEnabled);
        
      } catch (error) {
        console.error('Failed to load settings:', error);
        setSaveStatus({type: 'error', message: 'Failed to load settings'});
      } finally {
        setLoading(false);
      }
    };

    loadSettings();
  }, []);

  /* option arrays */
  const timezones = ['UTC', 'EST', 'PST', 'GMT', 'IST', 'CET'];
  const languages = ['English', 'Spanish', 'French', 'German', 'Chinese'];
  const policies = [
    'Weak (Min 6 chars)',
    'Medium (Min 8 chars, mixed case)',
    'Strong (Min 8 chars, mixed case, number, symbol)',
    'Very Strong (Min 12 chars, all requirements)'
  ];

  /* ✅ FIXED: Save handlers with proper error handling */
  const handleSaveGeneral = async () => {
    try {
      setLoading(true);
      const dto: GeneralSettings = { appName, timezone, language };
      await settingsService.saveGeneral(dto);
      setSaveStatus({type: 'success', message: 'General settings saved successfully'});
    } catch (error) {
      console.error('Failed to save general settings:', error);
      setSaveStatus({type: 'error', message: 'Failed to save general settings'});
    } finally {
      setLoading(false);
    }
  };

  const handleSaveSecurity = async () => {
    try {
      setLoading(true);
      const dto: SecuritySettings = { twoFactorEnabled: twoFactor, passwordPolicy };
      await settingsService.saveSecurity(dto);
      setSaveStatus({type: 'success', message: 'Security settings updated successfully'});
    } catch (error) {
      console.error('Failed to save security settings:', error);
      setSaveStatus({type: 'error', message: 'Failed to save security settings'});
    } finally {
      setLoading(false);
    }
  };

  const handleSaveNotifications = async () => {
    try {
      setLoading(true);
      // ✅ FIXED: Using correct property names that match the interface
      const dto: NotificationSettings = { 
        emailApproval: notifEmail,
        sms: notifSms,
        push: notifPush,
        // Include required properties with defaults
        emailEnabled: notifEmail,
        smsEnabled: notifSms,
        pushEnabled: notifPush,
        emailTaskAssigned: true,
        emailWorkflowApproved: true,
        emailWorkflowRejected: true,
        smsUrgentOnly: true,
        pushTaskAssigned: true,
        pushWorkflowUpdates: true,
        quietHoursStart: '22:00',
        quietHoursEnd: '08:00'
      };
      await settingsService.saveNotifications(dto);
      setSaveStatus({type: 'success', message: 'Notification preferences saved successfully'});
    } catch (error) {
      console.error('Failed to save notification settings:', error);
      setSaveStatus({type: 'error', message: 'Failed to save notification preferences'});
    } finally {
      setLoading(false);
    }
  };

  /* logout button */
  const handleLogout = async () => {
    try {
      await authService.logout();
      navigate('/login');
    } catch (error) {
      console.error('Logout failed:', error);
      // Force navigation even if logout API fails
      navigate('/login');
    }
  };

  // ✅ ADDED: Auto-hide status messages
  useEffect(() => {
    if (saveStatus.type) {
      const timer = setTimeout(() => {
        setSaveStatus({type: null, message: ''});
      }, 5000);
      return () => clearTimeout(timer);
    }
  }, [saveStatus]);

  return (
    <div className="flex h-screen bg-gray-50">
      <Sidebar />

      <main className="flex-1 flex flex-col">
        {/* ------- Header ------- */}
        <header className="bg-white border-b border-gray-200 px-8 py-6 flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-semibold text-gray-900 mb-1">Settings</h1>
            <p className="text-sm text-gray-500">
              Manage your application preferences and security settings
            </p>
          </div>

          <button
            onClick={handleLogout}
            className="flex items-center px-4 py-2 text-sm text-red-600 border border-red-300 rounded-lg hover:bg-red-50 transition-colors"
          >
            <LogOut className="w-4 h-4 mr-2" />
            Logout
          </button>
        </header>

        {/* ✅ ADDED: Status Messages */}
        {saveStatus.type && (
          <div className={`mx-8 mt-4 p-4 rounded-lg border ${
            saveStatus.type === 'success' 
              ? 'bg-green-50 border-green-200 text-green-800' 
              : 'bg-red-50 border-red-200 text-red-800'
          }`}>
            <div className="flex items-center">
              {saveStatus.type === 'success' ? (
                <CheckCircle className="w-5 h-5 mr-2" />
              ) : (
                <div className="w-5 h-5 mr-2 rounded-full bg-red-500" />
              )}
              {saveStatus.message}
            </div>
          </div>
        )}

        {/* ------- Content ------- */}
        <section className="flex-1 p-8 overflow-y-auto">
          <div className="max-w-4xl mx-auto space-y-6">

            {/* ----- General Settings ----- */}
            <div className="bg-white border rounded-xl p-6 hover:shadow-lg transition-shadow">
              <h2 className="text-xl font-medium mb-6">General Settings</h2>

              <div className="space-y-6">
                <div>
                  <label className="block text-sm font-medium mb-2">Application Name</label>
                  <input
                    type="text"
                    value={appName}
                    onChange={e => setAppName(e.target.value)}
                    className="w-full h-11 px-4 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                    disabled={loading}
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium mb-2">Timezone</label>
                  <CustomSelect
                    value={timezone}
                    options={timezones}
                    isOpen={open.tz}
                    setIsOpen={v => setOpen({ ...open, tz: v })}
                    onChange={setTimezone}
                    placeholder="Select timezone..."
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium mb-2">Language</label>
                  <CustomSelect
                    value={language}
                    options={languages}
                    isOpen={open.lang}
                    setIsOpen={v => setOpen({ ...open, lang: v })}
                    onChange={setLanguage}
                    placeholder="Select language..."
                  />
                </div>
              </div>

              <div className="flex justify-end mt-6">
                <button
                  onClick={handleSaveGeneral}
                  disabled={loading}
                  className="px-6 py-2.5 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                >
                  {loading ? 'Saving...' : 'Save Changes'}
                </button>
              </div>
            </div>

            {/* ----- Security Settings ----- */}
            <div className="bg-white border rounded-xl p-6 hover:shadow-lg transition-shadow">
              <h2 className="text-xl font-medium mb-6">Security</h2>

              <div className="space-y-6">
                <label className="flex items-center space-x-3 cursor-pointer hover:bg-gray-50 p-2 rounded">
                  <input
                    type="checkbox"
                    checked={twoFactor}
                    onChange={e => setTwoFactor(e.target.checked)}
                    className="w-4 h-4 text-blue-600 border-gray-300 rounded focus:ring-blue-500"
                    disabled={loading}
                  />
                  <span className="text-sm">Enable Two-Factor Authentication</span>
                </label>

                <div>
                  <label className="block text-sm font-medium mb-2">Password Policy</label>
                  <CustomSelect
                    value={passwordPolicy}
                    options={policies}
                    isOpen={open.pwd}
                    setIsOpen={v => setOpen({ ...open, pwd: v })}
                    onChange={setPasswordPolicy}
                    placeholder="Select password policy..."
                  />
                </div>
              </div>

              <div className="flex justify-end mt-6">
                <button
                  onClick={handleSaveSecurity}
                  disabled={loading}
                  className="px-6 py-2.5 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                >
                  {loading ? 'Updating...' : 'Update Security'}
                </button>
              </div>
            </div>

            {/* ----- Notification Settings ----- */}
            <div className="bg-white border rounded-xl p-6 hover:shadow-lg transition-shadow">

              <div className="space-y-4">
                <label className="flex items-center space-x-3 cursor-pointer hover:bg-gray-50 p-2 rounded">
                  <input
                    type="checkbox"
                    checked={notifEmail}
                    onChange={e => setNotifEmail(e.target.checked)}
                    className="w-4 h-4 text-blue-600 border-gray-300 rounded focus:ring-blue-500"
                    disabled={loading}
                  />
                  <div className="flex-1">
                    <span className="text-sm font-medium">Email Notifications for Document Approval</span>
                    <p className="text-xs text-gray-500">Receive email alerts when documents need approval or are approved</p>
                  </div>
                </label>

                <label className="flex items-center space-x-3 cursor-pointer hover:bg-gray-50 p-2 rounded">
                  <input
                    type="checkbox"
                    checked={notifSms}
                    onChange={e => setNotifSms(e.target.checked)}
                    className="w-4 h-4 text-blue-600 border-gray-300 rounded focus:ring-blue-500"
                    disabled={loading}
                  />
                  <div className="flex-1">
                    <span className="text-sm font-medium">SMS Notifications</span>
                    <p className="text-xs text-gray-500">Receive text messages for urgent workflow updates</p>
                  </div>
                </label>

                <label className="flex items-center space-x-3 cursor-pointer hover:bg-gray-50 p-2 rounded">
                  <input
                    type="checkbox"
                    checked={notifPush}
                    onChange={e => setNotifPush(e.target.checked)}
                    className="w-4 h-4 text-blue-600 border-gray-300 rounded focus:ring-blue-500"
                    disabled={loading}
                  />
                  <div className="flex-1">
                    <span className="text-sm font-medium">Push Notifications</span>
                    <p className="text-xs text-gray-500">Receive browser notifications for real-time updates</p>
                  </div>
                </label>
              </div>

              <div className="flex justify-end mt-6">
                <button
                  onClick={handleSaveNotifications}
                  disabled={loading}
                  className="px-6 py-2.5 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                >
                  {loading ? 'Saving...' : 'Save Preferences'}
                </button>
              </div>
            </div>
          </div>
        </section>
      </main>
    </div>
  );
};

export default Settings;
