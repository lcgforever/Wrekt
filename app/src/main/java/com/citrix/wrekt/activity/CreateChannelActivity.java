package com.citrix.wrekt.activity;

import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.ActionMenuView;
import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.citrix.wrekt.R;
import com.citrix.wrekt.WrektApplication;
import com.citrix.wrekt.data.pref.StringPreference;
import com.citrix.wrekt.di.annotation.UidPref;
import com.citrix.wrekt.di.annotation.UserEmailPref;
import com.citrix.wrekt.di.annotation.UsernamePref;
import com.citrix.wrekt.di.component.ActivityComponent;
import com.citrix.wrekt.di.module.ActivityModule;
import com.citrix.wrekt.firebase.api.IFirebaseFactory;
import com.citrix.wrekt.firebase.api.IFirebaseUrlFormatter;
import com.citrix.wrekt.fragment.dialog.CreateChannelDialogFragment;
import com.citrix.wrekt.fragment.dialog.ProgressDialogFragment;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

public class CreateChannelActivity extends BaseActivity implements ActionMenuView.OnMenuItemClickListener,
        CreateChannelDialogFragment.CreateChannelActionListener {

    private static final String TAG_CREATE_CHANNEL_DIALOG = "TAG_CREATE_CHANNEL_DIALOG";
    private static final String TAG_CREATE_CHANNEL_PROGRESS_DIALOG = "TAG_CREATE_CHANNEL_PROGRESS_DIALOG";

    @Inject
    @UidPref
    StringPreference uidPref;

    @Inject
    @UserEmailPref
    StringPreference userEmailPref;

    @Inject
    @UsernamePref
    StringPreference usernamePref;

    @Inject
    IFirebaseFactory firebaseFactory;

    @Inject
    IFirebaseUrlFormatter firebaseUrlFormatter;

    private ActionMenuView actionMenuView;
    private ViewSwitcher channelCategoryViewSwitcher;
    private AppCompatSpinner channelCategorySpinner;
    private TextInputEditText channelCategoryEditText;
    private TextInputEditText channelNameEditText;
    private TextInputEditText channelDescriptionEditText;

    private ActivityComponent activityComponent;
    private FragmentManager fragmentManager;
    private String category;
    private String name;
    private String description;

    public static void start(Context context) {
        Intent intent = new Intent(context, CreateChannelActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_channel);

        inject();

        fragmentManager = getFragmentManager();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar != null) {
            supportActionBar.setHomeButtonEnabled(true);
            supportActionBar.setDisplayHomeAsUpEnabled(true);
        }

        actionMenuView = (ActionMenuView) findViewById(R.id.action_menu_view);
        channelCategoryViewSwitcher = (ViewSwitcher) findViewById(R.id.channel_category_view_switcher);
        channelCategorySpinner = (AppCompatSpinner) findViewById(R.id.channel_category_spinner);
        channelCategoryEditText = (TextInputEditText) findViewById(R.id.channel_category_edit_text);
        channelNameEditText = (TextInputEditText) findViewById(R.id.channel_name_edit_text);
        channelDescriptionEditText = (TextInputEditText) findViewById(R.id.channel_description_edit_text);

        actionMenuView.setOnMenuItemClickListener(this);

        category = channelCategorySpinner.getItemAtPosition(0).toString();
        channelCategorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedItem = parent.getItemAtPosition(position).toString();
                if (getString(R.string.customize_category_text).equals(selectedItem)) {
                    category = "";
                    channelCategoryViewSwitcher.showNext();
                    channelCategoryEditText.setText("");
                    channelCategoryEditText.requestFocus();
                } else {
                    category = selectedItem;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        channelCategoryEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                channelCategoryEditText.setError(null);
                category = s.toString();
            }
        });
        channelNameEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_NEXT || actionId == EditorInfo.IME_ACTION_UNSPECIFIED) {
                    channelDescriptionEditText.requestFocus();
                    return true;
                }
                return false;
            }
        });
        channelNameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                channelNameEditText.setError(null);
                name = s.toString();
            }
        });
        channelDescriptionEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_UNSPECIFIED) {
                    showCreateChannelDialog();
                    return true;
                }
                return false;
            }
        });
        channelDescriptionEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                channelDescriptionEditText.setError(null);
                description = s.toString();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        activityComponent = null;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        hideCreateChannelDialog();
        hideCreateChannelProgressDialog();
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void inject() {
        WrektApplication application = (WrektApplication) getApplication();
        activityComponent = application.getAppComponent().plus(new ActivityModule(this));
        activityComponent.inject(this);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Menu actionMenu = actionMenuView.getMenu();
        actionMenu.clear();
        getMenuInflater().inflate(R.menu.menu_create_channel, actionMenu);
        return true;
    }


    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_done:
                showCreateChannelDialog();
                return true;
        }
        return false;
    }

    @Override
    public void onCreateChannelConfirmed() {
        saveChannelInFirebase();
    }

    private void saveChannelInFirebase() {
        if (validateInputInformation()) {
            showCreateChannelProgressDialog();
            Firebase channelsRef = firebaseFactory.createFirebase(firebaseUrlFormatter.getChannelsUrl());
            Firebase newChannelRef = channelsRef.push();
            String channelId = newChannelRef.getKey();
            addSubscriptionToChannel(channelId, name);
        }
    }

    private boolean validateInputInformation() {
        if (TextUtils.isEmpty(category)) {
            channelCategoryEditText.setError(getString(R.string.channel_category_invalid_message));
            return false;
        } else if (TextUtils.isEmpty(name)) {
            channelNameEditText.setError(getString(R.string.channel_name_invalid_message));
            return false;
        } else if (TextUtils.isEmpty(description)) {
            channelDescriptionEditText.setError(getString(R.string.channel_description_invalid_message));
            return false;
        }
        return true;
    }

    private void addSubscriptionToChannel(final String channelId, final String channelName) {
        Firebase subscriptionRef = firebaseFactory.createFirebase(firebaseUrlFormatter.getSubscriptionsUrl()).child(uidPref.get());
        Map<String, Object> channelMap = new HashMap<>();
        channelMap.put(channelId, channelName);
        subscriptionRef.updateChildren(channelMap, new Firebase.CompletionListener() {
            @Override
            public void onComplete(FirebaseError firebaseError, Firebase firebase) {
                if (firebaseError == null) {
                    saveChannelAdminData(channelId);
                } else {
                    showCreateChannelFailedMessage(1 + firebaseError.getMessage());
                }
            }
        });
    }

    private void saveChannelAdminData(final String channelId) {
        Firebase channelAdminRef = firebaseFactory.createFirebase(firebaseUrlFormatter.getChannelAdminsUrl()).child(channelId);
        channelAdminRef.setValue(uidPref.get(), new Firebase.CompletionListener() {
            @Override
            public void onComplete(FirebaseError firebaseError, Firebase firebase) {
                if (firebaseError == null) {
                    saveChannelMemberData(channelId);
                } else {
                    removeSubscriptionToChannel(channelId);
                    showCreateChannelFailedMessage(2 + firebaseError.getMessage());
                }
            }
        });
    }

    private void saveChannelMemberData(final String channelId) {
        Firebase channelMemberRef = firebaseFactory.createFirebase(firebaseUrlFormatter.getChannelMembersUrl()).child(channelId);
        Map<String, Object> memberMap = new HashMap<>();
        Map<String, Object> newMemberMap = new HashMap<>();
        newMemberMap.put("uid", uidPref.get());
        newMemberMap.put("email", userEmailPref.get());
        newMemberMap.put("username", usernamePref.get());
        memberMap.put(uidPref.get(), newMemberMap);
        channelMemberRef.updateChildren(memberMap, new Firebase.CompletionListener() {
            @Override
            public void onComplete(FirebaseError firebaseError, Firebase firebase) {
                if (firebaseError == null) {
                    saveChannelData(channelId);
                } else {
                    removeChannelAdminData(channelId);
                    removeSubscriptionToChannel(channelId);
                    showCreateChannelFailedMessage(3 + firebaseError.getMessage());
                }
            }
        });
    }

    private void saveChannelData(final String channelId) {
        Firebase channelRef = firebaseFactory.createFirebase(firebaseUrlFormatter.getChannelsUrl()).child(channelId);
        Map<String, Object> channelMap = new HashMap<>();
        channelMap.put("id", channelId);
        channelMap.put("category", category);
        channelMap.put("name", name);
        channelMap.put("description", description);
        channelMap.put("createTime", System.currentTimeMillis());
        channelMap.put("adminUid", uidPref.get());
        channelMap.put("adminName", usernamePref.get());
        channelMap.put("memberCount", 1);
        channelMap.put("imageUrl", "invalidUrl");
        channelRef.updateChildren(channelMap, new Firebase.CompletionListener() {
            @Override
            public void onComplete(FirebaseError firebaseError, Firebase firebase) {
                if (firebaseError == null) {
                    hideCreateChannelProgressDialog();
                    finish();
                } else {
                    removeChannelMemberData(channelId);
                    removeChannelAdminData(channelId);
                    removeSubscriptionToChannel(channelId);
                    showCreateChannelFailedMessage(4 + firebaseError.getMessage());
                }
            }
        });
    }

    private void removeChannelMemberData(final String channelId) {
        Firebase channelMemberRef = firebaseFactory.createFirebase(firebaseUrlFormatter.getChannelMembersUrl()).child(channelId);
        channelMemberRef.child(uidPref.get()).removeValue();
    }

    private void removeChannelAdminData(final String channelId) {
        Firebase channelAdminRef = firebaseFactory.createFirebase(firebaseUrlFormatter.getChannelAdminsUrl()).child(channelId);
        channelAdminRef.removeValue();
    }

    private void removeSubscriptionToChannel(final String channelId) {
        Firebase subscriptionRef = firebaseFactory.createFirebase(firebaseUrlFormatter.getSubscriptionsUrl()).child(uidPref.get());
        subscriptionRef.child(channelId).removeValue();
    }

    private void showCreateChannelFailedMessage(String failureMessage) {
        Log.e("findme: ", "Firebase create channel failed: " + failureMessage);
        hideCreateChannelProgressDialog();
        Toast.makeText(this,
                R.string.create_channel_failed_message,
                Toast.LENGTH_SHORT)
                .show();
    }

    private void showCreateChannelDialog() {
        CreateChannelDialogFragment fragment
                = (CreateChannelDialogFragment) fragmentManager.findFragmentByTag(TAG_CREATE_CHANNEL_DIALOG);
        if (fragment == null) {
            fragment = CreateChannelDialogFragment.newInstance();
        }
        fragment.show(fragmentManager, TAG_CREATE_CHANNEL_DIALOG);
    }

    private void hideCreateChannelDialog() {
        CreateChannelDialogFragment fragment
                = (CreateChannelDialogFragment) fragmentManager.findFragmentByTag(TAG_CREATE_CHANNEL_DIALOG);
        if (fragment != null) {
            fragment.dismiss();
        }
    }

    private void showCreateChannelProgressDialog() {
        ProgressDialogFragment fragment
                = (ProgressDialogFragment) fragmentManager.findFragmentByTag(TAG_CREATE_CHANNEL_PROGRESS_DIALOG);
        if (fragment == null) {
            fragment = ProgressDialogFragment.newInstance(R.string.create_channel_progress_dialog_message);
        }
        fragment.show(fragmentManager, TAG_CREATE_CHANNEL_PROGRESS_DIALOG);
    }

    private void hideCreateChannelProgressDialog() {
        ProgressDialogFragment fragment
                = (ProgressDialogFragment) fragmentManager.findFragmentByTag(TAG_CREATE_CHANNEL_PROGRESS_DIALOG);
        if (fragment != null) {
            fragment.dismiss();
        }
    }
}
