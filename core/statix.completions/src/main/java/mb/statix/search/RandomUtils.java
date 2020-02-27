package mb.statix.search;

import java.lang.reflect.Field;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;


/**
 * Utility functions for working with {@link Random}.
 */
public final class RandomUtils {

    /**
     * Gets the seed of a {@see Random} instance.
     *
     * @param rng the {@link Random} instance
     * @return the seed; or 0 when it could not be determined or is zero
     */
    public static long getSeed(Random rng) {
        try
        {
            Field field = Random.class.getDeclaredField("seed");
            field.setAccessible(true);
            // XOR with a magic constant to get the original seed
            return ((AtomicLong) field.get(rng)).get() ^ 0x5DEECE66DL;
        } catch (IllegalAccessException | NoSuchFieldException e) {
            // Could not determine seed.
            return 0;
        }
    }

}
