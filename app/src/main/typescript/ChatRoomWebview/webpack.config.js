const path = require("path");
const HtmlWebpackPlugin = require("html-webpack-plugin");
const MiniCssExtractPlugin = require("mini-css-extract-plugin");
const CssMinimizerPlugin = require("css-minimizer-webpack-plugin");
const { DefinePlugin } = require("webpack");

module.exports = (env, argv) => {
  const isProd = argv.mode === "production";
  const envDebug = process.env.DEBUG === "true" || process.env.DEBUG === "1";
  const debugFlag = envDebug || !isProd;

  return {
    entry: "./src/index.ts",
    output: {
      filename: "bundle.js",
      path: path.resolve(__dirname, "../../assets"),
      clean: true,
    },
    devtool: isProd ? "source-map" : "eval-source-map",
    resolve: {
      extensions: [".ts", ".js"],
    },
    module: {
      rules: [
        {
          test: /\.ts$/,
          use: [
            {
              loader: "minify-html-literals-loader",
              options: { minifyCSS: true },
            },
            "ts-loader",
          ],
          exclude: /node_modules/,
        },
        {
          test: /\.css$/i,
          use: [MiniCssExtractPlugin.loader, "css-loader"],
        },
      ],
    },
    plugins: [
      new DefinePlugin({
        DEBUG: JSON.stringify(debugFlag),
      }),
      new HtmlWebpackPlugin({ template: "./public/index.html" }),
      new MiniCssExtractPlugin({ filename: "styles.css" }),
    ],
    optimization: {
      minimize: isProd,
      minimizer: ["...", new CssMinimizerPlugin()],
    },
    devServer: {
      static: {
        directory: path.join(__dirname, "public"),
      },
      open: true,
      port: 3000,
      hot: true,
      historyApiFallback: true,
    },
  };
};
