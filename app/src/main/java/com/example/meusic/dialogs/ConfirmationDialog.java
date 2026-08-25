package com.example.meusic.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;

import com.example.meusic.R;

public class ConfirmationDialog {

    public interface Callback {
        void onConfirm();
        void onCancel();
    }

    private Dialog dialog;
    private TextView confirmationMessage;
    private Button btnConfirm, btnCancel;

    private ConfirmationDialog(@NonNull Context context) {
        dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_confirmation, null);
        dialog.setContentView(view);
        dialog.setCancelable(true);
        dialog.getWindow().setBackgroundDrawableResource(R.drawable.rounded_dialog_bg);

        confirmationMessage = view.findViewById(R.id.confirmation_message);
        btnConfirm = view.findViewById(R.id.confirmation_confirm_btn);
        btnCancel = view.findViewById(R.id.confirmation_cancel_btn);
    }

    /**
     * Builder-style factory to create and configure the dialog.
     */
    public static class Builder {
        private final Context context;
        private String message;
        private String confirmText;
        private String cancelText;
        private boolean cancelable = true;
        private Callback callback;

        public Builder(@NonNull Context context) {
            this.context = context;
        }

        public Builder setMessage(String message) {
            this.message = message;
            return this;
        }

        public Builder setConfirmText(String confirmText) {
            this.confirmText = confirmText;
            return this;
        }

        public Builder setCancelText(String cancelText) {
            this.cancelText = cancelText;
            return this;
        }

        public Builder setCancelable(boolean cancelable) {
            this.cancelable = cancelable;
            return this;
        }

        public Builder setCallback(Callback callback) {
            this.callback = callback;
            return this;
        }

        public ConfirmationDialog build() {
            final ConfirmationDialog cd = new ConfirmationDialog(context);

            if (message != null) cd.confirmationMessage.setText(message);
            if (confirmText != null) cd.btnConfirm.setText(confirmText);
            if (cancelText != null) cd.btnCancel.setText(cancelText);

            cd.dialog.setCancelable(cancelable);

            cd.btnConfirm.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (callback != null) callback.onConfirm();
                    cd.dismiss();
                }
            });

            cd.btnCancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (callback != null) callback.onCancel();
                    cd.dismiss();
                }
            });

            return cd;
        }
    }

    public void show() {
        if (dialog != null && !dialog.isShowing()) dialog.show();
    }

    public void dismiss() {
        if (dialog != null && dialog.isShowing()) dialog.dismiss();
    }
}
