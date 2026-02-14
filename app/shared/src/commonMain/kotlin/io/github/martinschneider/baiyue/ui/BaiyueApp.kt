package io.github.martinschneider.baiyue.ui

import androidx.compose.runtime.Composable

/**
 * Common app entry point marker.
 * The actual navigation host is platform-specific because it depends
 * on the navigation library and map implementation.
 */
expect @Composable fun BaiyueApp()
