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


import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.stream.Stream;

/**
 *
 * @author gkesler
 */
class BreadthFirstQueue<T> extends AbstractTraverseContextQueue<T, Queue<TraverseContext<T>>> {
    public BreadthFirstQueue(Traverser<? super T, ? super TraverseContext<T>> outer) {
        super(new LinkedList<>(), outer);
    }
    
    @Override
    public TraverseContext<T> pop() {
        return impl.remove();
    }

    @Override
    public TraverseContext<T> peek() {
        return impl.peek();
    }

    @Override
    public void pushAll(Optional<TraverseContext<? super T>> parent, Stream<TraverseContext<T>> contexts) {
        // let parent have children: {child1, child2, ..., childN}
        // let stack have state: {ctx1, ctx2, ctx3, ..., ctxN}
        // BFS algorithm implies that the most recent nodes are visited last
        // therefore we need to append children contexts followed by the parent post-order context
        // at the tail of the queue and let the older content remain closer to the head of the queue 
        List<TraverseContext<T>> tail = (List<TraverseContext<T>>)impl;
        contexts
            .forEach(tail::add);
        parent
            .map(this::newPostOrderContext)
            .ifPresent(tail::add);
    }
}
