// next.config.js

const path = require("path");

const withPWA = require("next-pwa")({
  dest: "public",
  register: true,
  skipWaiting: true,
  sw: "sw.js",

  // Disable PWA in development
  disable: process.env.NODE_ENV === "development",

  runtimeCaching: [
    // ── API routes — NetworkFirst ───────────────────────────────────────────
    {
      urlPattern: /^\/api\/goals/,
      handler: "NetworkFirst",
      options: {
        cacheName: "goals-api-cache",
        expiration: {
          maxEntries: 50,
          maxAgeSeconds: 5 * 60,
        },
        networkTimeoutSeconds: 4,
      },
    },

    {
      urlPattern: /^\/api\/users/,
      handler: "NetworkFirst",
      options: {
        cacheName: "users-api-cache",
        expiration: {
          maxAgeSeconds: 10 * 60,
        },
        networkTimeoutSeconds: 4,
      },
    },

    // ── Static assets — CacheFirst ──────────────────────────────────────────
    {
      urlPattern: /\/_next\/static\/.*/,
      handler: "CacheFirst",
      options: {
        cacheName: "next-static",
        expiration: {
          maxAgeSeconds: 30 * 24 * 60 * 60,
        },
      },
    },

    // ── Images — StaleWhileRevalidate ───────────────────────────────────────
    {
      urlPattern: /\/_next\/image\?.*/,
      handler: "StaleWhileRevalidate",
      options: {
        cacheName: "next-images",
        expiration: {
          maxAgeSeconds: 7 * 24 * 60 * 60,
        },
      },
    },

    // ── HTML pages — NetworkFirst ───────────────────────────────────────────
    {
      urlPattern: ({ request }) => request.mode === "navigate",
      handler: "NetworkFirst",
      options: {
        cacheName: "pages-cache",
        expiration: {
          maxAgeSeconds: 60 * 60,
        },
        networkTimeoutSeconds: 3,
      },
    },
  ],
});

/** @type {import("next").NextConfig} */
const nextConfig = {
  eslint: {
    ignoreDuringBuilds: true,  // ← add this
  },
  reactStrictMode: true,

  outputFileTracingRoot: path.join(__dirname),
};


module.exports = withPWA(nextConfig);