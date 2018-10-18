package edu.artic.db

/**
 * Used to ensure content that requires content description can use this interface
 */
interface AccessibilityAware {
    fun getContentDescription(): String
}