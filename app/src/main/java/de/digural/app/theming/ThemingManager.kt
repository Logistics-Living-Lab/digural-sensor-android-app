package de.digural.app.theming

import android.content.Context
import android.content.res.Resources.NotFoundException
import android.util.Log
import android.util.TypedValue
import androidx.core.content.ContextCompat

class ThemingManager {

    private val LOG_TAG = ThemingManager::class.java.name

    fun getThemeColorResourceId(context: Context?, colorAttributeId: Int): Int {
        if (context === null) {
            Log.w(LOG_TAG, "Context is null")
            return -1
        }
        val theme = context.theme
        val typedValue = TypedValue();
        theme.resolveAttribute(colorAttributeId, typedValue, true);
        return typedValue.resourceId
    }

    fun getThemeColor(context: Context?, colorAttributeId: Int): Int {
        if (context === null) {
            Log.w(LOG_TAG, "Context is null")
            return -1
        }
        return try {
            getColor(context, getThemeColorResourceId(context, colorAttributeId))
        } catch (e: NotFoundException) {
            Log.w(LOG_TAG, "Color not found: $colorAttributeId")
            -1
        }
    }

    fun getColor(context: Context?, colorId: Int): Int {
        if (context === null) {
            Log.w(LOG_TAG, "Context is null - getColor")
            return -1
        }
        return ContextCompat.getColor(context, colorId)
    }
}