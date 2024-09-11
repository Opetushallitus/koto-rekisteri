/** @type {import('next').NextConfig} */
const nextConfig = {
  reactStrictMode: true,

  // https://nextjs.org/docs/app/building-your-application/deploying/static-exports
  output: "export",

  // The build fails without this.
  images: {
    unoptimized: true
  }
};

export default nextConfig;
