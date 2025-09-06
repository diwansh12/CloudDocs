// ✅ components/AuthenticatedImage.tsx
import React, { useState, useEffect } from 'react';

interface AuthenticatedImageProps {
  src: string;
  alt: string;
  className?: string;
  token?: string;
  fallbackSrc?: string;
}

const AuthenticatedImage: React.FC<AuthenticatedImageProps> = ({ 
  src, 
  alt, 
  className = '', 
  token,
  fallbackSrc = '/default-avatar.png' 
}) => {
  const [imageSrc, setImageSrc] = useState<string>('');
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<boolean>(false);

  useEffect(() => {
    const fetchImage = async () => {
      if (!src || !token) {
        setError(true);
        setLoading(false);
        return;
      }

      try {
        setLoading(true);
        setError(false);

        const response = await fetch(src, {
          method: 'GET',
          headers: {
            'Authorization': `Bearer ${token}`,
            'Accept': 'image/*'
          }
        });

        if (!response.ok) {
          throw new Error(`HTTP error! status: ${response.status}`);
        }

        const blob = await response.blob();
        const imageUrl = URL.createObjectURL(blob);
        setImageSrc(imageUrl);
        setLoading(false);

      } catch (err) {
        console.error('Failed to load authenticated image:', err);
        setError(true);
        setLoading(false);
      }
    };

    fetchImage();

    // Cleanup function to revoke object URL
    return () => {
      if (imageSrc && imageSrc.startsWith('blob:')) {
        URL.revokeObjectURL(imageSrc);
      }
    };
  }, [src, token]);

  // Loading state
  if (loading) {
    return (
      <div className={`image-loading ${className}`}>
        <div className="loading-spinner">Loading...</div>
      </div>
    );
  }

  // Error state with fallback
  if (error) {
    return (
      <img 
        src={fallbackSrc} 
        alt={alt} 
        className={className}
      />
    );
  }

  return (
    <img 
      src={imageSrc} 
      alt={alt} 
      className={className}
    />
  );
};

export default AuthenticatedImage;
