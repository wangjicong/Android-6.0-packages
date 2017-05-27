/**
 *   Copyright (C) 2010,2013 Thundersoft Corporation
 *   All rights Reserved
 */
package com.ucamera.ucomm.sns.services;

import java.util.Map;
import org.scribe.exceptions.OAuthException;
import org.scribe.model.OAuthConstants;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.CookieSyncManager;

import com.ucamera.ucomm.sns.R;
import com.ucamera.ucomm.sns.AuthDialog;
import com.ucamera.ucomm.sns.RequestException;
import com.ucamera.ucomm.sns.RequestListener;
import com.ucamera.ucomm.sns.Util;

public abstract class AbstractService implements ShareService {
    protected final String TAG = getServiceName();

    private static final String TOKEN_STORAGE = "share_service";

    private OAuthService mOAuthService;
    private Token mAccessToken;
    private ShareError mShareError;

    protected abstract OAuthService createOAuthService();

    /**
     * Some sites support follow, but some NOT.
     * please override this method in subclass for those support.
     */
    protected void followMe(){}

    protected final OAuthService getOAuthService() {
        if (mOAuthService == null) {
            synchronized (this) {
                mOAuthService = createOAuthService();
            }
        }
        return mOAuthService;
    }

    public Token getAccessToken() {
        return this.mAccessToken;
    }

    @Override public ShareError getShareError() {
        return mShareError;
    }

    @Override
    public boolean isAuthorized() {
        return getAccessToken() != null
            && getAccessToken() != OAuthConstants.EMPTY_TOKEN
            && getAccessToken().isValid();
    }

    @Override
    public boolean isAuthorized(Activity activity) {
        return false;
    }

    private boolean canRefresh() {
        return getAccessToken() != null
            && !TextUtils.isEmpty(getAccessToken().getRefreshToken());
    }
    //
    // Session manage
    //

    private final String KEY_TOKEN  = getServiceName() + "_token";
    private final String KEY_SECRET = getServiceName() + "_secret";
    private final String KEY_EXPIRES= getServiceName() + "_expires";
    private final String KEY_REFRESH= getServiceName() + "_refresh";

    public void saveSession(Context context) {
        final Token token = getAccessToken();
        if (token == null) {
            Log.w(TAG, "Access token is null, abort saving!");
            return;
        }
        SharedPreferences prefs = context.getSharedPreferences(TOKEN_STORAGE, Context.MODE_PRIVATE);
        Editor editor = prefs.edit();
        editor.putString(KEY_TOKEN, token.getToken());
        if (!TextUtils.isEmpty(token.getSecret())){
            editor.putString(KEY_SECRET, token.getSecret());
        }
        if (token.getExpires() != 0){
            editor.putLong(KEY_EXPIRES, token.getExpires());
        }
        if (!TextUtils.isEmpty(token.getRefreshToken())){
            editor.putString(KEY_REFRESH, token.getRefreshToken());
        }
        editor.commit();
    }

    public void loadSession(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(TOKEN_STORAGE, Context.MODE_PRIVATE);
        String token = prefs.getString(KEY_TOKEN, null);
        if (TextUtils.isEmpty(token)) {
            mAccessToken = OAuthConstants.EMPTY_TOKEN;
            return;
        }

        String secret = prefs.getString(KEY_SECRET, null);
        long expires  = prefs.getLong(KEY_EXPIRES, 0);
        String refresh = prefs.getString(KEY_REFRESH, null);
        mAccessToken = new Token(token, secret,expires).setRefreshToken(refresh);
    }

    protected void clearSession(Context context) {
        mAccessToken = OAuthConstants.EMPTY_TOKEN;
        SharedPreferences prefs = context.getSharedPreferences(TOKEN_STORAGE, Context.MODE_PRIVATE);
        Editor editor = prefs.edit();
        editor.remove(KEY_TOKEN);
        editor.remove(KEY_SECRET);
        editor.remove(KEY_EXPIRES);
        editor.remove(KEY_REFRESH);
        editor.commit();
    }

