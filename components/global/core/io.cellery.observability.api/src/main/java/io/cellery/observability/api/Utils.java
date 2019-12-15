/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package io.cellery.observability.api;

import io.cellery.observability.api.exception.InvalidParamException;

/**
 * Common utilities for the API.
 */
public class Utils {

    /**
     * Validate and check if a text is a valid Cellery ID.
     *
     * @param paramName The name of the parameter
     * @param text The text to test for
     * @throws InvalidParamException if the text is not a valid Cellery ID
     */
    public static void validateCelleryIdParam(String paramName, String text) throws InvalidParamException {
        if (!Constants.CELLERY_ID_PATTERN.matcher(text).matches()) {
            throw new InvalidParamException(paramName, "a string of lower case letters, " +
                    "numbers and dashes with not leading or trailing dashes");
        }
    }

    /**
     * Validate and check if a query range is correct.
     *
     * @param queryStartTime Start timestamp of the query range
     * @param queryEndTime End timestamp of the query range
     * @throws InvalidParamException if the query range is invalid
     */
    public static void validateQueryRangeParam(long queryStartTime, long queryEndTime) throws InvalidParamException {
        if (queryStartTime <= 0) {
            throw new InvalidParamException("queryStartTime", "value greater than zero");
        }
        if (queryEndTime <= 0) {
            throw new InvalidParamException("queryEndTime", "value greater than zero");
        }
        if (queryStartTime >= queryEndTime) {
            throw new InvalidParamException("queryEndTime", "value greater than queryStartTime");
        }
    }

    /**
     * Validate and check if a trace ID is correct.
     *
     * @param text The trace ID to validate
     * @throws InvalidParamException if the trace ID is invalid
     */
    public static void validateTimeGranularityParam(String text) {
        if (!Constants.TIME_GRANULARITY_PATTERN.matcher(text).matches()) {
            throw new InvalidParamException("timeGranularity", "one of [second, minute, hour, day, month, year]");
        }
    }

    /**
     * Validate and check if a simple string is acceptable for the API.
     *
     * @param paramName The name of the parameter
     * @param text The text to validate
     * @throws InvalidParamException if the text is invalid
     */
    public static void validateSimpleStringParam(String paramName, String text) {
        if (!Constants.SIMPLE_STRING_PATTERN.matcher(text).matches()) {
            throw new InvalidParamException(paramName, "not to contain illegal characters [\", ']");
        }
    }

    /**
     * Generate a Siddhi match condition to match a set of values for a particular attribute.
     *
     * Eg:-
     *     Input  - traceId, ["id01", "id02", "id03"]
     *     Output - traceId == "id01" or traceId == "id02" or traceId == "id03"
     *
     * @param attributeName The name of the attribute
     * @param values The array of values from which at least one should match
     * @return The match condition which would match any value from the provided array
     */
    public static String generateSiddhiMatchConditionForMultipleValues(String attributeName, String[] values) {
        StringBuilder traceIdMatchConditionBuilder = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i != 0) {
                traceIdMatchConditionBuilder.append(" or ");
            }
            traceIdMatchConditionBuilder.append(attributeName)
                    .append(" == \"")
                    .append(values[i])
                    .append("\"");
        }
        return traceIdMatchConditionBuilder.toString();
    }

    private Utils() {   // Prevent initialization
    }
}
