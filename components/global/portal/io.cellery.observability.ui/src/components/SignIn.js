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
import Avatar from "@material-ui/core/Avatar";
import Button from "@material-ui/core/Button";
import Checkbox from "@material-ui/core/Checkbox";
import CssBaseline from "@material-ui/core/CssBaseline";
import FormControl from "@material-ui/core/FormControl";
import FormControlLabel from "@material-ui/core/FormControlLabel";
import Input from "@material-ui/core/Input";
import InputLabel from "@material-ui/core/InputLabel";
import LockIcon from "@material-ui/icons/LockOutlined";
import Paper from "@material-ui/core/Paper";
import React from "react";
import Typography from "@material-ui/core/Typography";
import withStyles from "@material-ui/core/styles/withStyles";
import withGlobalState, {StateHolder} from "./common/state";
import * as PropTypes from "prop-types";
import axios from "axios";
import CircularProgress from "@material-ui/core/CircularProgress/CircularProgress";
import jwt_decode from "jwt-decode";


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
                alert("hit null");
                localStorage.setItem("isAuthenticated", "true");
                window.location.href = "https://192.168.56.1:9443/oauth2/authorize?response_type=code"
                    + "&client_id=tNK8tIR21bfVaP1occAZ5QmrJRAa&"
                    + "redirect_uri=http://localhost:3000&nonce=abc&scope=openid";
            }

            else if (localStorage.getItem("isAuthenticated") === "true" && !searchParams.has("code")){
                window.location.href = "https://192.168.56.1:9443/oauth2/authorize?response_type=code"
                    + "&client_id=tNK8tIR21bfVaP1occAZ5QmrJRAa&"
                    + "redirect_uri=http://localhost:3000&nonce=abc&scope=openid";
            }
            else if (searchParams.has("code") && localStorage.getItem("isAuthenticated") !== "codeAuthorized") {
                const oneTimeToken = searchParams.get("code");
                const data = {
                    grant_type: "authorization_code",
                    code: oneTimeToken,
                    redirect_uri: "http://localhost:3000"

                };
                // const requestData = Object.keys(data).map((key) => `${encodeURIComponent(key)}=
                // ${encodeURIComponent(data[key])}`).join("&");

                axios.post("https://192.168.56.1:9443/oauth2/token?grant_type=authorization_code&code=" +
                    oneTimeToken + "&redirect_uri=http://localhost:3000", null, {
                    headers: {
                        "Content-Type": "application/x-www-form-urlencoded",
                        Authorization: "Basic dE5LOHRJUjIxYmZWYVAxb2NjQVo1UW1ySlJBYTpqVHI1SDlZTWMyWjRlaDZORmpDZkpuU0dXdzhh"
                    }
                }).then((response) => {
                    alert(response.data.id_token);
                    localStorage.setItem("idToken",response.data.id_token);
                });
                localStorage.setItem("isAuthenticated", "codeAuthorized");
                window.location.reload();
            }
            else if (localStorage.getItem("isAuthenticated") === "codeAuthorized") {
                const decoded = jwt_decode(localStorage.getItem("idToken"));

                const user1 = {
                    username: decoded.sub
                };
                AuthUtils.signIn(user1.username,globalState);
            }


        }

        else if (localStorage.getItem("isAuthenticated") === "loggedOut"){
            localStorage.removeItem(StateHolder.USER)
            window.location.href =  "https://192.168.56.1:9443/oidc/logout?id_token_hint="+localStorage.getItem("idToken")+"&post_logout_redirect_uri=http://localhost:3000";
        }
    }

}

SignIn.propTypes = {
    classes: PropTypes.object.isRequired,
    globalState: PropTypes.instanceOf(StateHolder).isRequired
};

export default withStyles(styles)(withGlobalState(SignIn));