    protected boolean doLogout(){
        String url = getOAuthService().getLogoutUrl(getAccessToken());
        if (!TextUtils.isEmpty(url)){
            OAuthRequest request = new OAuthRequest(Verb.POST, url);
            getOAuthService().signRequest(getAccessToken(), request);
            isRequestSuccess("logout", request.send(), TRUE_CHECKER);
        }
        return true;
    }

    @Override
    public void logout(final Context context, final LogoutListener listener){
        clearSession(context);
        Util.clearCookies(context);
        listener.onSuccess();
        new AsyncTask<Void, Void, Boolean>(){
            protected Boolean doInBackground(Void... params) {
                try {
                    return doLogout();
                }catch (Exception e) {
                    Log.w(TAG, "Fail logout", e);
                    return false;
                }
            }
        }.execute();
    }

    @Override
    public void login(Context context, final LoginListener listener) {
        if (!Util.checkNetworkShowAlert(context)){
            listener.onCancel();
            return;
        }

        OAuthService oauthService = getOAuthService();
        if (oauthService == null) {
            throw new OAuthException("getOAuthService return null!");
        }

        if (isOAuth2()) {
            if (canRefresh()) {
                new AccessTokenRefresher(this, context, listener).execute();
            } else {
                showAuthDialog(context, OAuthConstants.EMPTY_TOKEN, listener);
            }
        } else {
//            new RequestTokenFetcher(this, context, listener).execute();
            new LoginThread(this, context, listener).logIn();
        }
    }

    protected void showAuthDialog(Context context, final Token requestToken,
            final LoginListener listener) {
        CookieSyncManager.createInstance(context);
        String authUrl = getOAuthService().getAuthorizationUrl(requestToken);
        authUrl = wrapAuthorizationUrl(authUrl);
        new AuthDialog(context, authUrl, new RequestListener() {
            public void onComplete(Bundle data) {
                CookieSyncManager.getInstance().sync();
                new AccessTokenFetcher(AbstractService.this, requestToken, listener).execute(data);
            }

            public void onException(RequestException e) {
                listener.onFail(e.getMessage());
            }

            public void onCancel() {
                listener.onCancel();
            }
        }).show();
    }

    protected final boolean isOAuth2() {
        return "2.0".equals(getOAuthService().getVersion());
    }

    /*
     * for sina 1.0, need pass callback_url parameter
     */
    protected String wrapAuthorizationUrl(String url) {
        return url;
    }

    protected Token prepareRequestToken() {
        return isOAuth2() ?
                OAuthConstants.EMPTY_TOKEN : getOAuthService().getRequestToken();
    }

    private String getVerifyCode(Bundle data) {
        return isOAuth2() ?
                data.getString(OAuthConstants.CODE) : data.getString(OAuthConstants.VERIFIER);
    }

    static class RequestTokenFetcher extends AsyncTask<Void, Void, Token> {
        private AbstractService mService;
        private Context mContext;
        private LoginListener mListener;
        private ProgressDialog mSpinner;

        public RequestTokenFetcher(AbstractService service, Context context, LoginListener listener) {
            mService = service;
            mContext = context;
            mListener = listener;
            mSpinner = new ProgressDialog(mContext);
            mSpinner.setMessage(context.getString(R.string.sns_msg_connecting));
            mSpinner.setOnCancelListener(new DialogInterface.OnCancelListener() {
                public void onCancel(DialogInterface dialog) {
                    cancel(true);
                }
            });
        }

        protected void onPreExecute() {
            mSpinner.show();
        }

        protected Token doInBackground(Void... params) {
            try {
                return mService.prepareRequestToken();
            } catch (Exception e) {
                return null;
            }
        }

        protected void onCancelled() {
            mListener.onCancel();
        }

        protected void onPostExecute(final Token result) {
            mSpinner.dismiss();
            if (result == null) {
                mListener.onFail("Fail to fetch request token.");
            } else {
                mService.showAuthDialog(mContext, result, mListener);
            }
        }
    }

    static class AccessTokenFetcher extends AsyncTask<Bundle, Void, Token> {
        private AbstractService mService;
        private LoginListener mListener;
        private Token mRequestToken;

        public AccessTokenFetcher(AbstractService service, Token requestToken, LoginListener listener) {
            mService = service;
            mRequestToken = requestToken;
            mListener = listener;
        }

