/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.xingen.volleylib.volley.toolbox;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.xingen.volleylib.volley.AuthFailureError;


/**
 * An Authenticator that uses {@link AccountManager} to get auth
 * tokens of a specified type for a specified account.
 *
 * 用途：使用AccountManager来获取一个验证令牌从指定类型的指定账户中。
 */
public class AndroidAuthenticator implements Authenticator {
    private final Context mContext;
    private final Account mAccount;
    private final String mAuthTokenType;
    private final boolean mNotifyAuthFailure;

    /**
     * Creates a new authenticator.
     * @param context Context for accessing AccountManager
     * @param account Account to authenticate as
     * @param authTokenType Auth token type passed to AccountManager
     */
    public AndroidAuthenticator(Context context, Account account, String authTokenType) {
        this(context, account, authTokenType, false);
    }

    /**
     * Creates a new authenticator.
     * @param context Context for accessing AccountManager
     * @param account Account to authenticate as
     * @param authTokenType Auth token type passed to AccountManager
     * @param notifyAuthFailure Whether to raise a notification upon auth failure
     *
     *  参数
     */
    public AndroidAuthenticator(Context context, Account account, String authTokenType,
                                boolean notifyAuthFailure) {
        mContext = context;
        mAccount = account;
        mAuthTokenType = authTokenType;
        mNotifyAuthFailure = notifyAuthFailure;
    }

    /**
     * Returns the Account being used by this authenticator.
     *
     * 返回被认证的使用的账户
     */
    public Account getAccount() {
        return mAccount;
    }


    /**AccountManager:
     * 这类提供访问一个现在账号使用者的集中注册。用户每个账户输入一次凭证（账号和密码），通过一键批准授权访问在线资源。
     * 不同的在线服务器具备不同的处理账户和认证的方式。
     *
     * 认证器可以第三方编写，认证器处理账户凭证和存储账户信息的实际细节。
     *
     * AccountManager可以为运用程序生成认证令牌，身份验证令牌通常可AccountManager:重用和缓存，但是必须定期刷新。
     *
     *
     * 衍生点：
     *    降低钓鱼攻击的成功的可能性，使用授权令牌来刷新证书。
     *
     *    可能的情况下，账户名和密码不应该存储到设备上。而是使用用户提供的账号名和密码执行初始化认证，然后使用一个短暂的，特定服务的授权令牌。
     *
     *    可以被多个运用访问的服务器应该使用AxxountManager访问。若是可能的话，使用AccountManager类来执行基于云服务器的操作且不把密码存储到设备上。
     *
     *    使用AccountManager获取Account后，进入任何证书前需要检查CREATOR,避免将证书传递给错的运用
     *
     *    如果证书只用于你创建的运用，那么你可以用checkSignature（）验证访问的AccountManager的运用。或者一个运用需要使用证书，，可以使用KeyStore来存储。
     *
     * 使用案例连接：https://developer.android.com/training/id-auth/authenticate.html#Gather
     *
     * @return
     * @throws AuthFailureError
     */

    // TODO: Figure out what to do about notifyAuthFailure
    @SuppressWarnings("deprecation")
    @Override
    public String getAuthToken() throws AuthFailureError {
        //创建一个AccountManager 对象
        final AccountManager accountManager = AccountManager.get(mContext);
        //若是用户必须输入凭证，则可选择提出一个通知。获取到一个指定类型的特定账号的授权令牌
        AccountManagerFuture<Bundle> future = accountManager.getAuthToken(mAccount,
                mAuthTokenType, mNotifyAuthFailure, null, null);
        Bundle result;
        try {
            result = future.getResult();
        } catch (Exception e) {
            throw new AuthFailureError("Error while retrieving auth token", e);
        }
        String authToken = null;
        if (future.isDone() && !future.isCancelled()) {
            if (result.containsKey(AccountManager.KEY_INTENT)) {
                Intent intent = result.getParcelable(AccountManager.KEY_INTENT);
                throw new AuthFailureError(intent);
            }
            //获取身份验证令牌
            authToken = result.getString(AccountManager.KEY_AUTHTOKEN);
        }
        if (authToken == null) {
            throw new AuthFailureError("Got null auth token for type: " + mAuthTokenType);
        }

        return authToken;
    }

    @Override
    public void invalidateAuthToken(String authToken) {
        //从AccountManager缓存中移除一个验证令牌
        //该方法在主线程中调用是安全的。
        //注意点：在api22之前的版本，需使用USE_CREDENTIALS和MANAGE_ACCOUNTS 权限
        AccountManager.get(mContext).invalidateAuthToken(mAccount.type, authToken);
    }
}
