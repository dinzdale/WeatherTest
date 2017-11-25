package widgets

import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.support.v7.widget.AppCompatAutoCompleteTextView
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.MultiAutoCompleteTextView
import android.widget.TextView


class ComboBox : AppCompatAutoCompleteTextView {

    private var clientClickListener: View.OnClickListener? = null

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    fun setClientClickListener(clientClickListener: View.OnClickListener) {
        this.clientClickListener = clientClickListener
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        setOnEditorActionListener { v, actionId, event ->
            var retValue = false
            if (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER || actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
                retValue = true
                dismissKeyboard()
                dismissDropDown()
                clientClickListener?.let {
                    it.onClick(v)
                }
            }
            retValue
        }


        setAdapter(arrayAdapter)
        onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                if (clientClickListener != null) {
                    clientClickListener!!.onClick(view)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {

            }
        }

    }

    private fun dismissKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(windowToken, 0)
    }

    override fun performFiltering(text: CharSequence, keyCode: Int) {
        super.performFiltering("", 0)
    }

    private val arrayAdapter = ArrayAdapter<String>(context, android.R.layout.simple_dropdown_item_1line)

    val cannedList = arrayListOf<String>("11803,08057,22334")

    fun getCurrentText() : String? {
        return text.toString()
    }


}
