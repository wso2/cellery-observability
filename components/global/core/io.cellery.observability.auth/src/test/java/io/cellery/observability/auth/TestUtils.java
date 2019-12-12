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

package io.cellery.observability.auth;

import com.google.gson.JsonObject;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Testing related utilities.
 */
public class TestUtils {

    /**
     * Generate a mock Http Response object that would return the string of the JSON object provided.
     *
     * @param jsonObject The JSON obgenerateHttpResponseject to be returned from the HTTP response
     * @param statusCode The status code of the response
     * @return The HTTP response
     * @throws Exception if mocking fails
     */
    public static HttpResponse generateHttpResponse(JsonObject jsonObject, int statusCode) throws Exception {
        char[] resultantCharArray = jsonObject.toString().toCharArray();
        HttpEntity httpEntity = Mockito.mock(HttpEntity.class);
        InputStream inputStream = Mockito.mock(InputStream.class);
        Mockito.when(httpEntity.getContent()).thenReturn(inputStream);
        Mockito.when(httpEntity.getContentLength()).thenReturn((long) resultantCharArray.length);

        HttpResponse response = Mockito.mock(HttpResponse.class);
        Mockito.when(response.getEntity()).thenReturn(httpEntity);
        StatusLine statusLine = Mockito.mock(StatusLine.class);
        Mockito.when(statusLine.getStatusCode()).thenReturn(statusCode);
        Mockito.when(response.getStatusLine()).thenReturn(statusLine);

        NameValuePair nameValuePair = Mockito.mock(NameValuePair.class);
        Mockito.when(nameValuePair.getName()).thenReturn("charset");
        Mockito.when(nameValuePair.getValue()).thenReturn(StandardCharsets.UTF_8.toString());
        HeaderElement headerElement = Mockito.mock(HeaderElement.class);
        Mockito.when(headerElement.getParameters()).thenReturn(new NameValuePair[]{nameValuePair});
        Header header = Mockito.mock(Header.class);
        Mockito.when(header.getElements()).thenReturn(new HeaderElement[]{headerElement});
        Mockito.when(httpEntity.getContentType()).thenReturn(header);

        InputStreamReader inputStreamReader = Mockito.mock(InputStreamReader.class);
        PowerMockito.whenNew(InputStreamReader.class)
                .withParameterTypes(InputStream.class, Charset.class)
                .withArguments(Mockito.eq(inputStream), Mockito.any(Charset.class))
                .thenReturn(inputStreamReader);

        Mockito.when(inputStreamReader.read(Mockito.any(char[].class)))
                .thenAnswer(invocationOnMock -> {
                    char[] charArray = invocationOnMock.getArgumentAt(0, char[].class);
                    System.arraycopy(resultantCharArray, 0, charArray, 0, resultantCharArray.length);
                    return resultantCharArray.length;
                })
                .thenReturn(-1);

        return response;
    }

    private TestUtils() {   // Prevent initialization
    }
}
