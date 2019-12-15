/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import React from "react";
import SvgIcon from "@material-ui/core/SvgIcon";

const HttpTrafficIcon = (props) => (
    <SvgIcon viewBox="0 0 14 14" {...props}>
        <path d="M1.1,6.2V2.9c0-0.2,0.2-0.4,0.4-0.4h8.8V0.2l2.4,2.6c0.1,0.1,0.1,0.3,0,0.4l-2.5,2.6l0-2.3H2.1l0,2.9c0,0.2-0.2,0.4-0.4,0.4
H1.5C1.3,6.6,1.1,6.4,1.1,6.2z M12.5,7.4h-0.3c-0.2,0-0.4,0.2-0.4,0.4l0,2.9H3.7l0-2.3l-2.5,2.6c-0.1,0.1-0.1,0.3,0,0.4l2.4,2.6
v-2.3h8.8c0.2,0,0.4-0.2,0.4-0.4V7.8C12.9,7.6,12.7,7.4,12.5,7.4z"/>
    </SvgIcon>
);

export default HttpTrafficIcon;
