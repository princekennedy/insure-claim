# Build stage: install dependencies and build the Next.js application
FROM node:22-alpine AS builder

WORKDIR /build

# Copy package files first for better layer caching
COPY package.json package-lock.json ./
COPY next.config.ts ./
COPY tsconfig.json ./
COPY postcss.config.mjs ./
COPY eslint.config.mjs ./

# Copy source code
COPY app ./app
COPY public ./public

# Install dependencies and build
RUN npm ci && npm run build

# Production stage: run the optimized Next.js server
FROM node:22-alpine

WORKDIR /app

# Create a non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy built artifacts from builder stage
COPY --from=builder /build/.next ./.next
COPY --from=builder /build/public ./public
COPY --from=builder /build/package.json ./
COPY --from=builder /build/node_modules ./node_modules
COPY --from=builder /build/next.config.ts ./

# Switch to non-root user
USER appuser

# Expose the Next.js port
EXPOSE 3000

# Health check
HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
  CMD wget -qO- http://localhost:3000 || exit 1

# Set environment variable for production
ENV NODE_ENV=production

# Start the Next.js server
CMD ["npm", "start"]
