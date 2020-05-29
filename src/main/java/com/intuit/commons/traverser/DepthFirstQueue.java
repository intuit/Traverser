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

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 *
 * @author gkesler
 */
class DepthFirstQueue<T> extends AbstractTraverseContextQueue<T, Deque<TraverseContext<T>>> {
    public DepthFirstQueue(Traverser<? super T, ? super TraverseContext<T>> outer) {
        super(new LinkedList<>(), outer);
    }
    
    @Override
    public TraverseContext<T> pop() {
        return impl.pop();
    }

    @Override
    public TraverseContext<T> peek() {
        return impl.peek();
    }

    @Override
    public void pushAll(Optional<TraverseContext<? super T>> parent, Stream<TraverseContext<T>> contexts) {
        // let parent have children: {child1, child2, ..., childN}
        // let stack have state: {ctx1, ctx2, ctx3, ..., ctxN}
        // DFS algorithm implies that the most recent nodes are visited first
        // therefore we need to insert children contexts followed by the parent post-order context
        // at the head of the queue and push the older content further to the end of the queue  
        List<TraverseContext<T>> head = ((List<TraverseContext<T>>)impl).subList(0, 0);
        contexts
            .forEach(head::add);
        parent
            .map(this::newPostOrderContext)
            .ifPresent(head::add);
    }
}
