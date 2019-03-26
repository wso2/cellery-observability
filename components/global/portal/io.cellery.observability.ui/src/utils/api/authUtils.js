/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import HttpUtils from "./httpUtils";
import {StateHolder} from "../../components/common/state";
import jwtDecode from "jwt-decode";

/**
 * Authentication/Authorization related utilities.
 */


class AuthUtils {

    /**
     * Sign in the user.
     *
     * @param {Object} user The user to be signed in
     * @param {StateHolder} globalState The global state provided to the current component
     */
    static signIn = (user, globalState) => {
        if (user.username) {
            AuthUtils.updateUser(user, globalState);
        } else {
            throw Error(`Username provided cannot be "${user.username}"`);
        }
    };

    /**
     * Redirects the user to IDP for authentication.
     *
     * @param {StateHolder} globalState The global state provided to the current component
     */
    static redirectToIDP(globalState) {
        HttpUtils.callObservabilityAPI(
            {
                url: "/auth/client-id",
                method: "GET"
            },
            globalState).then((resp) => {
            window.location.href = `${globalState.get(StateHolder.CONFIG).idpAuthURL}`
                + `&client_id=${resp}&`
                + `redirect_uri=${globalState.get(StateHolder.CONFIG).callBackURL}&nonce=auth&scope=openid`;
        }).catch((err) => {
            throw Error(`Failed to redirect to Identity Provider for Authentication. ${err}`);
        });

        /*
         * Window.location.href = `https://10.100.4.121:9443/oauth2/authorize?response_type=code`
         *     + `&client_id=H87NsL_4MG_FVsx8hzRmfEeCxFQa&`
         *     + `redirect_uri=http://localhost:3000&nonce=auth&scope=openid`;
         */
    }

    /**
     * Requests the API backend for tokens in exchange for authorization code.
     *
     * @param {string} oneTimeCode The one time Authorization code given by the IDP.
     * @param {StateHolder} globalState The global state provided to the current component
     */
    static getTokens(oneTimeCode, globalState) {
        HttpUtils.callObservabilityAPI(
            {
                url: `/auth/tokens/${oneTimeCode}`,
                method: "GET"
            },
            globalState
        ).then((resp) => {
            const decoded = jwtDecode(resp.id_token);
            const user1 = {
                username: decoded.sub,
                accessToken: resp.access_token,
                idToken: resp.id_token
            };
            AuthUtils.signIn(user1, globalState);
        });

        /*
         * axios.post(`https://10.100.4.121:9443/oauth2/token?grant_type=authorization_code&code=${
         *     oneTimeCode}&redirect_uri=http://localhost:3000`, null, {
         *     headers: {
         *         "Content-Type": "application/x-www-form-urlencoded",
         *         Authorization:
         *             "Basic SDg3TnNMXzRNR19GVnN4OGh6Um1mRWVDeEZRYTpzSm5oVUNLSGdmV2o2SXFmbTRyY1F3eWtEa2dh"
         *     }
         * }).then((response) => {
         *     localStorage.setItem("idToken", response.data.id_token);
         *     const decoded = jwtDecode(response.data.id_token);
         *     const user1 = {
         *         username: decoded.sub,
         *         accessToken: response.data.access_token,
         *         idToken: response.data.id_token
         *     };
         *     AuthUtils.signIn(user1, globalState);
         * }).catch((err) => {
         *     alert(err);
         * });
         */
    }

    /**
     * Updates the StateHolder and localStorage with new user object.
     *
     * @param {Object} user The user object which has been created.
     * @param {StateHolder} globalState The global state provided to the current component
     */
    static updateUser = (user, globalState) => {
        localStorage.setItem(StateHolder.USER, JSON.stringify(user));
        globalState.set(StateHolder.USER, user);
    };

    /**
     * Sign out the current user.
     * The provided global state will be updated accordingly as well.
     *
     * @param {StateHolder} globalState The global state provided to the current component
     */
    static signOut = (globalState) => {
        // Const idToken = globalState.get(StateHolder.USER).idToken;
        localStorage.setItem("isLoggedout", "true");
        window.location.href = `https://10.100.4.121:9443/oidc/logout?id_token_hint=
            ${localStorage.getItem("idToken")}&post_logout_redirect_uri=http://localhost:3000`;
    };

    /**
     * Get the currently authenticated user.
     *
     * @returns {string} The current user
     */
    static getAuthenticatedUser = () => {
        let user;
        try {
            user = JSON.parse(localStorage.getItem(StateHolder.USER));
        } catch {
            user = null;
            localStorage.removeItem(StateHolder.USER);
        }
        return user;
    };

}

export default AuthUtils;
