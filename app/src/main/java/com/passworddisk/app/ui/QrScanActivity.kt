package com.passworddisk.app.ui

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.journeyapps.barcodescanner.CaptureActivity
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import com.passworddisk.app.R

/**
 * 自定义扫码页。
 *
 * 替换 ZXing 默认的 CaptureActivity：
 * - 竖屏固定（清单里声明 portrait，配合 [ScanOptions] 的方向锁定）；
 * - 关闭 ZXing 自带的方形暗角与红线，交给 [ScanOverlayView] 画圆角取景窗与扫描光带；
 * - 顶部加标题与关闭按钮、底部加提示语与手电筒，避免用户面对一个「只有取景框」的黑屏。
 */
class QrScanActivity : CaptureActivity() {

    private lateinit var barcodeView: DecoratedBarcodeView
    private var torchOn = false

    override fun initializeContent(): DecoratedBarcodeView {
        setContentView(R.layout.activity_qr_scan)
        barcodeView = findViewById(R.id.zxing_barcode_scanner)

        // 自绘遮罩接管视觉，ZXing 的暗角与红线保持「什么都不画」
        barcodeView.viewFinder.setMaskColor(Color.TRANSPARENT)
        barcodeView.viewFinder.setLaserVisibility(false)
        barcodeView.statusView?.visibility = View.GONE

        return barcodeView
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        applySystemBarInsets()

        findViewById<View>(R.id.scanCloseButton).setOnClickListener { finish() }
        findViewById<View>(R.id.scanTorchButton).setOnClickListener { toggleTorch() }
    }

    private fun applySystemBarInsets() {
        val topBar = findViewById<View>(R.id.scanTopBar)
        val bottomBar = findViewById<View>(R.id.scanBottomBar)
        val topBase = topBar.paddingTop
        val bottomBase = bottomBar.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.scanOverlay)) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            topBar.updatePadding(top = topBase + bars.top)
            bottomBar.updatePadding(bottom = bottomBase + bars.bottom)
            insets
        }
    }

    private fun toggleTorch() {
        torchOn = !torchOn
        if (torchOn) barcodeView.setTorchOn() else barcodeView.setTorchOff()

        findViewById<ImageView>(R.id.scanTorchButton).apply {
            imageTintList = ColorStateList.valueOf(if (torchOn) BRAND_GREEN else Color.WHITE)
            contentDescription = getString(
                if (torchOn) R.string.scan_torch_off else R.string.scan_torch_on
            )
        }
    }

    private companion object {
        const val BRAND_GREEN = 0xFF0E9F6E.toInt()
    }
}
