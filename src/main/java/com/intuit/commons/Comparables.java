/**
 * Copyright 2019 Intuit Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intuit.commons;

import java.util.Comparator;
import java.util.Objects;

/**
 *
 * @author gkesler
 */
public class Comparables {
    // disable instantiation
    private Comparables () {
    }
    
    /**
     * Compares two enum values and returns one with the highest ordinal value
     * 
     * @param <E> type of Comparable objects in this method
     * 
     * @param left  LHS of comparison
     * @param right RHS of comparison
     * @return left argument if its ordinal value is highest, or right otherwise
     */
    public static <E extends Comparable<? super E>> E max (E left, E right) {
        return Objects.compare(left, right, Comparator.naturalOrder()) > 0
                ? left 
                : right;
    }
    
    /**
     * Compares two enum values and returns one with the lowest ordinal value
     * 
     * @param <E> type of Comparable objects in this method
     * 
     * @param left  LHS of comparison
     * @param right RHS of comparison
     * @return left argument if its ordinal value is lowest, or right otherwise
     */
    public static <E extends Comparable<? super E>> E min (E left, E right) {
        return Objects.compare(left, right, Comparator.naturalOrder()) < 0
                ? left 
                : right;
    }
}
