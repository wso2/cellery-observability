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

const CellIcon = (props) => (
    <SvgIcon viewBox="0 0 14 14" {...props}>
        <path d="M9.81.21H4.19l-4,4V9.81l4,4H9.81l4-4V4.19Zm3,9.19L9.4,12.79H4.6L1.21,9.4V4.6L4.6,1.21H9.4L12.79,4.6Z"/>
    </SvgIcon>
);

export default CellIcon;
