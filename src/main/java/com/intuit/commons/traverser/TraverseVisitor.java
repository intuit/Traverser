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

import com.intuit.commons.Comparables;

import java.util.Objects;
import java.util.function.Function;

/**
 * GoF Visitor @see <a href="https://www.oodesign.com/visitor-pattern.html">visitor-pattern</a> that
 * allows the Traverser to execute clients code during graph traversal.
 * 
 * @param <T> type of nodes in the graph
 * 
 * @author gkesler
 */
public interface TraverseVisitor<T, C extends TraverseContext<T>> {
    /**
     * Signals the Visitor that a new graph node is being visited (pre-order)
     * 
     * @param context traversal context around graph node
     * @return signal to Traverser on the next traversal step.
     * 
     * @see Traverser.Action
     */
    Traverser.Action enter(C context);
    /**
     * Signals the Visitor that a graph node is being exited (post-order)
     * 
     * @param context traversal context around graph node
     * @return signal to Traverser on the next traversal step.
     * 
     * @see Traverser.Action
     */
    Traverser.Action leave(C context);
    /**
     * Signals the Visitor that an already visited graph node is being visited again (cycle prevention)
     * 
     * @param context traversal context around graph node
     * @return signal to Traverser on the next traversal step.
     * 
     * @see Traverser.Action
     */
    Traverser.Action onBackRef(C context);    
    
    /**
     *  Returns new builder
     * @param <T> type
     * @param <C> context
     * @return  builder with visitor, which does nothing
     */
    static <T, C extends TraverseContext<T>> Builder<T, C> newBuilder () {
        return new Builder<>();
    }
    
    /**
     * Creates new builder based on current visitors actions
     * @param <T>type
     * @param <C> context
     * @param visitor a visitor
     * @return new builder
     */
    static <T, C extends TraverseContext<T>> Builder<T, C> of (TraverseVisitor<? super T, ? super C> visitor) {
        return new Builder<T, C>(visitor);
    }
    
    /**
     * Chain visitor invocation after current one
     * @param visitor visitor
     * @return returns new visitor
     */
    default TraverseVisitor<T, C> andThen (TraverseVisitor<? super T, ? super C> visitor) {
        return of(this)
                .thenApply(visitor)
                .build();
    }
    
    /**
     * Builds new visitor based on current
     * @param visitor visitor to combine visiting with
     * @return new visitor
     */
    default TraverseVisitor<T, C> compose (TraverseVisitor<? super T, ? super C> visitor) {
        return of(this)
                .compose(visitor)
                .build();
    }
    
    /**
     * 
     * @param <T> type
     * @param <C> context
     */
    static class Builder<T, C extends TraverseContext<T>> {
        private Builder (Function<? super C, ? extends Traverser.Action> onEnter,
                Function<? super C, ? extends Traverser.Action> onLeave,
                Function<? super C, ? extends Traverser.Action> onBackRef) {
            this.onEnter = Objects.requireNonNull((Function<C, Traverser.Action>)onEnter);
            this.onLeave = Objects.requireNonNull((Function<C, Traverser.Action>)onLeave);
            this.onBackRef = Objects.requireNonNull((Function<C, Traverser.Action>)onBackRef);
        }
        
        public Builder () {
            this(noop(), noop(), noop());
        }
        
        public Builder (TraverseVisitor<? super T, ? super C> visitor) {
            this(Objects.requireNonNull(visitor)::enter, visitor::leave, visitor::onBackRef);
        }
        
        public Builder<T, C> onEnter (Function<? super C, ? extends Traverser.Action> func) {
            this.onEnter = Objects.requireNonNull((Function<C, Traverser.Action>)func);
            return this;
        }
        
        public Builder<T, C> onLeave (Function<? super C, ? extends Traverser.Action> func) {
            this.onLeave = Objects.requireNonNull((Function<C, Traverser.Action>)func);
            return this;
        }
        
        public Builder<T, C> onBackRef (Function<? super C, ? extends Traverser.Action> func) {
            this.onBackRef = Objects.requireNonNull((Function<C, Traverser.Action>)func);
            return this;
        }
        
        public Builder<T, C> thenApply (TraverseVisitor<? super T, ? super C> visitor) {
            Objects.requireNonNull(visitor);
            
            return this
                .onEnter(combine(onEnter, visitor::enter))
                .onLeave(combine(onLeave, visitor::leave))
                .onBackRef(combine(onBackRef, visitor::onBackRef));
        }
        
        public Builder<T, C> compose (TraverseVisitor<? super T, ? super C> visitor) {
            Objects.requireNonNull(visitor);
            
            return this
                .onEnter(combine(visitor::enter, onEnter))
                .onLeave(combine(visitor::leave, onLeave))
                .onBackRef(combine(visitor::onBackRef, onBackRef));
        }
        
        private <U> Function<U, Traverser.Action> combine (Function<U, Traverser.Action> first, Function<U, Traverser.Action> next) {
            return u -> Comparables.max(first.apply(u), next.apply(u));
        }
        
        public TraverseVisitor<T, C> build () {
            return new TraverseVisitor<T, C>() {
                @Override
                public Traverser.Action enter(C context) {
                    return onEnter.apply(context);
                }

                @Override
                public Traverser.Action leave(C context) {
                    return onLeave.apply(context);
                }

                @Override
                public Traverser.Action onBackRef(C context) {
                    return onBackRef.apply(context);
                }
            };
        }

        private static <T, C extends TraverseContext<T>> Function<C, Traverser.Action> noop () {
            return (Function<C, Traverser.Action>)NOOP_FUNC;
        }
        
        private Function<C, Traverser.Action> onEnter;
        private Function<C, Traverser.Action> onLeave;
        private Function<C, Traverser.Action> onBackRef;
    }
    
    static final Function<? super TraverseContext<Object>, ? extends Traverser.Action> NOOP_FUNC = 
            c -> Traverser.Action.CONTINUE;
    static final TraverseVisitor<?, ?> NOOP_VISITOR = new TraverseVisitorStub<>();
}
