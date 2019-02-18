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

import AuthUtils from "../utils/api/authUtils";
import CircularProgress from "@material-ui/core/CircularProgress/CircularProgress";
import HttpUtils from "../utils/api/httpUtils";
import React from "react";
import jwtDecode from "jwt-decode";
import withStyles from "@material-ui/core/styles/withStyles";
import withGlobalState, {StateHolder} from "./common/state";
import * as PropTypes from "prop-types";

const styles = (theme) => ({
    layout: {
        width: "auto",
        display: "block", // Fix IE 11 issue.
        marginLeft: theme.spacing.unit * 3,
        marginRight: theme.spacing.unit * 3,
        [theme.breakpoints.up(400 + (theme.spacing.unit * 3 * 2))]: {
            width: 400,
            marginLeft: "auto",
            marginRight: "auto"
        }
    },
    paper: {
        marginTop: theme.spacing.unit * 8,
        display: "flex",
        flexDirection: "column",
        alignItems: "center",
        padding: `${theme.spacing.unit * 2}px ${theme.spacing.unit * 3}px ${theme.spacing.unit * 3}px`
    },
    avatar: {
        margin: theme.spacing.unit,
        backgroundColor: theme.palette.secondary.main
    },
    form: {
        width: "100%", // Fix IE 11 issue.
        marginTop: theme.spacing.unit
    },
    submit: {
        marginTop: theme.spacing.unit * 3
    },
    centerDiv: {
        position: "absolute",
        margin: "auto",
        top: 0,
        right: 0,
        bottom: 0,
        left: 0,
        width: "200px",
        height: "100px"

    }
});

const idpAddress = "gateway.cellery-system:9443";

class SignIn extends React.Component {

    handleLogin = () => {
        const {globalState} = this.props;

        const username = document.getElementById("username").value;
        AuthUtils.signIn(username, globalState);
    };

    handleKeyPress = (event) => {
        if (event.key === "Enter") {
            this.handleLogin();
        }
    };

    render() {
        const {classes} = this.props;
        return (
            <React.Fragment className={classes.progress}>
                <div className={classes.centerDiv}>
                    <CircularProgress/>
                    <div>
                        Loading
                    </div>
                </div>

            </React.Fragment>
        );
    }

    componentDidMount() {
        const url = window.location.search.substr(1);
        const searchParams = new URLSearchParams(url);
        const {globalState} = this.props;
        if (localStorage.getItem("isAuthenticated") === null || localStorage.getItem(StateHolder.USER) === null) {
            if (localStorage.getItem("isAuthenticated") !== "true"
                && localStorage.getItem("isAuthenticated") !== "codeAuthorized") {
                localStorage.setItem("isAuthenticated", "true");
                window.location.href = `https://${idpAddress}/oauth2/authorize?response_type=code`
                    + "&client_id=IwjnlXzbrVpe0Ft0HHXiRImnS98a&"
                    + "redirect_uri=http://localhost:3000&nonce=abc&scope=openid";
            } else if (localStorage.getItem("isAuthenticated") === "true" && !searchParams.has("code")) {
                window.location.href = `https://${idpAddress}/oauth2/authorize?response_type=code`
                    + "&client_id=IwjnlXzbrVpe0Ft0HHXiRImnS98a&"
                    + "redirect_uri=http://localhost:3000&nonce=abc&scope=openid";
            } else if (searchParams.has("code") && localStorage.getItem("isAuthenticated") !== "codeAuthorized") {
                const oneTimeToken = searchParams.get("code");

                /*
                 * Const requestData = Object.keys(data).map((key) => `${encodeURIComponent(key)}=
                 * ${encodeURIComponent(data[key])}`).join("&");
                 */
                HttpUtils.callObservabilityAPI(
                    {
                        url: `/user-auth/requestToken/${oneTimeToken}`,
                        method: "GET"
                    },
                    globalState).then((resp) => {
                    localStorage.setItem("idToken", resp.data);
                    const decoded = jwtDecode(resp.data);
                    const user1 = {
                        username: decoded.sub
                    };
                    AuthUtils.signIn(user1.username, globalState);
                });


                /*
                 * axios.get(`http://0.0.0.0:9090/user-auth/requestToken/${oneTimeToken}`).then((response) => {
                 *     localStorage.setItem("idToken", response.data);
                 *     alert("rsponse -" + response.data);
                 *     const decoded = jwt_decode(response.data);
                 *     const user1 = {
                 *         username: decoded.sub
                 *     };
                 *     AuthUtils.signIn(user1.username, globalState);
                 * }).catch((err) => {
                 *     alert(err);
                 * });
                 */
            }
        } else if (localStorage.getItem("isAuthenticated") === "loggedOut") {
            localStorage.removeItem(StateHolder.USER);
            window.location.href = `https://${idpAddress}/oidc/logout?id_token_hint=
            ${localStorage.getItem("idToken")}&post_logout_redirect_uri=http://localhost:3000`;
        }
    }

}

SignIn.propTypes = {
    classes: PropTypes.object.isRequired,
    globalState: PropTypes.instanceOf(StateHolder).isRequired
};

export default withStyles(styles)(withGlobalState(SignIn));
