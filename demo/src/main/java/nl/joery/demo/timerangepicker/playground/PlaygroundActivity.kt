package nl.joery.demo.timerangepicker.playground

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.Spanned
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_playground.*
import nl.joery.demo.timerangepicker.ExampleActivity
import nl.joery.demo.timerangepicker.R
import nl.joery.demo.timerangepicker.playground.properties.*
import nl.joery.timerangepicker.TimeRangePicker


class PlaygroundActivity : AppCompatActivity() {
    private lateinit var properties: ArrayList<Property>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playground)

        initProperties()
        initRecyclerView()

        view_xml.setOnClickListener {
            showXmlDialog()
        }

        open_examples.setOnClickListener {
            startActivity(Intent(this, ExampleActivity::class.java))
        }
    }

    private fun initProperties() {
        properties = ArrayList()
        properties.add(
            CategoryProperty(
                "Time"
            )
        )
        properties.add(
            EnumProperty(
                "hourFormat",
                TimeRangePicker.HourFormat::class.java
            )
        )
        properties.add(
            IntegerProperty(
                "stepTimeMinutes"
            )
        )

        properties.add(
            CategoryProperty(
                "Slider"
            )
        )
        properties.add(
            IntegerProperty(
                "sliderWidth",
                false,
                TypedValue.COMPLEX_UNIT_DIP
            )
        )
        properties.add(
            ColorProperty(
                "sliderColor"
            )
        )
        properties.add(
            ColorProperty(
                "sliderRangeColor"
            )
        )
        properties.add(
            ColorProperty(
                "sliderRangeGradientStart"
            )
        )
        properties.add(
            ColorProperty(
                "sliderRangeGradientMiddle"
            )
        )
        properties.add(
            ColorProperty(
                "sliderRangeGradientEnd"
            )
        )


        properties.add(
            CategoryProperty(
                "Thumb"
            )
        )
        properties.add(
            IntegerProperty(
                "thumbSize",
                false,
                TypedValue.COMPLEX_UNIT_DIP
            )
        )
        properties.add(
            IntegerProperty(
                "thumbSizeActiveGrow",
                true
            )
        )
        properties.add(
            ColorProperty(
                "thumbColor"
            )
        )
        properties.add(
            ColorProperty(
                "thumbIconColor"
            )
        )
        properties.add(
            IntegerProperty(
                "thumbIconSize",
                false,
                TypedValue.COMPLEX_UNIT_DIP
            )
        )


        properties.add(
            CategoryProperty(
                "Clock"
            )
        )
        properties.add(
            BooleanProperty(
                "clockVisible"
            )
        )
        properties.add(
            EnumProperty(
                "clockFace",
                TimeRangePicker.ClockFace::class.java
            )
        )
        properties.add(
            IntegerProperty(
                "clockLabelSize",
                false,
                TypedValue.COMPLEX_UNIT_SP
            )
        )
        properties.add(
            ColorProperty(
                "clockLabelColor"
            )
        )
        properties.add(
            ColorProperty(
                "clockTickColor"
            )
        )
    }

    private fun initRecyclerView() {
        recycler.layoutManager =
            LinearLayoutManager(applicationContext, LinearLayoutManager.VERTICAL, false)
        recycler.adapter = PropertyAdapter(picker, properties)
    }

    private fun showXmlDialog() {
        val html = XmlGenerator.generateHtmlXml(
            "nl.joery.timerangepicker.TimeRangepicker",
            "trp",
            picker,
            properties,
            arrayOf(TimeRangePicker(this))
        )

        val layout = LayoutInflater.from(this).inflate(R.layout.view_generated_xml, null)
        val textView = layout.findViewById<TextView>(R.id.xml)
        textView.setHorizontallyScrolling(true)
        textView.text = htmlToSpanned(html)

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.generate_xml_title)
            .setView(layout)
            .setPositiveButton(R.string.copy_to_clipboard) { _, _ ->
                val clipboard =
                    getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip =
                    ClipData.newPlainText(getString(R.string.generate_xml_title), htmlToText(html))
                clipboard.setPrimaryClip(clip)

                Snackbar.make(
                    findViewById<View>(android.R.id.content),
                    R.string.copied_xml_clipboard,
                    Snackbar.LENGTH_LONG
                ).show()
            }
            .show()
    }

    private fun htmlToText(html: String): String {
        return htmlToSpanned(html).toString().replace("\u00A0", " ")
    }

    private fun htmlToSpanned(html: String): Spanned {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)
        } else {
            @Suppress("DEPRECATION")
            Html.fromHtml(html)
        }
    }
}