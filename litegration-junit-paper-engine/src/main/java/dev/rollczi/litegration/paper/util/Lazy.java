package dev.rollczi.litegration.paper.util;

import org.junit.jupiter.api.function.ThrowingSupplier;

public class Lazy<T> {

    private final ThrowingSupplier<T> supplier;
    private boolean initialized;
    private T value;

    public Lazy(ThrowingSupplier<T> supplier) {
        this.supplier = supplier;
    }

    public synchronized T get() {
        if (this.initialized) {
            return this.value;
        } else {
            this.initialized = true;
            try {
                return this.value = this.supplier.get();
            }
            catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        }
    }

}
