// Configuration for API calls
// Automatically detects baseURL and contextPath from browser URI

/**
 * Detects the context path from the current browser URL
 * Examples:
 * - http://localhost:3000/ -> ''
 * - http://localhost:3000/ds-editor -> '/ds-editor'
 * - http://example.com/app/ds-editor -> '/app/ds-editor'
 */
const detectContextPath = () => {
  // In development mode with proxy, no context path
  if (process.env.NODE_ENV === 'development') {
    return '';
  }

  const pathname = window.location.pathname;

  // If at root or just index.html, no context path
  if (pathname === '/' || pathname === '/index.html') {
    return '';
  }

  // Remove trailing slashes and file names
  let path = pathname.replace(/\/index\.html$/, '');
  path = path.replace(/\/$/, '');

  // Check if this looks like a context path
  // Context paths typically don't have file extensions and aren't API calls
  if (path && !path.includes('.') && !path.startsWith('/api/')) {
    // If path has multiple segments, take all but potentially the last one
    // This handles cases like /ds-editor/some-page
    const segments = path.split('/').filter(s => s);

    // If only one segment and it's not a known route, it's likely the context path
    if (segments.length === 1) {
      return `/${segments[0]}`;
    }

    // For multiple segments, assume the first is the context path
    // This works for most standard deployments
    if (segments.length > 1) {
      return `/${segments[0]}`;
    }
  }

  return '';
};

/**
 * Gets the base URL for API calls from browser location
 * Includes protocol, host, port, and context path
 */
const getBaseURL = () => {
  // Check if config is explicitly injected from server (highest priority)
  if (window.APP_CONFIG && window.APP_CONFIG.API_BASE_URL) {
    return window.APP_CONFIG.API_BASE_URL;
  }

  // In development mode, use relative URLs (proxy will handle)
  if (process.env.NODE_ENV === 'development') {
    return '';
  }

  // Construct base URL from current browser location
  const protocol = window.location.protocol; // http: or https:
  const host = window.location.host; // hostname:port
  const contextPath = detectContextPath();

  // Build full base URL
  const baseUrl = `${protocol}//${host}${contextPath}`;

  return baseUrl;
};

/**
 * Gets just the context path portion
 */
const getContextPath = () => {
  // Check for explicit config
  if (window.APP_CONFIG && window.APP_CONFIG.CONTEXT_PATH) {
    return window.APP_CONFIG.CONTEXT_PATH;
  }

  // Detect from browser URL
  return detectContextPath();
};

/**
 * Gets the relative base path for API calls
 * This is used when making relative API requests
 */
const getRelativeBasePath = () => {
  if (process.env.NODE_ENV === 'development') {
    return '';
  }

  const contextPath = getContextPath();
  return contextPath || '';
};

// Export configuration
export const API_BASE_URL = getBaseURL();
export const CONTEXT_PATH = getContextPath();
export const RELATIVE_BASE_PATH = getRelativeBasePath();

// API endpoints - use relative paths that work with or without context path
export const API_ENDPOINTS = {
  SQL_RUN: `${RELATIVE_BASE_PATH}/api/sql/run`,
  SQL_BATCH: `${RELATIVE_BASE_PATH}/api/sql/run/batch`,
};

// Debugging helper (remove in production)
if (process.env.NODE_ENV === 'development') {
  console.log('🔧 API Configuration:', {
    API_BASE_URL,
    CONTEXT_PATH,
    RELATIVE_BASE_PATH,
    API_ENDPOINTS,
    currentURL: window.location.href,
    pathname: window.location.pathname
  });
}

export default {
  API_BASE_URL,
  CONTEXT_PATH,
  RELATIVE_BASE_PATH,
  API_ENDPOINTS,
  // Expose functions for dynamic recalculation if needed
  detectContextPath,
  getBaseURL,
  getContextPath
};
