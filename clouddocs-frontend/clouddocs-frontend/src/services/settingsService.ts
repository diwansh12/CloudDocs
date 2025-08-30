import api from './api';

export interface NotificationSettings {
  emailEnabled: boolean;
  smsEnabled: boolean;
  pushEnabled: boolean;
  emailTaskAssigned: boolean;
  emailWorkflowApproved: boolean;
  emailWorkflowRejected: boolean;
  smsUrgentOnly: boolean;
  pushTaskAssigned: boolean;
  pushWorkflowUpdates: boolean;
  quietHoursStart: string;
  quietHoursEnd: string;
  
  // ✅ ADDED: Backward compatibility properties for existing frontend code
  emailApproval?: boolean;
  sms?: boolean;
  push?: boolean;
}

export interface GeneralSettings {
  appName: string;
  timezone: string;
  language: string;
}

export interface SecuritySettings {
  twoFactorEnabled: boolean;
  passwordPolicy: string;
}

export interface UserSettingsDTO {
  general: GeneralSettings;
  security: SecuritySettings;
  notifications: NotificationSettings;
}

class SettingsService {
  
  async fetch(): Promise<UserSettingsDTO> {
    try {
      const [general, security, notifications] = await Promise.all([
        this.fetchGeneral(),
        this.fetchSecurity(), 
        this.fetchNotifications()
      ]);
      
      return { general, security, notifications };
    } catch (error) {
      console.error('Failed to fetch settings:', error);
      throw error;
    }
  }

  // ✅ ADDED: Missing fetchGeneral method
  async fetchGeneral(): Promise<GeneralSettings> {
    try {
      const response = await api.get('/users/settings/general');
      return {
        appName: response.data.appName ?? 'CloudDocs',
        timezone: response.data.timezone ?? 'UTC',
        language: response.data.language ?? 'English'
      };
    } catch (error) {
      // Return defaults if API call fails
      return {
        appName: 'CloudDocs',
        timezone: 'UTC', 
        language: 'English'
      };
    }
  }

  // ✅ ADDED: Missing fetchSecurity method
  async fetchSecurity(): Promise<SecuritySettings> {
    try {
      const response = await api.get('/users/settings/security');
      return {
        twoFactorEnabled: response.data.twoFactorEnabled ?? false,
        passwordPolicy: response.data.passwordPolicy ?? ''
      };
    } catch (error) {
      // Return defaults if API call fails
      return {
        twoFactorEnabled: false,
        passwordPolicy: ''
      };
    }
  }

  async fetchNotifications(): Promise<NotificationSettings> {
    try {
      const response = await api.get('/users/notification-settings');
      return {
        emailEnabled: response.data.emailEnabled ?? true,
        smsEnabled: response.data.smsEnabled ?? false,
        pushEnabled: response.data.pushEnabled ?? true,
        emailTaskAssigned: response.data.emailTaskAssigned ?? true,
        emailWorkflowApproved: response.data.emailWorkflowApproved ?? true,
        emailWorkflowRejected: response.data.emailWorkflowRejected ?? true,
        smsUrgentOnly: response.data.smsUrgentOnly ?? true,
        pushTaskAssigned: response.data.pushTaskAssigned ?? true,
        pushWorkflowUpdates: response.data.pushWorkflowUpdates ?? true,
        quietHoursStart: response.data.quietHoursStart ?? '22:00',
        quietHoursEnd: response.data.quietHoursEnd ?? '08:00',
        
        // ✅ BACKWARD COMPATIBILITY: Map to old property names
        emailApproval: response.data.emailEnabled ?? true,
        sms: response.data.smsEnabled ?? false,
        push: response.data.pushEnabled ?? true
      };
    } catch (error) {
      console.error('Failed to fetch notification settings:', error);
      // Return defaults
      return {
        emailEnabled: true,
        smsEnabled: false,
        pushEnabled: true,
        emailTaskAssigned: true,
        emailWorkflowApproved: true,
        emailWorkflowRejected: true,
        smsUrgentOnly: true,
        pushTaskAssigned: true,
        pushWorkflowUpdates: true,
        quietHoursStart: '22:00',
        quietHoursEnd: '08:00',
        emailApproval: true,
        sms: false,
        push: true
      };
    }
  }

  // ✅ ADDED: Missing saveGeneral method
  async saveGeneral(settings: GeneralSettings): Promise<void> {
    try {
      await api.put('/users/settings/general', settings);
    } catch (error) {
      console.error('Failed to save general settings:', error);
      throw error;
    }
  }

  // ✅ ADDED: Missing saveSecurity method
  async saveSecurity(settings: SecuritySettings): Promise<void> {
    try {
      await api.put('/users/settings/security', settings);
    } catch (error) {
      console.error('Failed to save security settings:', error);
      throw error;
    }
  }

  async saveNotifications(settings: NotificationSettings): Promise<void> {
    try {
      // Map old property names to new ones for backend compatibility
      const backendSettings = {
        emailEnabled: settings.emailApproval ?? settings.emailEnabled,
        smsEnabled: settings.sms ?? settings.smsEnabled,
        pushEnabled: settings.push ?? settings.pushEnabled,
        emailTaskAssigned: settings.emailTaskAssigned ?? true,
        emailWorkflowApproved: settings.emailWorkflowApproved ?? true,
        emailWorkflowRejected: settings.emailWorkflowRejected ?? true,
        smsUrgentOnly: settings.smsUrgentOnly ?? true,
        pushTaskAssigned: settings.pushTaskAssigned ?? true,
        pushWorkflowUpdates: settings.pushWorkflowUpdates ?? true,
        quietHoursStart: settings.quietHoursStart ?? '22:00',
        quietHoursEnd: settings.quietHoursEnd ?? '08:00'
      };
      
      await api.put('/users/notification-settings', backendSettings);
    } catch (error) {
      console.error('Failed to save notification settings:', error);
      throw error;
    }
  }

  async saveFcmToken(token: string): Promise<void> {
    try {
      await api.post('/users/notification-settings/fcm-token', { token });
    } catch (error) {
      console.error('Failed to save FCM token:', error);
      throw error;
    }
  }

  async removeFcmToken(): Promise<void> {
    try {
      await api.delete('/users/notification-settings/fcm-token');
    } catch (error) {
      console.error('Failed to remove FCM token:', error);
      throw error;
    }
  }

  // ✅ ADDED: Test notification method
  async sendTestNotification(): Promise<void> {
    try {
      await api.post('/users/notification-settings/test');
    } catch (error) {
      console.error('Failed to send test notification:', error);
      throw error;
    }
  }

  // ✅ ADDED: Get notification summary
  async getNotificationSummary(): Promise<any> {
    try {
      const response = await api.get('/users/notification-settings/summary');
      return response.data;
    } catch (error) {
      console.error('Failed to get notification summary:', error);
      throw error;
    }
  }
}

export default new SettingsService();
