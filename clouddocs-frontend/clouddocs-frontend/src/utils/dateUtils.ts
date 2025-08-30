// utils/dateUtils.ts
export const formatDate = (dateValue: string | null | undefined): string => {
  // Handle null, undefined, or empty values
  if (!dateValue || dateValue === null || dateValue === undefined) {
    return 'Never updated';
  }
  
  try {
    // Handle the ISO format from your backend
    const date = new Date(dateValue);
    
    // Check if the date is valid
    if (isNaN(date.getTime())) {
      console.error('Invalid date value received:', dateValue);
      return 'Invalid Date';
    }
    
    const now = new Date();
    const diffInMs = now.getTime() - date.getTime();
    const diffInHours = Math.floor(diffInMs / (1000 * 60 * 60));
    const diffInDays = Math.floor(diffInHours / 24);
    
    // Handle future dates (like due dates)
    if (diffInMs < 0) {
      const futureDiffInHours = Math.floor(Math.abs(diffInMs) / (1000 * 60 * 60));
      const futureDiffInDays = Math.floor(futureDiffInHours / 24);
      
      if (futureDiffInHours < 24) return `In ${futureDiffInHours} hours`;
      if (futureDiffInDays === 1) return 'Tomorrow';
      if (futureDiffInDays < 7) return `In ${futureDiffInDays} days`;
    }
    
    // Handle past dates
    if (diffInMs < 60000) return 'Just now'; // Less than 1 minute
    if (diffInMs < 3600000) return `${Math.floor(diffInMs / 60000)} min ago`; // Less than 1 hour
    if (diffInHours < 24) return `${diffInHours} hours ago`;
    if (diffInDays === 1) return 'Yesterday';
    if (diffInDays < 7) return `${diffInDays} days ago`;
    
    // For older dates, show formatted date
    return date.toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: date.getFullYear() !== now.getFullYear() ? 'numeric' : undefined,
      hour: '2-digit',
      minute: '2-digit'
    });
    
  } catch (error) {
    console.error('Date formatting error:', error, 'Original value:', dateValue);
    return 'Invalid Date';
  }
};

// Additional utility for absolute dates
export const formatAbsoluteDate = (dateValue: string | null | undefined): string => {
  if (!dateValue || dateValue === null) return 'Not set';
  
  try {
    const date = new Date(dateValue);
    if (isNaN(date.getTime())) return 'Invalid Date';
    
    return date.toLocaleDateString('en-US', {
      weekday: 'short',
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  } catch (error) {
    return 'Invalid Date';
  }
};
