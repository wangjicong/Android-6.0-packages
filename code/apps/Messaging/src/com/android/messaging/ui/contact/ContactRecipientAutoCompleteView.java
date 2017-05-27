/*
 * Copyright (C) 2015 The Android Open Source Project
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
package com.android.messaging.ui.contact;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v7.appcompat.R;
import android.text.Editable;
import android.text.TextPaint;
import android.text.TextWatcher;
import android.text.util.Rfc822Tokenizer;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import com.android.ex.chips.RecipientEditTextView;
import com.android.ex.chips.RecipientEntry;
import com.android.ex.chips.recipientchip.DrawableRecipientChip;
import com.android.messaging.datamodel.data.ParticipantData;
import com.android.messaging.ui.conversation.ConversationActivity;
import com.android.messaging.util.ContactRecipientEntryUtils;
import com.android.messaging.util.ContactUtil;
import com.android.messaging.util.PhoneUtils;
import com.android.messaging.ui.contact.NumberMatchUtils;
import com.sprd.messaging.util.Utils;
import com.sprd.plat.mms.plugin.Impl.NotifyStatus;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
//add for SPCSS00276417
import com.android.messaging.util.TextUtil;
//end for SPCSS00276417
/**
 * An extension for {@link RecipientEditTextView} which shows a list of Materialized contact chips.
 * It uses Bugle's ContactUtil to perform contact lookup, and is able to return the list of
 * recipients in the form of a ParticipantData list.
 */
public class ContactRecipientAutoCompleteView extends RecipientEditTextView {
    public interface ContactChipsChangeListener {
        void onContactChipsChanged(int oldCount, int newCount);
        void onInvalidContactChipsPruned(int prunedCount);
        void onEntryComplete();
    }

    private final int mTextHeight;
    private ContactChipsChangeListener mChipsChangeListener;

    private boolean mEditing;
    private String  mLastText;
    /**
     * Watches changes in contact chips to determine possible state transitions.
     */
    private class ContactChipsWatcher implements TextWatcher {
        /**
         * Tracks the old chips count before text changes. Note that we currently don't compare
         * the entire chip sets but just the cheaper-to-do before and after counts, because
         * the chips view don't allow for replacing chips.
         */
        private int mLastChipsCount = 0;

        @Override
        public void onTextChanged(final CharSequence s, final int start, final int before,
                final int count) {
            String current = s.toString();
            /* SPRD: modify for Bug 521135 begin */
            String lastS = current.length() > 0 ? current.substring(current.length()-1) : "";
            if (",".equals(lastS) || ";".equals(lastS)){
                ConversationActivity.sContactConfirmAct = true;
            }
            /* SPRD: modify for Bug 521135 end */
            if (!current.replace(" ", "").equals(mLastText)){
                mLastText = current.replace(" ", "");
                mEditing = true;
            }
        }

        @Override
        public void beforeTextChanged(final CharSequence s, final int start, final int count,
                final int after) {
            // We don't take mLastChipsCount from here but from the last afterTextChanged() run.
            // The reason is because at this point, any chip spans to be removed is already removed
            // from s in the chips text view.
        }

        @Override
        public void afterTextChanged(final Editable s) {
            final int currentChipsCount = s.getSpans(0, s.length(),
                    DrawableRecipientChip.class).length;
            if (currentChipsCount != mLastChipsCount) {
                // When a sanitizing task is running, we don't want to notify any chips count
                // change, but we do want to track the last chip count.
                if (mChipsChangeListener != null && mCurrentSanitizeTask == null) {
                    mChipsChangeListener.onContactChipsChanged(mLastChipsCount, currentChipsCount);
                }
                mLastChipsCount = currentChipsCount;
                mEditing = false;
            }
        }
    }

    private static final String TEXT_HEIGHT_SAMPLE = "a";

    public ContactRecipientAutoCompleteView(final Context context, final AttributeSet attrs) {
        super(new ContextThemeWrapper(context, R.style.ColorAccentGrayOverrideStyle), attrs);

        // Get the height of the text, given the currently set font face and size.
        final Rect textBounds = new Rect(0, 0, 0, 0);
        final TextPaint paint = getPaint();
        paint.getTextBounds(TEXT_HEIGHT_SAMPLE, 0, TEXT_HEIGHT_SAMPLE.length(), textBounds);
        mTextHeight = textBounds.height();

        setTokenizer(new Rfc822Tokenizer());
        addTextChangedListener(new ContactChipsWatcher());
        setOnFocusListShrinkRecipients(false);

        setBackground(context.getResources().getDrawable(
                R.drawable.abc_textfield_search_default_mtrl_alpha));
    }

    public void setContactChipsListener(final ContactChipsChangeListener listener) {
        mChipsChangeListener = listener;
    }

    /**
     * A tuple of chips which AsyncContactChipSanitizeTask reports as progress to have the
     * chip actually replaced/removed on the UI thread.
     */
    private class ChipReplacementTuple {
        public final DrawableRecipientChip removedChip;
        public final RecipientEntry replacedChipEntry;

