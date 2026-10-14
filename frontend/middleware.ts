import { NextResponse } from 'next/server';
import type { NextRequest } from 'next/server';

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

// Role types matching the backend
type Role = 'CUSTOMER' | 'AGENT' | 'INSURER_ADMIN' | 'ADMIN';

// Public routes that don't require authentication
const publicRoutes = ['/', '/login', '/register', '/forgot-password', '/reset-password', '/file-claim', '/track', '/submit-feedback', '/staff/login'];

// Staff routes - only accessible by staff roles
const staffRoutes = ['/dashboard', '/claims', '/customers', '/policies', '/kyc', '/garages', '/feedback', '/users', '/roles', '/audit-trail'];

// Customer routes - only accessible by customers
const customerRoutes = ['/customer'];

// Helper to check if a path is public
function isPublicRoute(pathname: string): boolean {
  // Exact match for public routes
  if (publicRoutes.includes(pathname)) {
    return true;
  }
  // Check if path starts with any public route prefix
  return publicRoutes.some(route => pathname.startsWith(route + '/'));
}

// Helper to check if a path is a staff route
function isStaffRoute(pathname: string): boolean {
  return staffRoutes.some(route => pathname.startsWith(route));
}

// Helper to check if a path is a customer route
function isCustomerRoute(pathname: string): boolean {
  return customerRoutes.some(route => pathname.startsWith(route));
}

// Helper to validate token with backend
async function validateToken(accessToken: string): Promise<{ valid: boolean; role?: Role }> {
  try {
    const response = await fetch(`${API_BASE_URL}/api/v1/auth/me`, {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${accessToken}`,
        'Content-Type': 'application/json',
      },
      cache: 'no-store',
    });

    if (response.ok) {
      const user = await response.json();
      return { valid: true, role: user.role };
    }
    return { valid: false };
  } catch {
    return { valid: false };
  }
}

// Helper to check if role is staff
function isStaffRole(role: Role): boolean {
  return role !== 'CUSTOMER';
}

export async function middleware(request: NextRequest) {
  const { pathname } = request.nextUrl;
  const accessToken = request.cookies.get('accessToken')?.value;

  // Allow public routes
  if (isPublicRoute(pathname)) {
    // If user is already authenticated and trying to access login/register/staff-login, redirect appropriately
    if (accessToken && (pathname === '/login' || pathname === '/register' || pathname === '/staff/login')) {
      const validation = await validateToken(accessToken);
      if (validation.valid && validation.role) {
        const redirectUrl = isStaffRole(validation.role) ? '/dashboard' : '/customer/home';
        return NextResponse.redirect(new URL(redirectUrl, request.url));
      }
    }
    return NextResponse.next();
  }

  // Protected routes require authentication
  if (!accessToken) {
    const loginUrl = new URL('/login', request.url);
    loginUrl.searchParams.set('redirect', pathname);
    return NextResponse.redirect(loginUrl);
  }

  // Validate token with backend
  const validation = await validateToken(accessToken);

  if (!validation.valid) {
    // Token is invalid, clear cookies and redirect to login
    const response = NextResponse.redirect(new URL('/login', request.url));
    response.cookies.delete('accessToken');
    response.cookies.delete('refreshToken');
    return response;
  }

  // Role-based access control
  const userRole = validation.role;

  if (!userRole) {
    // If role is missing, redirect to login
    const response = NextResponse.redirect(new URL('/login', request.url));
    response.cookies.delete('accessToken');
    response.cookies.delete('refreshToken');
    return response;
  }

  // Staff routes - only allow staff roles
  if (isStaffRoute(pathname) && !isStaffRole(userRole)) {
    return NextResponse.redirect(new URL('/customer/home', request.url));
  }

  // Customer routes - only allow customer role
  if (isCustomerRoute(pathname) && isStaffRole(userRole)) {
    return NextResponse.redirect(new URL('/dashboard', request.url));
  }

  // Allow access
  return NextResponse.next();
}

// Configure which routes the middleware should run on
export const config = {
  matcher: [
    /*
     * Match all request paths except:
     * - _next/static (static files)
     * - _next/image (image optimization files)
     * - favicon.ico (favicon file)
     * - public folder files
     */
    '/((?!_next/static|_next/image|favicon.ico|.*\\.(?:svg|png|jpg|jpeg|gif|webp)$).*)',
  ],
};
