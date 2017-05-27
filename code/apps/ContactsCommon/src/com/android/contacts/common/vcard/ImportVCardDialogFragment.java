/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.contacts.common.vcard;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

import com.android.contacts.common.R;

/** Asks for confirmation before importing contacts from a vcard. */
public class ImportVCardDialogFragment extends DialogFragment {

    /** Callbacks for hosts of the {@link ImportVCardDialogFragment}. */
    public interface Listener {

        /** Invoked after the user has confirmed that contacts should be imported. */
        void onImportVCardConfirmed();

        /** Invoked after the user has rejected importing contacts. */
        void onImportVCardDenied();
    }

    /** Displays the dialog asking for confirmation before importing contacts. */
    public static void show(Activity activity) {
        if (!(activity instanceof Listener)) {
            throw new IllegalArgumentException(
                    "Activity must implement " + Listener.class.getName());
        }

        final ImportVCardDialogFragment dialog = new ImportVCardDialogFragment();
        dialog.show(activity.getFragmentManager(), "importVCardDialogFragment");
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setMessage(R.string.import_from_vcf_file_confirmation_message)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) {
                        final Listener listener = (Listener) getActivity();
                        if (listener != null) {
                            listener.onImportVCardConfirmed();
                        }
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) {
                        final Listener listener = (Listener) getActivity();
                        if (listener != null) {
                            listener.onImportVCardDenied();
                        }
                    }
                })
                .create();
    }
}
