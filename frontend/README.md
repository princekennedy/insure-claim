# InsureClaim Frontend

Next.js 16.3.8 frontend for the InsureClaim Portal motor insurance claims platform.

## Tech Stack

- **Framework:** Next.js 16.3.8 (App Router)
- **React:** 19.2.8
- **Language:** TypeScript 5
- **Styling:** Tailwind CSS 4
- **State Management:** React Hooks + Context

## Project Structure

```
frontend/
├── app/
│   ├── (auth)/            # Authentication pages
│   │   ├── login/         # Login page
│   │   └── register/      # Registration page
│   ├── (dashboard)/       # Dashboard pages (protected)
│   │   ├── layout.tsx     # Dashboard layout with sidebar
│   │   ├── dashboard/     # Main dashboard
│   │   ├── claims/        # Claims list & detail pages
│   │   ├── vehicles/      # Vehicle management
│   │   ├── policies/      # Policy management
│   │   ├── kyc/           # KYC verification
│   │   ├── garages/       # Garage browsing
│   │   └── feedback/      # Feedback management
│   ├── api/               # API route handlers (optional)
│   ├── layout.tsx         # Root layout
│   └── page.tsx           # Home redirect
├── lib/
│   ├── components/
│   │   ├── ui/            # Reusable UI components
│   │   │   ├── button.tsx
│   │   │   ├── input.tsx
│   │   │   ├── select.tsx
│   │   │   ├── textarea.tsx
│   │   │   ├── card.tsx
│   │   │   ├── badge.tsx
│   │   │   ├── avatar.tsx
│   │   │   ├── progress-bar.tsx
│   │   │   ├── spinner.tsx
│   │   │   ├── empty-state.tsx
│   │   │   ├── page-header.tsx
│   │   │   ├── alert.tsx
│   │   │   └── dialog.tsx
│   │   ├── layout/        # Layout components
│   │   │   ├── app-layout.tsx   # Main app layout with sidebar
│   │   │   └── auth-layout.tsx  # Auth pages layout
│   │   ├── claims/        # Claims-specific components
│   │   ├── garage/        # Garage-specific components
│   │   └── kyc/           # KYC-specific components
│   ├── services/          # API services
│   │   ├── api.ts         # Base API client
│   │   ├── auth.ts        # Authentication service
│   │   ├── claims.ts      # Claims service
│   │   ├── vehicles.ts    # Vehicles service
│   │   ├── policies.ts    # Policies service
│   │   ├── kyc.ts         # KYC service
│   │   └── garages.ts     # Garages & feedback service
│   └── types/             # TypeScript types
│       ├── auth.ts        # Auth types
│       ├── vehicle.ts     # Vehicle types
│       ├── policy.ts      # Policy types
│       ├── claim.ts       # Claim types
│       ├── garage.ts      # Garage types
│       ├── kyc.ts         # KYC types
│       └── feedback.ts    # Feedback types
├── public/                # Static assets
├── next.config.ts         # Next.js configuration
├── tailwind.config.ts     # Tailwind configuration
└── tsconfig.json          # TypeScript configuration
```

## Getting Started

```bash
# Install dependencies
npm install

# Run development server
npm run dev

# Open http://localhost:3000
```

## Available Scripts

| Command | Description |
|---------|-------------|
| `npm run dev` | Start development server |
| `npm run build` | Build for production |
| `npm run start` | Start production server |
| `npm run lint` | Run ESLint |

## Environment Variables

Create a `.env.local` file in the frontend directory:

```env
NEXT_PUBLIC_API_URL=http://localhost:8080
```

## Components

### UI Components

All UI components are in `lib/components/ui/` and follow a consistent pattern:

- **Button:** Primary, secondary, danger, ghost variants with loading state
- **Input:** Text input with label, error, and hint support
- **Select:** Dropdown with custom styling
- **Textarea:** Multi-line text input
- **Card:** Container with header, content, and footer slots
- **Badge:** Status labels with color variants
- **Avatar:** User avatar with initials fallback
- **ProgressBar:** Progress indicator with percentage
- **Spinner:** Loading indicator
- **EmptyState:** Placeholder for empty lists
- **PageHeader:** Page title with optional actions
- **Alert:** Info, success, warning, error messages
- **Dialog:** Modal dialog with overlay

### Service Layer

The API service layer (`lib/services/`) provides typed methods for all backend endpoints with automatic authentication handling.

```typescript
import { login, getMyClaims } from '@/lib/services';

// Authentication
const { accessToken, refreshToken } = await login({
  email: 'user@example.com',
  password: 'password123',
});

// API calls (tokens managed automatically)
const claims = await getMyClaims(0, 20);
```

## Integration with Backend

The frontend expects a running backend at the URL specified by `NEXT_PUBLIC_API_URL`. See the root [readme.md](../readme.md) for backend setup instructions.

## License

Proprietary - Britam Insurance PLC
