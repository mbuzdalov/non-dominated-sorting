package ru.ifmo.nds.util.median;

import java.lang.reflect.InvocationTargetException;

/**
 * This interface encapsulates factories to create instances of {@link DestructiveMedianAlgorithm}
 * given their maximum allowed size.
 *
 * @author Maxim Buzdalov
 */
public interface DestructiveMedianFactory {
    /**
     * <p>
     * Creates an instance of the algorithm performing destructive median search
     * that is capable of working on indices smaller than <code>maxSize</code>.
     * Generally this means that the algorithm is able to find a median in an array of size equal to,
     * or smaller than, <code>maxSize</code>, although the actual contract is more complicated
     * (see {@link DestructiveMedianAlgorithm} for details).
     * </p>
     * <p>
     * Implementations may return the same object as long as it does not have internal storage that depends
     * on the size requirements, and can support concurrent queries with overlapping index ranges.
     * </p>
     *
     * @param maxSize the maximum size supported (that is, the maximum value of the <code>until</code> argument
     *                in a {@link DestructiveMedianAlgorithm#solve(double[], int, int)} call).
     * @return the algorithm for computing medians destructively.
     */
    DestructiveMedianAlgorithm createInstance(int maxSize);

    static DestructiveMedianFactory getByName(String name)
            throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Class<?> myClass = DestructiveMedianFactory.class;
        String factoryClassName = myClass.getName().replace(myClass.getSimpleName(), name);
        return (DestructiveMedianFactory) Class.forName(factoryClassName).getMethod("factory").invoke(null);
    }
}