        protected Token doInBackground(Bundle... params) {
            try {
                Bundle data = params[0];
                String verify = mService.getVerifyCode(data);
                if (!TextUtils.isEmpty(verify)) {
                   return mService.getOAuthService().getAccessToken(mRequestToken,new Verifier(verify));
                } else {
                    if (data.containsKey(OAuthConstants.ACCESS_TOKEN)){
                        String expiresIn = data.getString(OAuthConstants.EXPIRES);
                        return new Token(data.getString(OAuthConstants.ACCESS_TOKEN),
                                         data.getString(OAuthConstants.TOKEN_SECRET))
                                   .setExpiresIn(expiresIn);
                    }
                }
            }catch (Exception e) {
                Log.w(mService.TAG, "Fail fetch access token.",e);
            }
            return null;
        }

        protected void onPostExecute(Token accessToken) {
            if (accessToken == null ){
                mListener.onFail("Fail fetch access code!");
            } else {
                mService.mAccessToken = accessToken;
                mListener.onSuccess(accessToken);
                try {
                    mService.followMe();
                }catch (Throwable e) {
                    // IGNORE
                }
            }
        }
    }

    static class AccessTokenRefresher extends AsyncTask<Void, Void, Token> {
        private AbstractService mService;
        private Context mContext;
        private LoginListener mListener;
        private ProgressDialog mSpinner;

        public AccessTokenRefresher(AbstractService service, Context context, LoginListener listener) {
            mService = service;
            mContext = context;
            mListener = listener;
            mSpinner = new ProgressDialog(mContext);
            mSpinner.setMessage(context.getString(R.string.sns_msg_connecting));
            mSpinner.setOnCancelListener(new DialogInterface.OnCancelListener() {
                public void onCancel(DialogInterface dialog) {
                    cancel(true);
                }
            });
        }

        protected void onPreExecute() {
            mSpinner.show();
        }

        protected Token doInBackground(Void... params) {
            try {
                return mService.getOAuthService().refreshAccessToken(mService.getAccessToken());
            } catch (Exception e) {
                return null;
            }
        }

        protected void onCancelled() {
            mListener.onCancel();
        }

        protected void onPostExecute(final Token result) {
            mSpinner.dismiss();
            if (result == null) {
                mService.showAuthDialog(mContext, OAuthConstants.EMPTY_TOKEN, mListener);
            } else {
                mService.mAccessToken = result;
                mListener.onSuccess(result);
            }
        }
    }
    //
    // methods below is for request process.
    //
    protected static final ResponseChecker TRUE_CHECKER = new ResponseChecker() {
        @Override public boolean isSuccess(String response) throws Exception {
            return true;
        }

        @Override public ShareError getShareError(String response) throws Exception {
            return null;
        }
    };

    public interface ResponseChecker {
        boolean isSuccess(String response) throws Exception;
        ShareError getShareError(String response) throws Exception;
    }

    protected abstract ResponseChecker getResponseChecker(String action);

    protected boolean isRequestSuccess(String action, Response response){
        return isRequestSuccess(action, response, getResponseChecker(action));
    }

    private boolean isRequestSuccess(String action, Response response, ResponseChecker check) {
        try {
            if (response.isSuccessful()) {
                if (check == null || check.isSuccess(response.getBody())) {
                    mShareError = null;
                    return true;
                }
            }
            if (check != null) {
                mShareError = check.getShareError(response.getBody());
            }
        } catch (Exception e) {
            // IGNORE
        }

        // HERE, the request is fail, so print the reponse
        logResponse(action, response);
        return false;
    }

    protected void logResponse(String action, Response response) {
        StringBuilder builder = new StringBuilder();
        builder.append(TextUtils.isEmpty(action)? "Request":action).append(" fail.")
               .append("\nResponse code=").append(response.getCode());
        builder.append("\nHeaders are:");
        for (Map.Entry<String,String> entry: response.getHeaders().entrySet()) {
            builder.append("\n\t").append(entry.getKey()).append("=").append(entry.getValue());
        }
        builder.append("\nResponse body is:")
               .append(response.getBody());
        Log.w(TAG, builder.toString());
    }
}