        public ChipReplacementTuple(final DrawableRecipientChip removedChip,
                final RecipientEntry replacedChipEntry) {
            this.removedChip = removedChip;
            this.replacedChipEntry = replacedChipEntry;
        }
    }

    /**
     * An AsyncTask that cleans up contact chips on every chips commit (i.e. get or create a new
     * conversation with the given chips).
     */
    private class AsyncContactChipSanitizeTask extends
            AsyncTask<Void, ChipReplacementTuple, Integer> {

        @Override
        protected Integer doInBackground(final Void... params) {
            final DrawableRecipientChip[] recips = getText()
                    .getSpans(0, getText().length(), DrawableRecipientChip.class);
            int invalidChipsRemoved = 0;
            for (final DrawableRecipientChip recipient : recips) {
                final RecipientEntry entry = recipient.getEntry();
                //add for SPCSS00276417
                String number = TextUtil.replaceUnicodeDigits(entry.getDestination());
                //end for SPCSS00276417
                if (entry != null) {
                    if (entry.isValid()) {
                        if (RecipientEntry.isCreatedRecipient(entry.getContactId()) ||
                                ContactRecipientEntryUtils.isSendToDestinationContact(entry)) {
                            // This is a generated/send-to contact chip, try to look it up and
                            // display a chip for the corresponding local contact.
                            final Cursor lookupResult = ContactUtil.lookupDestination(getContext(),
                                    number).performSynchronousQuery();
                            if (lookupResult != null && lookupResult.moveToNext()) {
                                // Found a match, remove the generated entry and replace with
                                // a better local entry.
                                publishProgress(new ChipReplacementTuple(recipient,
                                        ContactUtil.createRecipientEntryForPhoneQuery(
                                                lookupResult, true)));
                            }
                            //add for SPCSS00276417
                            /*else if (NumberMatchUtils.getNumberMatchUtils().getNumberMatchNotify().OnNotify(0,0,0,entry.getDestination(),null)  == NumberMatchUtils.getNumberMatchUtils().SUCC/*PhoneUtils.isValidSmsMmsDestination(
                                    entry.getDestination())*/
                            else if (NumberMatchUtils.getNumberMatchUtils().getNumberMatchNotify().OnNotify(0,0,0,number,null)  == NumberMatchUtils.getNumberMatchUtils().SUCC
                            //end for SPCSS00276417
                            ){
                                // No match was found, but we have a valid destination so let's at
                                // least create an entry that shows an avatar.
                                publishProgress(new ChipReplacementTuple(recipient,
                                        ContactRecipientEntryUtils.constructNumberWithAvatarEntry(
                                                /*following for SPCSS00276417*//*entry.getDestination()*/number)));
                            } else {
                                // Not a valid contact. Remove and show an error.
                                publishProgress(new ChipReplacementTuple(recipient, null));
                                invalidChipsRemoved++;
                            }
                        }
                    } else {
                        publishProgress(new ChipReplacementTuple(recipient, null));
                        invalidChipsRemoved++;
                    }
                }
            }
            return invalidChipsRemoved;
        }

        @Override
        protected void onProgressUpdate(final ChipReplacementTuple... values) {
            for (final ChipReplacementTuple tuple : values) {
                if (tuple.removedChip != null) {
                    final Editable text = getText();
                    final int chipStart = text.getSpanStart(tuple.removedChip);
                    final int chipEnd = text.getSpanEnd(tuple.removedChip);
                    if (chipStart >= 0 && chipEnd >= 0) {
                        text.delete(chipStart, chipEnd);
                    }

                    if (tuple.replacedChipEntry != null) {
                        appendRecipientEntry(tuple.replacedChipEntry);
                    }
                }
            }
        }

        @Override
        protected void onPostExecute(final Integer invalidChipsRemoved) {
            mCurrentSanitizeTask = null;
            if (invalidChipsRemoved > 0) {
                mChipsChangeListener.onInvalidContactChipsPruned(invalidChipsRemoved);
            }
        }
    }

    /**
     * We don't use SafeAsyncTask but instead use a single threaded executor to ensure that
     * all sanitization tasks are serially executed so as not to interfere with each other.
     */
    private static final Executor SANITIZE_EXECUTOR = Executors.newSingleThreadExecutor();

    private AsyncContactChipSanitizeTask mCurrentSanitizeTask;

    /**
     * Whenever the caller wants to start a new conversation with the list of chips we have,
     * make sure we asynchronously:
     * 1. Remove invalid chips.
     * 2. Attempt to resolve unknown contacts to known local contacts.
     * 3. Convert still unknown chips to chips with generated avatar.
     *
     * Note that we don't need to perform this synchronously since we can
     * resolve any unknown contacts to local contacts when needed.
     */
    private void sanitizeContactChips() {
        if (mCurrentSanitizeTask != null && !mCurrentSanitizeTask.isCancelled()) {
            mCurrentSanitizeTask.cancel(false);
            mCurrentSanitizeTask = null;
        }
        mCurrentSanitizeTask = new AsyncContactChipSanitizeTask();
        mCurrentSanitizeTask.executeOnExecutor(SANITIZE_EXECUTOR);
    }

