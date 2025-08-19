package com.batoni.badges.exception;

public class PluginLoadException extends Exception {
    public PluginLoadException () {}

    public PluginLoadException (String message) {
        super(message);
    }

    public PluginLoadException (Throwable cause) {
        super(cause);
    }

    public PluginLoadException (String message, Throwable cause) {
        super(message, cause);
    }
}
