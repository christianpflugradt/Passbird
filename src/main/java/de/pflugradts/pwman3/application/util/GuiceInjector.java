package de.pflugradts.pwman3.application.util;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

public class GuiceInjector {

    public Injector create(final Module... modules) {
        return Guice.createInjector(modules);
    }

}
