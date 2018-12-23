package ua.roma.multicolor.ui;

import android.app.AlertDialog;
import android.app.Dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.View;

import ua.roma.multicolor.R;
import ua.roma.multicolor.view.ColorPickerView;

public class PickerDialog extends DialogFragment {

    private PickerDialogListener pickerDialogListener;

    ColorPickerView.OnColorChangedListener onColorChangedListener = new ColorPickerView.OnColorChangedListener() {
        @Override
        public void colorChanged(int color) {
            pickerDialogListener.onColorChanged(color);
        }
    };

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try{
            pickerDialogListener = (PickerDialogListener) getActivity();
        }catch (ClassCastException e){
            throw new ClassCastException(getActivity().toString() + "must implement PickerDialogListener");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = getActivity().getLayoutInflater().inflate(R.layout.color_picker_layout,null);
        final ColorPickerView colorPickerView = view.findViewById(R.id.colorPicker);
        colorPickerView.setInitialColor(pickerDialogListener.getCurrentColor());
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setView(view)
                .setTitle("Pick a color")
                .setPositiveButton(R.string.positiveButton, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        pickerDialogListener.onColorChanged(colorPickerView.getColor());
                        dismiss();
                    }
                })
                .setNegativeButton(R.string.negativeButton, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dismiss();
                    }
                });
        return builder.create();

    }

    public interface PickerDialogListener{
        void onColorChanged(int color);
        int getCurrentColor();
    }
}
