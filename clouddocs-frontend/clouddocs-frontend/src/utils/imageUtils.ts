// ✅ utils/imageUtils.ts 
export const normalizeProfilePicturePath = (profilePicture: string): string => {
  if (!profilePicture) return '';
  
  // Remove any leading path separators and folder prefixes
  let cleanPath = profilePicture.replace(/^[\/\\]+/, ''); // Remove leading slashes
  
  // If path already contains 'profile-pictures/', extract just the filename
  if (cleanPath.includes('profile-pictures/')) {
    cleanPath = cleanPath.split('profile-pictures/').pop() || cleanPath;
  }
  
  // Return normalized path with consistent folder structure
  return `profile-pictures/${cleanPath}`;
};

export const buildProfileImageUrl = (profilePicture: string | undefined, baseUrl: string): string | undefined => {
  if (!profilePicture) return undefined;
  
  const normalizedPath = normalizeProfilePicturePath(profilePicture);
  return `${baseUrl}/api/users/profile/picture/${normalizedPath}`;
};
