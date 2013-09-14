package com.rokoroku.lolmessenger.fragments;

import android.app.Dialog;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import com.rokoroku.lolmessenger.R;

/**
 * Created by Youngrok Kim on 13. 8. 29.
 */
public class ConfirmationDialog extends DialogFragment {

    TextView viewTitle = null;
    TextView viewContent = null;

    Button buttonOK = null;
    Button buttonCancel = null;

    String title = null;
    String content = null;

    String buttonOKString = null;
    String buttonCancelString = null;

    View.OnClickListener onClickListenerOK  = null;
    View.OnClickListener onClickListenerCancel = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            dismiss();
        }
    };

    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {

        final Dialog dialog = new Dialog(getActivity());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(true);

        return dialog;
    }

    public ConfirmationDialog() { }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_confirm, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewTitle  = (TextView) view.findViewById(R.id.title);
        viewContent= (TextView) view.findViewById(R.id.content);
        buttonOK   = (Button) view.findViewById(R.id.buttonOK);
        buttonCancel=(Button) view.findViewById(R.id.buttonCancle);

        if(title  != null)                  viewTitle.setText(title);
        if(content != null)                 viewContent.setText(content);
        if(onClickListenerOK    != null)    buttonOK.setOnClickListener(onClickListenerOK);
        if(onClickListenerCancel!= null)    buttonCancel.setOnClickListener(onClickListenerCancel);
        if(buttonOKString != null)          buttonOK.setText(buttonOKString);
        if(buttonCancelString != null)      buttonCancel.setText(buttonCancelString);

    }

    public void setDialogTitle(String title) {
        this.title = title;
    }

    public void setDialogContent(String content) {
        this.content = content;
    }

    public void setPositiveOnClickListener (final View.OnClickListener onClickListener) {
        this.onClickListenerOK = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onClickListener.onClick(view);
                dismiss();
            }
        };
    }

    public void setNegativeOnClickListener (final View.OnClickListener onClickListener) {
        this.onClickListenerCancel = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onClickListener.onClick(view);
                dismiss();
            }
        };
    }

    public void setPositiveButtonText (String string) {
        buttonOKString = string;
    }

    public void setNegativeButtonText (String string) {
        buttonCancelString = string;
    }

}
