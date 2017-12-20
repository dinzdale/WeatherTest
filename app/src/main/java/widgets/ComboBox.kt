package widgets

import android.content.Context
import android.graphics.Rect
import android.location.Address
import android.support.v7.widget.AppCompatAutoCompleteTextView
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import model.formatAddress


class ComboBox : AppCompatAutoCompleteTextView {

    var listener: View.OnClickListener? = null

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)


    init {

        onItemClickListener = object : AdapterView.OnItemClickListener {
            override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                performCompletion()
            }
        }

        onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                performCompletion()
            }
        }

    }

    override fun setOnClickListener(listener: OnClickListener?) {
        this.listener = listener
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        setOnEditorActionListener { v, actionId, event ->
            var retValue = false
            if (actionId == KeyEvent.KEYCODE_CALL || actionId == KeyEvent.KEYCODE_ENDCALL) {
                (adapter as ArrayAdapter<String>).clear()
                retValue = true
                dismissKeyboard()
                dismissDropDown()
                listener?.onClick(v)
            }
            retValue
        }


        setAdapter(ArrayAdapter<String>(context, android.R.layout.simple_dropdown_item_1line, arrayOf()))


    }

    fun updateComboBoxSelections(addresses: Array<Address>) {
        val newList = MutableList<String>(addresses.size, { index -> addresses[index].formatAddress() })
        val theAdapter = adapter as ArrayAdapter<String>
        theAdapter.clear()
        theAdapter.addAll(newList)
        theAdapter.notifyDataSetChanged()
        performFiltering("", 0)
        showDropDown()
        showKeyboard()
    }


    private fun dismissKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(windowToken, 0)
    }

    private fun showKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT);
    }

    fun getCurrentText(): String? {
        return text.toString()
    }


}
