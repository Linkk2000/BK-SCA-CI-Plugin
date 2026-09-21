const path = require('path')
const webpack = require('webpack')
const HtmlWebpackPlugin = require('html-webpack-plugin')
const VueLoaderPlugin = require('vue-loader/lib/plugin')

const resolve = dist => {
    return path.resolve(__dirname, dist)
}

module.exports = (env, argv) => {
    const isProd = argv.mode === 'production'
    
    return {
        devtool: isProd ? false : '#source-map',  // 生产环境不生成 source map
        entry: {
            remoteAtom: './src/main.js'
        },
        output: {
            publicPath: isProd ? './' : '/',
            filename: '[name].[contentHash].js',
            chunkFilename: 'js/[name].[chunkHash:8].js'
        },
        resolve: {
            extensions: ['.js', '.vue', '.json', '.css', '.scss'],
            alias: {
                'vue': 'vue/dist/vue.esm.js',
                '@': resolve('src')
            }
        },
        module: {
            rules: [
                {
                    test: /\.vue$/,
                    include: resolve('src'),
                    use: 'vue-loader'
                },
                {
                    test: /\.js$/,
                    include: path.resolve('src'),
                    use: [
                        {
                            loader: 'babel-loader'
                        }
                    ]
                },
                {
                    test: /\.css$/,
                    loader: ['style-loader', 'css-loader']
                },
                {
                    test: /.scss$/,
                    use: ['style-loader', 'css-loader', 'sass-loader']
                },
                {
                    test: /\.(png|jpe?g|gif|svg|cur)(\?.*)?$/,
                    loader: 'url-loader'
                },
                {
                    test: /\.(woff2?|eot|ttf|otf)(\?.*)?$/,
                    loader: 'url-loader'
                }
            ]
        },
        plugins: [
            new VueLoaderPlugin(),
            new webpack.DefinePlugin({
                // IMPORTANT: keep this as a boolean literal (not a string),
                // so code can safely do `ISLOCAL === true` checks.
                ISLOCAL: !isProd,
                // 插件使用文档地址（内部 xwiki），编译时从环境变量注入，不进仓库；为空时前端不显示入口
                WIKI_URL: JSON.stringify(process.env.WIKI_URL || '')
            }),
            new HtmlWebpackPlugin({
                filename: 'index.html',
                template: './src/index.html',
                inject: true
            })
        ],
        devServer: {
            port: 8001,
            contentBase: path.join(__dirname, 'src'), // 开发时直接指向源码目录
            historyApiFallback: {
                index: '/index.html'
            },
            noInfo: false,
            disableHostCheck: true
        }
    }
}
