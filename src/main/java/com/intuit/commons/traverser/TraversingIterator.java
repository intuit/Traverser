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
package com.intuit.commons.traverser;


import java.util.Iterator;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Standard Iterator extension that also providers traversal specific API.
 * 
 * @param <T> type of nodes in the graph.
 * 
 * @author gkesler
 */
public interface TraversingIterator<T> extends Iterator<T> {
    /**
     * Streams graph nodes that belong to the path from the current node to the traversal root.
     * 
     * @return stream of graph nodes on the path
     */
    default Stream<T> path () {
        return path(TraverseContext::thisNode);
    }
    /**
     * 
     * @param <E> element type of a path
     * @param func function to build path
     * @return stream of path elements
     */
    <E> Stream<E> path (Function<? super TraverseContext<T>, E> func);
    /**
     * 
     * @param newValue new value
     * @param replaceFunction function which performs replacement
     * @param <R> result
     *
     * @return result of a replacement
     */
    <R> R replace (Object newValue, BiFunction<? super TraverseContext<T>, ? super Object, ? extends R> replaceFunction);
}