    /**
     * Returns a list of ParticipantData from the entered chips in order to create
     * new conversation.
     */
    public ArrayList<ParticipantData> getRecipientParticipantDataForConversationCreation() {
        final DrawableRecipientChip[] recips = getText()
                .getSpans(0, getText().length(), DrawableRecipientChip.class);
        final ArrayList<ParticipantData> contacts =
                new ArrayList<ParticipantData>(recips.length);
        for (final DrawableRecipientChip recipient : recips) {
            final RecipientEntry entry = recipient.getEntry();
            //modify for SPCSS00276417
            String number = TextUtil.replaceUnicodeDigits(entry.getDestination());
            //if (entry != null && entry.isValid() && entry.getDestination() != null &&
            //        NumberMatchUtils.getNumberMatchUtils().getNumberMatchNotify().OnNotify(0,0,0,entry.getDestination(),null)  == NumberMatchUtils.getNumberMatchUtils().SUCC/*PhoneUtils.isValidSmsMmsDestination(entry.getDestination())*/) {
            //    contacts.add(ParticipantData.getFromRecipientEntry(recipient.getEntry()));
            if (entry != null && entry.isValid() && number != null &&
                    NumberMatchUtils.getNumberMatchUtils().getNumberMatchNotify().OnNotify(0,0,0,number,null)  == NumberMatchUtils.getNumberMatchUtils().SUCC/*PhoneUtils.isValidSmsMmsDestination(entry.getDestination())*/) {
                contacts.add(ParticipantData.getFromRecipientEntry(recipient.getEntry()));
            //end for SPCSS00276417
            }
        }
        sanitizeContactChips();
        return contacts;
    }

    /**c
     * Gets a set of currently selected chips' emails/phone numbers. This will facilitate the
     * consumer with determining quickly whether a contact is currently selected.
     */
    public Set<String> getSelectedDestinations() {
        Set<String> set = new HashSet<String>();
        final DrawableRecipientChip[] recips = getText()
                .getSpans(0, getText().length(), DrawableRecipientChip.class);

        for (final DrawableRecipientChip recipient : recips) {
            final RecipientEntry entry = recipient.getEntry();
            //modify for SPCSS00276417
            String number = TextUtil.replaceUnicodeDigits(entry.getDestination());
            /*if (entry != null && entry.isValid() && entry.getDestination() != null) {
                String dest = PhoneUtils.getDefault().getCanonicalBySystemLocale(
                        entry.getDestination()).replace(" ","");
                set.add(dest);*/
            if (entry != null && entry.isValid() && number != null) {
                String dest = PhoneUtils.getDefault().getCanonicalBySystemLocale(
                        number).replace(" ","");
                set.add(dest);
            //end for SPCSS00276417
            }
        }
        return set;
    }

    @Override
    public boolean onEditorAction(final TextView view, final int actionId, final KeyEvent event) {
        /* SPRD: modify for Bug 521135 begin */
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            ConversationActivity.sContactConfirmAct = true;
            if (!mEditing) {
                mChipsChangeListener.onEntryComplete();
            }
        }
        /* SPRD: modify for Bug 521135 end */
        return super.onEditorAction(view, actionId, event);
    }

    /* Add by SPRD for Bug:504724 2015.12.14 Start */
    // FIXME: fixed bug in APP, better may is in FW.
    @Override
    public void removeRecipientEntry(@NonNull final RecipientEntry entry) {
        if(entry == null) {
            return;
        }
        final DrawableRecipientChip[] recips = getText()
                .getSpans(0, getText().length(), DrawableRecipientChip.class);
        for (final DrawableRecipientChip recipient : recips) {
            final RecipientEntry existingEntry = recipient.getEntry();
            //modify for SPCSS00276417
            String number = TextUtil.replaceUnicodeDigits(entry.getDestination());
            // FIXME: Handle email address if need.
            /*boolean needRemove = existingEntry != null && existingEntry.isValid()
                    && existingEntry.isSamePerson(entry) // same contact ID
                    && existingEntry.getDestination().equals(entry.getDestination()); // same phone number*/
            boolean needRemove = existingEntry != null && existingEntry.isValid()
                    && existingEntry.isSamePerson(entry) // same contact ID
                    && existingEntry.getDestination().equals(number); // same phone number
            //end for SPCSS00276417      
            if (needRemove) {
                removeChip(recipient);
            }
        }

    }

    /**
     * FIXME: a more effective resolution is to set accessiblity of com.android.ex.chips.RecipientEditTextView.removeChip to "protected".
     */
    private void removeChip(DrawableRecipientChip entry) {
        Method m = Utils.getSuperMethod("removeChip", 1,
                getClass().getSuperclass(), DrawableRecipientChip.class);
        if (m != null) {
            Utils.invoke(this, m, new Object[]{entry});
        }
    }
    /* Add by SPRD for Bug:504724 2015.12.18 End */

    public int getRecipientCount() {
        Editable s = getText();
        return s.getSpans(0, s.length(),
                DrawableRecipientChip.class).length;
    }
}
