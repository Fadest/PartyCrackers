package dev.fadest.partycrackers.utils;

import org.jetbrains.annotations.NotNull;

/**
 * A pair of objects. Used for storing two objects in a single object.
 * This is useful for storing two objects in a single object, such as the pair of a player and their party.
 *
 * @param <F> The first object in the pair.
 * @param <S> The second object in the pair.
 */
public record Pair<F, S>(F first, S second) {

    /**
     * Creates a new Pair object with the given first and second objects.
     *
     * @param first  The first object in the pair.
     * @param second The second object in the pair.
     * @return A new pair with the given objects.
     */
    public static <F, S> Pair<F, S> of(@NotNull F first, @NotNull S second) {
        return new Pair<>(first, second);
    }

}
