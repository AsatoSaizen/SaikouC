package ani.saikouc.profile

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import ani.saikouc.databinding.ActivitySingleStatBinding
import ani.saikouc.getThemeColor
import ani.saikouc.initActivity
import ani.saikouc.themes.ThemeManager
import ani.saikouc.toast
import com.github.aachartmodel.aainfographics.aachartcreator.AAOptions

class SingleStatActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySingleStatBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager(this).applyTheme()
        initActivity(this)
        binding = ActivitySingleStatBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val chartOptions = chartOptions
        if (chartOptions != null) {
            chartOptions.chart?.backgroundColor = getThemeColor(android.R.attr.windowBackground)
            binding.chartView.aa_drawChartWithChartOptions(chartOptions)
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } else {
            toast("No chart data")
            finish()
        }
    }

    companion object {
        var chartOptions: AAOptions? = null  // I cba to pass this through an intent
    }
}