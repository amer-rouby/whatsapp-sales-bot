import type { NextConfig } from "next";
import pkg from "./package.json";

const nextConfig: NextConfig = {
  // The release version is shown in the dashboard; package.json is its single source.
  env: {
    NEXT_PUBLIC_APP_VERSION: pkg.version,
  },
};

export default nextConfig;
