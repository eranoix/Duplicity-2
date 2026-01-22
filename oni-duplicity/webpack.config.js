"use strict";
const path = require("path");

const webpack = require("webpack");

const HtmlWebpackPlugin = require("html-webpack-plugin");
const WebpackPwaManifest = require("webpack-pwa-manifest");
const WorkboxPlugin = require("workbox-webpack-plugin");

const isDev = process.env["NODE_ENV"] === "development";

const root = path.resolve(__dirname);

const PATHS = {
  appPackageJson: path.resolve(root, "package.json"),
  appSrc: path.resolve(root, "./src"),
  appDist: path.resolve(root, "./dist"),
  nodeModules: path.resolve(root, "./node_modules"),
  changelog: path.resolve(root, "./CHANGELOG.md")
};

const { friendlyName, description } = require(PATHS.appPackageJson);

// Configuração do caminho base para GitHub Pages
// Para repositório: username.github.io/Duplicity-2
const PUBLIC_URL_PATH = "/Duplicity-2/";

console.log("Webpack build", isDev ? "[development]" : "[production]");

module.exports = {
  mode: isDev ? "development" : "production",

  devtool: "source-map",
  
  // Ignore warnings from source-map-loader
  ignoreWarnings: [
    /Failed to parse source map/,
    /Can't resolve .* in .*node_modules/
  ],

  devServer: {
    contentBase: PATHS.appDist,
    hot: isDev,
    historyApiFallback: true
  },

  entry: {
    client: [path.join(PATHS.appSrc, "./index.tsx")]
  },

  output: {
    filename: "[name].[hash].bundle.js",
    path: PATHS.appDist,
    publicPath: isDev ? "/" : PUBLIC_URL_PATH,

    // Fix hot-reload interfering with worker-loader
    globalObject: "this"
  },

  resolve: {
    // Add '.ts' and '.tsx' as resolvable extensions.
    extensions: [".ts", ".tsx", ".js", ".json"],
    modules: [
      PATHS.nodeModules,
      path.resolve(root, "../node_modules"),
      "node_modules"
    ],
    alias: {
      "@": PATHS.appSrc,
      "@changelog": PATHS.changelog,
      "pako": path.resolve(PATHS.nodeModules, "pako/dist/pako.min.js")
    }
  },

  module: {
    rules: [
      // Process source maps in input sources
      //  All output '.js' files will have any sourcemaps re-processed by 'source-map-loader'.
      {
        enforce: "pre",
        test: /\.(jsx?|tsx?)$/,
        loader: "source-map-loader",
        include: [/src\/.+\.tsx?/]
      },

      {
        test: /\.tsx?$/,
        use: [
          {
            loader: "ts-loader",
            options: {
              transpileOnly: true,
              compilerOptions: {
                skipLibCheck: true
              },
              logLevel: "info"
            }
          }
        ],
        exclude: /node_modules\/(?!oni-save-parser)/
      },

      {
        test: /\.css$/,
        loader: ["style-loader", "css-loader"]
      },

      {
        test: /\.(woff|woff2)$/,
        use: {
          loader: "url-loader",
          options: {
            name: "fonts/[hash].[ext]",
            limit: 5000,
            mimetype: "application/font-woff"
          }
        }
      },
      {
        test: /\.(ttf|eot|svg)$/,
        use: {
          loader: "file-loader",
          options: {
            name: "fonts/[hash].[ext]"
          }
        }
      },

      {
        test: /\.png/,
        loader: "file-loader",
        options: {
          name: "images/[hash].[ext]"
        }
      },

      {
        test: /\.(txt|md)$/,
        loader: "raw-loader"
      }
    ]
  },

  plugins: [
    new webpack.DefinePlugin({
      "process.env": {
        NODE_ENV: JSON.stringify(isDev ? "development" : "production")
      }
    }),

    new HtmlWebpackPlugin({
      inject: true,
      template: path.resolve(PATHS.appSrc, "index.ejs")
    }),

    new WebpackPwaManifest({
      name: `${friendlyName}: ${description}`,
      short_name: friendlyName,
      description,
      background_color: "#000000",
      crossorigin: null,
      display: "standalone",
      inject: true
    }),

    new WorkboxPlugin.GenerateSW({
      clientsClaim: true,
      skipWaiting: true
    }),

    // Ignore Node.js built-in modules
    new webpack.NormalModuleReplacementPlugin(
      /^node:util$/,
      require.resolve('./src/node-stubs/util.js')
    ),
    new webpack.NormalModuleReplacementPlugin(
      /^node:zlib$/,
      require.resolve('./src/node-stubs/zlib.js')
    )
  ],

  optimization: {
    runtimeChunk: true,
    splitChunks: {
      chunks: "all",
      cacheGroups: {
        npm: {
          test: /node_modules/,
          name: mod => {
            const relToModule = path.relative(PATHS.nodeModules, mod.context);
            const moduleName = relToModule.substring(
              0,
              relToModule.indexOf(path.sep)
            );
            return `npm.${moduleName}`;
          }
        }
      }
    }
  }
};
