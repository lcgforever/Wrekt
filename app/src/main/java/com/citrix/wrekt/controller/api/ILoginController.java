package com.citrix.wrekt.controller.api;

import com.citrix.wrekt.data.LoginState;

public interface ILoginController {

    void signUpThenLogin(String email, String password, String username);

    void login(LoginState loginState, String email, String password, String username);

    void loginWithOAuth(LoginState loginState, String authClient, String authToken, String email);

    void logout();
}
