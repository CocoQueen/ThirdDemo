package com.test.thirddemo;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.Profile;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.tencent.connect.UserInfo;
import com.tencent.connect.auth.QQToken;
import com.tencent.mm.opensdk.modelbase.BaseReq;
import com.tencent.mm.opensdk.modelbase.BaseResp;
import com.tencent.mm.opensdk.modelmsg.SendAuth;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;
import com.tencent.tauth.IUiListener;
import com.tencent.tauth.Tencent;
import com.tencent.tauth.UiError;
import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.DefaultLogger;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.Twitter;
import com.twitter.sdk.android.core.TwitterApiClient;
import com.twitter.sdk.android.core.TwitterAuthConfig;
import com.twitter.sdk.android.core.TwitterAuthToken;
import com.twitter.sdk.android.core.TwitterConfig;
import com.twitter.sdk.android.core.TwitterCore;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.TwitterSession;
import com.twitter.sdk.android.core.identity.TwitterAuthClient;
import com.twitter.sdk.android.core.models.User;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private TwitterAuthClient twitterAuthClient;
    private IWXAPI mIwxapi;
    private Tencent mTencent;
    private CallbackManager callbackManager;
    private String TAG = "==================";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //初始化facebook
        FacebookSdk.sdkInitialize(getApplicationContext());
        AppEventsLogger.activateApp(this);
        // 初始化Twitter
        TwitterConfig config = new TwitterConfig.Builder(this)
                .logger(new DefaultLogger(Log.DEBUG))
                .twitterAuthConfig(
                        new TwitterAuthConfig(
                                "填自己申请的",
                                "填自己申请的"
                        )
                )
                .debug(true)
                .build();
        Twitter.initialize(config);

        initFacebook();
        mIwxapi = WXAPIFactory.createWXAPI(this, "填自己申请的", false);
        mIwxapi.registerApp("填自己申请的");
        mIwxapi.handleIntent(getIntent(), new IWXAPIEventHandler() {
            @Override
            public void onReq(BaseReq baseReq) {

            }

            @Override
            public void onResp(BaseResp baseResp) {

            }
        });
    }

    public void authQQ(View view) {
        //QQ
        authQQ();
    }

    public void authWechat(View view) {
        //微信
        if (!mIwxapi.isWXAppInstalled()) {
            Toast.makeText(this, "您的设备未安装微信客户端", Toast.LENGTH_SHORT).show();
            return;
        } else {
            final SendAuth.Req req = new SendAuth.Req();
            req.scope = "snsapi_userinfo";
            req.state = "wechat_sdk_demo_test";
            mIwxapi.sendReq(req);
        }
    }

    public void authFacebook(View view) {
        //facebook
        authFaceBook();
    }

    public void authTwitter(View view) {
        //Twitter
        authTwitter();
    }

    private void initFacebook() {
        callbackManager = CallbackManager.Factory.create();
        LoginManager.getInstance().registerCallback(callbackManager,
                new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(LoginResult loginResult) {
                        AccessToken token = loginResult.getAccessToken();
                        GraphRequest graphRequest = GraphRequest.newMeRequest(token,
                                new GraphRequest.GraphJSONObjectCallback() {
                                    @Override
                                    public void onCompleted(JSONObject object, GraphResponse response) {
                                        if (null != object) {
                                            String facebook_id = object.optString("id");
                                            String facebook_name = object.optString("name");
                                        }
                                    }
                                });
                        Bundle parameters = new Bundle();
                        parameters.putString(
                                "fields",
                                "id,email," +
//                                "gender," +
                                        "name,picture"
                        );
                        graphRequest.setParameters(parameters);
                        graphRequest.executeAsync();
                        updateUI();
                    }

                    @Override
                    public void onCancel() {
                        CookieSyncManager.createInstance(MainActivity.this);
                        CookieManager cookieManager = CookieManager.getInstance();
                        cookieManager.removeAllCookie();
                        CookieSyncManager.getInstance().sync();
                    }

                    @Override
                    public void onError(FacebookException exception) {
                    }
                });
    }

    private void updateUI() {
        boolean enableButtons = AccessToken.getCurrentAccessToken() != null;
        Profile profile = Profile.getCurrentProfile();
        if (enableButtons && null != profile) {
            String name = profile.getName();
            Uri linkUri = profile.getLinkUri();
            String s = linkUri.toString();
            Uri pictureUri = profile.getProfilePictureUri(100, 100);
            String userId = AccessToken.getCurrentAccessToken().getUserId();
            Log.e(TAG, "updateUI: " + name + userId);
        }
    }

    private List<String> permissions = Arrays.asList(/*"email", "user_likes",
            "user_status", "user_photos", "user_birthday",*/ "public_profile"/*, "user_friends"*/);

    private void authFaceBook() {
        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        boolean isLoggedIn = accessToken != null && !accessToken.isExpired();
        if (isLoggedIn) {
            LoginManager.getInstance().logOut();
        }
        LoginManager.getInstance().logInWithReadPermissions(this, permissions);
    }

    private void authQQ() {
        if (null == mTencent) {
            mTencent = Tencent.createInstance("填自己申请的", getApplicationContext());
        }
        if (mTencent.isSessionValid()) {
            mTencent.logout(this);
        }
        mTencent.login(this, "all", loginListener);
    }

    private void authTwitter() {
        if (null != twitterAuthClient) {
            twitterAuthClient.cancelAuthorize();
        }
        twitterAuthClient = new TwitterAuthClient();
        TwitterSession activeSession = TwitterCore.getInstance().getSessionManager().getActiveSession();
//        if (null == activeSession) {
        twitterAuthClient.authorize(this, new Callback<TwitterSession>() {
            @Override
            public void success(Result<TwitterSession> result) {
                TwitterAuthToken authToken = result.data.getAuthToken();
                TwitterSession twitterSession = result.data;
                getUserDetails(twitterSession);
            }

            @Override
            public void failure(TwitterException exception) {
            }
        });
//        }
    }

    public void getUserDetails(TwitterSession twitterSession) {
        TwitterApiClient twitterApiClient = new TwitterApiClient(twitterSession);
        twitterApiClient.getAccountService().verifyCredentials(true, false, true).enqueue(new Callback<User>() {
            @Override
            public void success(Result<User> userResult) {
                try {
                    User user = userResult.data;
                    String name = user.name;
                    long twitterID = user.getId();
                    String userSocialProfile = user.profileImageUrl;
                    String userEmail = user.email;
                    String userScreenName = user.screenName;
                    Log.e(TAG, "success: " + name + userEmail);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void failure(TwitterException e) {
            }
        });
    }

    String token;
    String expires_in;
    String thirdId;
    private IUiListener loginListener = new IUiListener() {
        @Override
        public void onComplete(Object o) {
            thirdId = ((JSONObject) o).optString("openid"); //QQ的openid
            try {
                token = ((JSONObject) o).getString("access_token");
                expires_in = ((JSONObject) o).getString("expires_in");
                QQToken qqtoken = mTencent.getQQToken();
                mTencent.setOpenId(thirdId);
                mTencent.setAccessToken(token, expires_in);
                UserInfo info = new UserInfo(getApplicationContext(), qqtoken);
                info.getUserInfo(new IUiListener() {
                    @Override
                    public void onComplete(Object o) {
                        String name = ((JSONObject) o).optString("nickname");
                        String avatar = ((JSONObject) o).optString("figureurl_qq_2");
                        Log.e(TAG, "onComplete: " + name + avatar);

                    }

                    @Override
                    public void onError(UiError uiError) {
                    }

                    @Override
                    public void onCancel() {
                    }

                    @Override
                    public void onWarning(int i) {

                    }
                });
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onError(UiError uiError) {
        }

        @Override
        public void onCancel() {
        }

        @Override
        public void onWarning(int i) {

        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Tencent.onActivityResultData(requestCode, resultCode, data, loginListener);
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == com.tencent.connect.common.Constants.REQUEST_API) {
            if (resultCode == com.tencent.connect.common.Constants.REQUEST_QQ_SHARE ||
                    resultCode == com.tencent.connect.common.Constants.REQUEST_QZONE_SHARE ||
                    resultCode == com.tencent.connect.common.Constants.REQUEST_OLD_SHARE) {
                Tencent.handleResultData(data, loginListener);
            }
        }
        if (requestCode == 64206) {
            callbackManager.onActivityResult(requestCode, resultCode, data);
        }
        if (requestCode == TwitterAuthConfig.DEFAULT_AUTH_REQUEST_CODE) {
            twitterAuthClient.onActivityResult(requestCode, resultCode, data);
        }
    }
}