package org.example.genpass;

public final class App {

    private static final String FALLBACK_VERSION = "1.0-SNAPSHOT";

    private App() {
    }

    public static void main(String[] args) {
        System.out.println("genpass " + version());
    }

    static String version() {
        String v = App.class.getPackage().getImplementationVersion();
        return v != null ? v : FALLBACK_VERSION;
    }
}
