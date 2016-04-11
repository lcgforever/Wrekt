package com.citrix.wrekt.controller.api;

import com.citrix.wrekt.data.LoginState;

public interface ILoginController {

    void signUpThenLogin(String username, String password);

    void login(LoginState loginState, String username, String password);

    void loginWithOAuth(LoginState loginState, String authClient, String authToken);

    void logout();
}
