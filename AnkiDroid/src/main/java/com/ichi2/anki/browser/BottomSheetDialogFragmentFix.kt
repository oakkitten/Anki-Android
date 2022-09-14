package com.ichi2.anki.browser

import android.content.Context
import android.content.res.Configuration
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.ichi2.anki.UIUtils

// https://stackoverflow.com/questions/41591733
open class BottomSheetDialogFragmentFix : BottomSheetDialogFragment() {
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        fixBottomSheetPeekHeight() // After device rotation
    }

    override fun onResume() {
        super.onResume()
        fixBottomSheetPeekHeight() // When showing for the first time
    }

    private fun fixBottomSheetPeekHeight() {
        (dialog as? BottomSheetDialog)?.behavior?.peekHeight =
            if (requireContext().isLandscape) {
                UIUtils.convertDpToPixel(512f, requireContext()).toInt()
            } else {
                BottomSheetBehavior.PEEK_HEIGHT_AUTO
            }
    }
}

private val Context.isLandscape get() =
    resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
