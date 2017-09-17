package pl.rasztabiga.architecturecomponents;

import android.databinding.BindingConversion;
import android.databinding.InverseBindingAdapter;
import android.util.Log;
import android.widget.EditText;

import com.google.common.base.Throwables;

public class EditTextBindingAdapters {

    private EditTextBindingAdapters() {}

    @BindingConversion
    public static String longToStr(Long value) {
        return value != null ? String.valueOf(value) : "";
    }

    @InverseBindingAdapter(attribute = "android:text", event = "android:textAttrChanged")
    public static Long captureLongValue(EditText view) {
        long value = 0;
        try {
            value = Long.parseLong(view.getText().toString());
        } catch (NumberFormatException e) {
            Log.d("EditTextBindingAdapters", Throwables.getStackTraceAsString(e));
        }
        return value;
    }
}