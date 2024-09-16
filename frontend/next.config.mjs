/** @type {import('next').NextConfig} */
const nextConfig = {
  reactStrictMode: true,

  // https://nextjs.org/docs/app/building-your-application/deploying/static-exports
  output: "export",

  // point the files straight to maven's target files.
  distDir: "../server/target/classes/static",

  trailingSlash: true,

  // The build fails without this.
  images: {
    unoptimized: true,
  },
};

export default nextConfig;
