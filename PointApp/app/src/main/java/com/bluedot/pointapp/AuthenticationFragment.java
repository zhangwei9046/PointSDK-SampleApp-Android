package com.bluedot.pointapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import au.com.bluedot.point.BDError;
import au.com.bluedot.point.ServiceStatusListener;

import au.com.bluedot.point.net.engine.ServiceManager;
import au.com.bluedot.point.net.engine.ZoneInfo;

import com.bluedotinnovation.android.pointapp.R;

import java.util.List;

public class AuthenticationFragment extends Fragment implements OnClickListener, ServiceStatusListener{

    // Context Activity and UI elements members
	private MainActivity mActivity;
	private Button mBtnAuthenticate;

    private EditText mEdtEmail;
    private EditText mEdtApiKey;
    private EditText mEdtPackageName;
	private boolean mIsAuthenticated;

    // Shared preferences - used to store Bluedot credentials
    private SharedPreferences mSharedPreferences;

    // Alternative back-end URL
    private String mAlternativeUrl = null;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_authenticate,
				container, false);
		mEdtEmail = (EditText) rootView.findViewById(R.id.edt_email);
		mEdtApiKey = (EditText) rootView.findViewById(R.id.edt_api_key);
		mEdtPackageName = (EditText) rootView.findViewById(R.id.edt_package_name);
        mBtnAuthenticate = (Button) rootView .findViewById(R.id.btn_authenticate);
        mBtnAuthenticate.setOnClickListener(this);

        // get existing credentials from shared preferences
        if (getActivity() != null ) {
            mSharedPreferences = getActivity().getSharedPreferences(AppConstants.APP_PROFILE, Activity.MODE_PRIVATE);
            mEdtEmail.setText(mSharedPreferences.getString(AppConstants.KEY_USERNAME, null));
            mEdtApiKey.setText(mSharedPreferences.getString(AppConstants.KEY_API_KEY, null));
            mEdtPackageName.setText(mSharedPreferences.getString(AppConstants.KEY_PACKAGE_NAME, null));
        }

        // get credentials from lanching Uri
        if ( (getArguments() != null && getArguments().containsKey("uri_data")) ){
            Uri customURI = Uri.parse(getArguments().getString("uri_data"));
            final String uriPackageName = customURI.getQueryParameter("BDPointPackageName");
            final String uriApiKey = customURI.getQueryParameter("BDPointAPIKey");
            final String uriEmail = customURI.getQueryParameter("BDPointUsername");
            mAlternativeUrl = customURI.getQueryParameter("BDPointAPIUrl");

            // Now decide which credentials to put onto UI
            if (uriApiKey != null && mEdtApiKey.getText().length() == 0){
                // Put launch Uri credentials
                mEdtEmail.setText(uriEmail);
                mEdtApiKey.setText(uriApiKey);
                mEdtPackageName.setText(uriPackageName);
            } else if ( ! uriApiKey.equals(mEdtApiKey.getText().toString()) ){
                // Both credentials present and Uri credentials are different
                // Ask user if he wants to replace with Uri
                new AlertDialog.Builder(getActivity())
                        .setTitle("Different Credentials")
                        .setCancelable(false)
                        .setMessage(
                                "Bluedot Service is already running with ApiKey :"+ mEdtApiKey.getText().toString()
                                        + "\nDo you want to use new ApiKey : " + uriApiKey + "?")
                        .setPositiveButton(R.string.yes,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        mEdtEmail.setText(uriEmail);
                                        mEdtApiKey.setText(uriApiKey);
                                        mEdtPackageName.setText(uriPackageName);
                                    }
                                })
                        .setNegativeButton(R.string.no, null).create().show();
            }
        }
		return rootView;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);

        ServiceManager.getInstance(getActivity()).addBlueDotPointServiceStatusListener(this);
		
		refresh();
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		if (getActivity() == null) {
			mActivity = (MainActivity) activity;
		} else {
			mActivity = (MainActivity) getActivity();
		}
	}

    @Override
    public void onResume()
    {
        super.onResume();
        refresh();
    }

	public void refresh() {

        //Checking the Bluedot Point Service status using isBlueDotPointServiceRunning in the ServiceManager
        mIsAuthenticated = ServiceManager.getInstance(getActivity()).isBlueDotPointServiceRunning();
        if(mIsAuthenticated){
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mBtnAuthenticate.setText(getString(R.string.clear_logout));
                }
            });
        }else{
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mBtnAuthenticate.setText(getString(R.string.save_authenticate));
                }
            });
        }
	}

	@Override
	public void onClick(View v) {
		if (mIsAuthenticated) {
			mActivity.stopService();
            mIsAuthenticated = false;
		} else {
			if (TextUtils.isEmpty(mEdtEmail.getText())
					|| TextUtils.isEmpty(mEdtApiKey.getText())
					|| TextUtils.isEmpty(mEdtPackageName.getText())) {
				new AlertDialog.Builder(getActivity()).setTitle("Error")
						.setMessage("Please enter login details.")
						.setPositiveButton("OK", null).create().show();
			} else {
                if (mAlternativeUrl == null){
                    mActivity.startAuthentication(mEdtEmail.getText().toString(),
                            mEdtApiKey.getText().toString(), mEdtPackageName
                                    .getText().toString());
                }else {
                    mActivity.startAuthenticationWithAlternateUrl(mEdtEmail.getText().toString(),
                            mEdtApiKey.getText().toString(), mEdtPackageName
                                    .getText().toString(), mAlternativeUrl);
                }
                mIsAuthenticated = true;
            }
		}
	}

	//Update the button status when the Bluedot Point Service status callback is invoked
    @Override
    public void onBlueDotPointServiceStartedSuccess() {
        mIsAuthenticated = true;

        //Here you can store the credentials in your app shared preference since they are correct
        if (mSharedPreferences != null) {
            mSharedPreferences.edit()
                    .putString(AppConstants.KEY_API_KEY, mEdtApiKey.getText().toString())
                    .putString(AppConstants.KEY_USERNAME, mEdtEmail.getText().toString())
                    .putString(AppConstants.KEY_PACKAGE_NAME, mEdtPackageName.getText().toString())
                    .commit();
        }

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mBtnAuthenticate.setText(getString(R.string.clear_logout));
            }
        });

    }

    @Override
    public void onBlueDotPointServiceStop() {
        mIsAuthenticated = false;
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mBtnAuthenticate.setText(getString(R.string.save_authenticate));
            }
        });
    }


    @Override
    public void onBlueDotPointServiceError(BDError bdError) {
        if(bdError.isFatal()){
            mIsAuthenticated = false;
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mBtnAuthenticate.setText(getString(R.string.save_authenticate));
                }
            });
        }
    }

    @Override
    public void onRuleUpdate(List<ZoneInfo> zoneInfos) {

    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        ServiceManager.getInstance(getActivity()).removeBlueDotPointServiceStatusListener(this);
    }
}