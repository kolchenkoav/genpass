package org.example.genpass.core;

/** Параметры PIN: длина 3–12, опция «без ведущих нулей». */
public record PinOptions(int length, boolean noLeadingZero) {

    public PinOptions {
        if (length < 3 || length > 12) {
            throw new IllegalArgumentException("length must be between 3 and 12");
        }
    }
}
