package com.android.messaging.smil.ui;

import android.view.MenuItem;
import android.view.Menu;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.text.TextUtils;
import android.text.InputType;
import com.android.messaging.ui.CustomHeaderViewPager;
import com.android.messaging.ui.contact.ContactPickerFragment;
import com.android.messaging.util.Assert;
import com.android.messaging.util.ImeUtil;
import com.android.messaging.util.UiUtils;
import android.view.inputmethod.EditorInfo;
import com.android.messaging.R;
import android.app.Activity;
import com.android.messaging.util.ContactRecipientEntryUtils;
import com.android.ex.chips.RecipientEntry;

public class SmilContactPickFragment extends ContactPickerFragment{
	public static final String FRAGMENT_TAG = "SmilContactPickFragment";
	public SmilContactPickHost myInterface = null;
	public interface SmilContactPickHost{
		public void updateSmilUi();
	}
	
	@Override
    public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}
	
	@Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
            final Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, container, savedInstanceState);
		//hide the contactPager
		super.showHideContactPagerWithAnimation(false);
		return view;
	}
	
	@Override
    public void onActivityCreated(final Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		super.setContactPickingMode(MODE_CHIPS_ONLY, true);
        Bundle bundle = getArguments();
        getAutoCompleteView().setText(bundle.getString("convName"));
        getAutoCompleteView().setFilterTouchesWhenObscured(false);
        getAutoCompleteView().setFocusable(false);
        getAutoCompleteView().clearFocus();
        // getAutoCompleteView().setSelection(bundle.getString("convName").length());
//        RecipientEntry entry = ContactRecipientEntryUtils
//                .constructSendToDestinationEntry(bundle.getString("convName"));
//        getAutoCompleteView().appendRecipientEntry(entry);
//        super.onEntryComplete();
	}

	@Override
	public void onAttach(Activity activity) {
		myInterface=(SmilContactPickHost) activity;
		super.onAttach(activity);
	}

	@Override
    public boolean onMenuItemClick(final MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.action_ime_dialpad_toggle:
                final int baseInputType = InputType.TYPE_TEXT_FLAG_MULTI_LINE;
                if ((super.getAutoCompleteView().getInputType() & InputType.TYPE_CLASS_PHONE) !=
                        InputType.TYPE_CLASS_PHONE) {
                	super.getAutoCompleteView().setInputType(baseInputType | InputType.TYPE_CLASS_PHONE);
                    menuItem.setIcon(R.drawable.ic_ime_light);
                } else {
                	super.getAutoCompleteView().setInputType(baseInputType | InputType.TYPE_CLASS_TEXT);
                    menuItem.setIcon(R.drawable.ic_numeric_dialpad);
                }
                ImeUtil.get().showImeKeyboard(getActivity(), super.getAutoCompleteView());
                return true;

            case R.id.action_add_more_participants:
                // comment now we don't need to changed the participants
//                if(super.getCantactPickerHost() != null) {
//                	super.getCantactPickerHost().onInitiateAddMoreParticipants();
//                }
//                setConfirmItemshow(true);
//                return true;
                // coment by spread

            case R.id.action_confirm_participants:
                //maybeGetOrCreateConversation();
                /*if(TextUtils.isEmpty(super.getAutoCompleteView().getText())){
                    UiUtils.showToast(R.string.no_few_participants);
                    return true;
                }
                super.getAutoCompleteView().onEditorAction(super.getAutoCompleteView(),EditorInfo.IME_ACTION_DONE,null);*/
            	myInterface.updateSmilUi();
            	setConfirmItemshow(false);
                return true;

            case R.id.action_delete_text:
                Assert.equals(MODE_PICK_INITIAL_CONTACT, super.getContactPickMode());
                super.getAutoCompleteView().setText("");
                return true;
        }
        return false;
    }
	
	public void setConfirmItemshow(boolean show){
		Menu menu = getToolbar().getMenu();
		MenuItem confirmParticipantsItem = menu.findItem(R.id.action_confirm_participants);
		if (show) {
			confirmParticipantsItem.setVisible(true);
		}else {
			confirmParticipantsItem.setVisible(false);
		}
	}
	
	public void setShowHideContact(boolean show){
		if (show) {
			super.showHideContactPagerWithAnimation(true);
		}else {
			super.showHideContactPagerWithAnimation(false);
		}
	}
}
