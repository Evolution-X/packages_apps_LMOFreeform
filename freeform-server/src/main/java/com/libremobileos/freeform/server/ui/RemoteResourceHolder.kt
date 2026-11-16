package com.libremobileos.freeform.server.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.CONTEXT_IGNORE_SECURITY
import android.content.Context.CONTEXT_INCLUDE_CODE
import android.graphics.drawable.Drawable
import android.util.Slog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

@SuppressLint("WrongConstant", "DiscouragedApi")
class RemoteResourceHolder(context: Context, private val resPkg: String) {

    private lateinit var remoteContext: Context

    companion object {
        private const val TAG = "LMOFreeform/RemoteResourceHolder"
    }

    init {
        try {
            remoteContext = context.createPackageContext(resPkg, CONTEXT_INCLUDE_CODE or CONTEXT_IGNORE_SECURITY)
        } catch (e: Exception) {
            e.printStackTrace()
            Slog.e(TAG, "init", e)
            runCatching { remoteContext = context.createPackageContext(resPkg, CONTEXT_INCLUDE_CODE or CONTEXT_IGNORE_SECURITY) }
        }
    }

    fun getLayout(layoutName: String): ViewGroup? {
        return try {
            val freeformLayoutId = remoteContext.resources.getIdentifier(layoutName, "layout", resPkg)
                val r = LayoutInflater.from(remoteContext).inflate(freeformLayoutId, null, false)
            if (null == r) Slog.e(TAG, "can not find layout $layoutName")
            r as ViewGroup
        } catch (e: Exception) {
            e.printStackTrace()
            Slog.e(TAG, "getLayout", e)
            null
        }
    }

    fun <T : View> getLayoutChildViewByTag(layout: ViewGroup, tagName: String): T? {
        return try {
            val r = layout.findViewWithTag<T>(tagName)
            if (null == r) Slog.e(TAG, "can not find tag $tagName in $layout")
            r
        } catch (e: Exception) {
            e.printStackTrace()
            Slog.e(TAG, "getLayoutChildViewByTag", e)
            null
        }
    }

    fun getDrawable(drawableName: String): Drawable? {
        return try {
            val drawableId = remoteContext.resources.getIdentifier(drawableName, "drawable", resPkg)
            if (drawableId != 0) remoteContext.getDrawable(drawableId) else null
        } catch (e: Exception) {
            e.printStackTrace()
            Slog.e(TAG, "getDrawable", e)
            null
        }
    }

    fun getColor(colorName: String): Int {
        return try {
            val colorId = remoteContext.resources.getIdentifier(colorName, "color", resPkg)
            if (colorId != 0) remoteContext.getColor(colorId) else 0
        } catch (e: Exception) {
            e.printStackTrace()
            Slog.e(TAG, "getColor", e)
            0
        }
    }
}
