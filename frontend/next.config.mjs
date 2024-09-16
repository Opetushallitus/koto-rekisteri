import config from './config.json' with { type: "json"}

/** @type {import('next').NextConfig} */
const nextConfig = {
  reactStrictMode: true,

  // static export is disabled for local development.
  // TODO: uncomment the line before it goes to production.
  // https://nextjs.org/docs/app/building-your-application/deploying/static-exports
  // output: "export",

  distDir: "out",

  trailingSlash: true,

  // The build fails without this.
  images: {
    unoptimized: true,
  },

  // NOTE:
  // when doing local development,
  // when the react app (frontend) is served from port 3000,
  // CORS is needed to allow access to port 8080
  // On production, the app is served within backend, so CORS is not needed.
  headers: async () => {
    return [
      {
        source: "/oppijanumerorekisteri",
        headers: [
          { key: "Access-Control-Allow-Origin", value: config.apiUrl },
        ],
      },
    ]
  },
}

export default nextConfig
