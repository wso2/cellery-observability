package io.cellery.observability.model.generator.datasource;

import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;

/**
 * Custom context factory for unit tests.
 */
public class CustomContextFactory implements InitialContextFactory {
    @Override
    public Context getInitialContext(Hashtable<?, ?> environment) throws NamingException {
        return new CustomContext(environment);
    }
}
