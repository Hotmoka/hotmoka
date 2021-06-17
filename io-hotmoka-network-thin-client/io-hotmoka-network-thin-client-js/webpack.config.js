const path = require('path');
const webpack = require("webpack");

module.exports = {
    entry: './src/index.ts',
    module: {
        rules: [
            {
                test: /\.tsx?$/,
                use: 'ts-loader',
                exclude: /node_modules/,
            },
        ],
    },
    resolve: {
        extensions: ['.ts', '.js'],
        fallback: {
            "stream": false,
            "crypto": require.resolve('crypto-browserify')
        }
    },
    output: {
        path: path.resolve(__dirname, 'dist'),
        filename: 'hotmoka.js',
        library: 'hotmoka',
        libraryTarget:'umd',
        globalObject: 'this'
    },
    target: 'web',
    plugins: [
        new webpack.ProvidePlugin({
            Buffer: ['buffer', 'Buffer']
        })
    ]
};