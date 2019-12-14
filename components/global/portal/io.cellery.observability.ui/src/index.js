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

import "./index.css";
import App from "./components/App";
import Constants from "./utils/constants";
import Logger from "js-logger";
import React from "react";
import ReactDOM from "react-dom";
import * as moment from "moment";
import * as serviceWorker from "./serviceWorker";

// Resolving the Log Level
const LOG_LEVEL_KEY = "LOG_LEVEL";
let logLevel;
switch (localStorage.getItem(LOG_LEVEL_KEY)) {
    case "TRACE":
        logLevel = Logger.TRACE;
        break;
    case "DEBUG":
        logLevel = Logger.DEBUG;
        break;
    case "INFO":
        logLevel = Logger.INFO;
        break;
    case "WARN":
        logLevel = Logger.WARN;
        break;
    case "ERROR":
        logLevel = Logger.ERROR;
        break;
    case "OFF":
        logLevel = Logger.OFF;
        break;
    default:
        logLevel = Logger.ERROR;
}
Logger.useDefaults({
    defaultLevel: logLevel,
    formatter: (messages, context) => {
        messages.unshift(`[${moment().format(Constants.Pattern.PRECISE_DATE_TIME)}] ${
            context.level.name}${context.name ? ` {${context.name}}` : ""} -`);
    }
});
const logger = Logger.get("index");
logger.info(`Initialized logging with Log Level ${logLevel.name}`);

logger.info("Rendering Observability Portal");
ReactDOM.render((<App/>), document.getElementById("root"));

/*
 * If you want your app to work offline and load faster, you can change
 * unregister() to register() below. Note this comes with some pitfalls.
 * Learn more about service workers: http://bit.ly/CRA-PWA
 */
serviceWorker.unregister();
