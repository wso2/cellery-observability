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
package io.cellery.observability.api.datasource;

import java.util.HashMap;
import java.util.Hashtable;
import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NameNotFoundException;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

/**
 * Custom context for unit tests.
 */
public class CustomContext implements Context {

    private HashMap<String, Object> contextObjects;
    private Hashtable<?, ?> environment;

    CustomContext(Hashtable<?, ?> environment) {
        this.contextObjects = new HashMap<>();
        this.environment = environment;
    }

    @Override
    public Object lookup(Name name) {
        // Do Nothing
        return null;
    }

    @Override
    public Object lookup(String name) throws NamingException {
        Object obj = contextObjects.get(name);
        if (obj == null) {
            throw new NameNotFoundException("No binding found for the name: " + name);
        }
        return obj;
    }

    @Override
    public void bind(Name name, Object obj) {
        // Do Nothing
    }

    @Override
    public void bind(String name, Object obj) {
        contextObjects.put(name, obj);
    }

    @Override
    public void rebind(Name name, Object obj) {
        // Do Nothing
    }

    @Override
    public void rebind(String name, Object obj) {
        contextObjects.put(name, obj);
    }

    @Override
    public void unbind(Name name) {
        // Do Nothing
    }

    @Override
    public void unbind(String name) {
        contextObjects.remove(name);
    }

    @Override
    public void rename(Name oldName, Name newName) {
        // Do Nothing
    }

    @Override
    public void rename(String oldName, String newName) {
        // Do Nothing
    }

    @Override
    public NamingEnumeration<NameClassPair> list(Name name) {
        // Do Nothing
        return null;
    }

    @Override
    public NamingEnumeration<NameClassPair> list(String name) {
        // Do Nothing
        return null;
    }

    @Override
    public NamingEnumeration<Binding> listBindings(Name name) {
        // Do Nothing
        return null;
    }

    @Override
    public NamingEnumeration<Binding> listBindings(String name) {
        // Do Nothing
        return null;
    }

    @Override
    public void destroySubcontext(Name name) {
        // Do Nothing
    }

    @Override
    public void destroySubcontext(String name) {
        // Do Nothing
    }

    @Override
    public Context createSubcontext(Name name) {
        // Do Nothing
        return null;
    }

    @Override
    public Context createSubcontext(String name) {
        CustomContext subContext = new CustomContext(environment);
        contextObjects.put(name, subContext);
        return subContext;
    }

    @Override
    public Object lookupLink(Name name) {
        // Do Nothing
        return null;
    }

    @Override
    public Object lookupLink(String name) {
        // Do Nothing
        return null;
    }

    @Override
    public NameParser getNameParser(Name name) {
        // Do Nothing
        return null;
    }

    @Override
    public NameParser getNameParser(String name) {
        // Do Nothing
        return null;
    }

    @Override
    public Name composeName(Name name, Name prefix) {
        // Do Nothing
        return null;
    }

    @Override
    public String composeName(String name, String prefix) {
        // Do Nothing
        return null;
    }

    @Override
    public Object addToEnvironment(String propName, Object propVal) {
        // Do Nothing
        return null;
    }

    @Override
    public Object removeFromEnvironment(String propName) {
        // Do Nothing
        return null;
    }

    @Override
    public Hashtable<?, ?> getEnvironment() {
        // Do Nothing
        return null;
    }

    @Override
    public void close() {
        // Do Nothing
    }

    @Override
    public String getNameInNamespace() {
        // Do Nothing
        return null;
    }
}
