package com.example.favorites

import com.appmarket.blog.plugin.sdk.JavaPlugin
import com.appmarket.blog.plugin.sdk.PluginContext

/**
 * 收藏夹插件示例：本地收藏应用市场内的应用，不发起任何网络请求。
 */
class FavoritesPlugin : JavaPlugin {
    private lateinit var ctx: PluginContext

    override fun onCreate(context: PluginContext) {
        ctx = context
        ctx.registerEntry("我的收藏") {
            val favorites = ctx.getStringSet("favorites").toList()
            android.util.Log.i("Favorites", "收藏 ${favorites.size} 个应用")
        }
    }

    override fun onDestroy() {
        // 插件卸载时调用，释放资源
    }

    /** 业务方法：收藏一个应用 */
    fun favorite(appId: String) {
        val current = ctx.getStringSet("favorites").toMutableSet()
        current.add(appId)
        ctx.putStringSet("favorites", current)
    }

    /** 业务方法：取消收藏 */
    fun unfavorite(appId: String) {
        val current = ctx.getStringSet("favorites").toMutableSet()
        current.remove(appId)
        ctx.putStringSet("favorites", current)
    }
}
