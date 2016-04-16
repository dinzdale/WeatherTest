package widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.garyjacobs.weathertest.R;


public class ComboBox extends FrameLayout {
    private EditText theText;
    private OnClickListener clientClickListener;
    private Spinner theSpinner;

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
        theText = (EditText) combobox.findViewById(R.id.the_text);
        theText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean retValue = false;
                if (event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                    retValue = true;
                    if (clientClickListener != null) {
                        clientClickListener.onClick(v);
                    }
                }
                return retValue;
            }
        });
//        theText.setOnClickListener(
//                new OnClickListener() {
//                    @Override
//                    public void onClick(View v) {
//                        if (clientClickListener != null) {
//                            clientClickListener.onClick(v);
//                        }
//                    }
//                }
//        );
        theSpinner = (Spinner) combobox.findViewById(R.id.the_spinner);
    }
}
