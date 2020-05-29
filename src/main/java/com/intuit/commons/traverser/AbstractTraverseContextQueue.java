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

import java.util.Objects;
import java.util.Queue;

/**
 *
 * @author gkesler
 * @param <T> traversable type
 * @param <Q> implementing type of a memory structure (stack ({@link java.util.Deque}) or queue)
 */

abstract public class AbstractTraverseContextQueue<T, Q extends Queue<TraverseContext<T>>> implements TraverseContextQueue<T> {    
    AbstractTraverseContextQueue(Q impl, Traverser<? super T, ? super TraverseContext<T>> outer) {
        this.impl = Objects.requireNonNull(impl);
        this.outer = Objects.requireNonNull((Traverser<T, TraverseContext<T>>)outer);
    }

    @Override
    public final boolean isEmpty() {
        return impl.isEmpty();
    }

    protected final TraverseContext<T> newPostOrderContext(TraverseContext<? super T> preOrder) {
        return outer.newPostOrderContext(preOrder);
    }
    
    protected final Q impl;
    protected final Traverser<T, TraverseContext<T>> outer;    
}
