package widgets;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;


public class ComboBox extends AutoCompleteTextView {

    private OnClickListener clientClickListener;


   // private String[] testValue = {"CURRENT LOCATION", "08057", "Lisbon", "Plainview, NY", "Plainview, TX"};

    public ComboBox(Context context) {
        super(context);
    }

    public ComboBox(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ComboBox(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setClientClickListener(OnClickListener clientClickListener) {
        this.clientClickListener = clientClickListener;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean retValue = false;
                if (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER || actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
                    retValue = true;
                    dismissKeyboard();
                    dismissDropDown();
                    if (clientClickListener != null) {
                        clientClickListener.onClick(v);
                    }
                }
                return retValue;
            }
        });


        setAdapter(arrayAdapter);
        setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (clientClickListener != null) {
                    clientClickListener.onClick(view);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

    }

    private void dismissKeyboard() {
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getWindowToken(), 0);
    }

    public String getCurrentLocation() {
        return getText().toString();
    }

    @Override
    protected void performFiltering(CharSequence text, int keyCode) {
        super.performFiltering("", 0);
    }

    private ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_dropdown_item_1line);


}
