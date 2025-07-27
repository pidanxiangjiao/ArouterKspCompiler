package cn.jailedbird.arouter.ksp.binding
import android.widget.TextView
import androidx.databinding.BindingAdapter

object  DataBindingAdapters {

    @BindingAdapter("formattedText")
    @JvmStatic
    fun setFormattedText(view: TextView, value: Int) {
        view.text = "Count: $value"
    }
}