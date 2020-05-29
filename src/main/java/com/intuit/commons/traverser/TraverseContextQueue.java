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

import java.util.Optional;
import java.util.stream.Stream;

/**
 * Collection of pending Contexts to visit while traversing.
 * 
 * Most important queues are:
 * <ul>
 *  <li>LIFO, i.e. Stack - for DFS traversal</li>
 *  <li>FIFO, i.e. Queue - for BFS traversal</li>
 * </ul>
 *
 * @author gkesler
 */
public interface TraverseContextQueue<T> {
    /**
     * Verifies if traversal is finished.
     * Traversal can finish naturally, when there are no more pending elements
     * in the traversal queue or is traversal was aborted by the client code.
     * 
     * @param action client provided action to control traversal loop. 
     * @return {@code true} if traversal is finished.
     */
    default boolean isDone(Traverser.Action action) {
        return action == Traverser.Action.QUIT || isEmpty();
    }
    /**
     * Verifies if traversal queue is empty.
     * 
     * @return {@code true} if the queue is empty.
     */
    boolean isEmpty();
    /**
     * Reads and removes the next element from the traversal queue.
     * 
     * @return the next element from the queue.
     */
    TraverseContext<T> pop();
    /**
     * Reads the next element in the queue without removing.
     * 
     * @return the next element from the queue.
     */
    TraverseContext<T> peek ();
    /**
     * Offers provided contexts into the traversal queue.
     * If parent context is specified, creates and enqueues POST-ORDER context for it.
     * 
     * @param parent    parent context to enqueue for post order processing
     * @param contexts  stream of child contexts
     */
    void pushAll(Optional<TraverseContext<? super T>> parent, Stream<TraverseContext<T>> contexts);    
}
