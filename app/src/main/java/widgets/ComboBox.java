package widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.garyjacobs.weathertest.R;


public class ComboBox extends FrameLayout {
    private AutoCompleteTextView theText;
    //private EditText theText;
    private OnClickListener clientClickListener;
    //private Spinner theSpinner;

    private String[] testValue = {"CURRENT LOCATION", "08057", "Lisbon", "Plainview, NY", "Plainview, TX"};

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
        ViewGroup combobox = (ViewGroup) inflate(getContext(), R.layout.combobox, this);
        theText = (AutoCompleteTextView) combobox.findViewById(R.id.the_text);
        theText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean retValue = false;
                if (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER || actionId == EditorInfo.IME_ACTION_DONE) {
                    retValue = true;
                    if (clientClickListener != null) {
                        clientClickListener.onClick(v);
                    }
                }
                return retValue;
            }
        });


        //theSpinner = (Spinner) combobox.findViewById(R.id.the_spinner);
        theText.setAdapter(arrayAdapter);
        theText.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String item = (String) parent.getItemAtPosition(position);
                theText.setText(item);
                if (clientClickListener != null) {
                    clientClickListener.onClick(view);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

    }

    public String getCurrentLocation() {
        return theText.getText().toString();
    }

    private ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_expandable_list_item_1, testValue);
}
