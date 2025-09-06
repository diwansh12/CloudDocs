// ✅ components/AuthenticatedImage.tsx - IMPROVED VERSION
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
  const [imageSrc, setImageSrc] = useState<string>(fallbackSrc);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<boolean>(false);

  useEffect(() => {
    const fetchImage = async () => {
      if (!src || !token) {
        setError(true);
        setImageSrc(fallbackSrc);
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

        // ✅ IMPROVED: Handle 404 specifically without logging error
        if (response.status === 404) {
          // Image not found - silently use fallback
          setError(true);
          setImageSrc(fallbackSrc);
          setLoading(false);
          return;
        }

        // ✅ IMPROVED: Handle other HTTP errors
        if (!response.ok) {
          // Only log non-404 errors for debugging
          if (response.status !== 404) {
            console.warn(`Image load failed with status ${response.status}:`, src);
          }
          setError(true);
          setImageSrc(fallbackSrc);
          setLoading(false);
          return;
        }

        // ✅ SUCCESS: Image found and accessible
        const blob = await response.blob();
        const imageUrl = URL.createObjectURL(blob);
        setImageSrc(imageUrl);
        setLoading(false);

      } catch (err) {
        // ✅ IMPROVED: Only log unexpected errors, not 404s
        console.warn('Network error loading image:', err);
        setError(true);
        setImageSrc(fallbackSrc);
        setLoading(false);
      }
    };

    fetchImage();

    // ✅ Cleanup function to revoke object URL
    return () => {
      if (imageSrc && imageSrc.startsWith('blob:')) {
        URL.revokeObjectURL(imageSrc);
      }
    };
  }, [src, token, fallbackSrc]);

  // ✅ IMPROVED: Better loading state
  if (loading) {
    return (
      <div className={`image-loading ${className} flex items-center justify-center`}>
        <div className="animate-spin rounded-full h-6 w-6 border-2 border-gray-300 border-t-blue-600"></div>
      </div>
    );
  }

  // ✅ Always return image (either loaded image or fallback)
  return (
    <img 
      src={imageSrc} 
      alt={alt} 
      className={className}
      onError={() => {
        // ✅ Additional safety: if even fallback fails, ensure we have something
        if (imageSrc !== fallbackSrc) {
          setImageSrc(fallbackSrc);
        }
      }}
    />
  );
};

export default AuthenticatedImage;
