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
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Spliterators;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Encapsulates current state of traversal.
 * Context wraps the current graph element being entered (pre-order) or left (post-order)
 * along with the convenient methods to navigate through the parent contexts all the way
 * to the root/source graph node, store traversal result, maintain traversal internal variables and more.
 * 
 * @param <T> type of the graph node being wrapped
 * 
 * @author gkesler
 */
public interface TraverseContext<T> {
    /**
     * Returns the graph node associated with this Context instance
     * 
     * @return wrapped graph node
     */
    T thisNode();
    /**
     * Returns previous context. 
     * Contexts are organized as a linked lists that start from the Context for 
     * wrapped the graph element used to start traversing all the way down to the 
     * current Context.
     * 
     * @return parent Context
     */
    TraverseContext<T> parentContext();
    /**
     * A shortcut to obtain result of immediate parent.
     * For performance and usability reasons, traverser guarantees that for every 
     * Context being traversed there always be a parent, so null check for parent Context is not needed.
     * 
     * @param <U> expected type of result
     * 
     * @return parent result
     */
    default<U> U getParentResult() {
        return parentContext().getResult();
    }
    /**
     * A shortcut to set result of immediate parent.
     * For performance and usability reasons, traverser guarantees that for every 
     * Context being traversed there always be a parent, so null check for parent Context is not needed.
     * 
     * @param <U> expected type of result
     * @param o parent result value
     */
    default<U> void setParentResult(U o) {
        parentContext().setResult(o);
    }
    /**
     * Sets result for the current context.
     * Depending on the Context implementation, results could be shared among all 
     * Contexts in a single traversal or they could be stored per each Context/graph element
     * 
     * Default: all Contexts share the same result {@link TraverseContextBuilder#ROOT_STRATEGY}.
     * 
     * @see TraverseContextBuilder.ContextStrategy
     * 
     * @param <U> expected type of result
     * @param o result value
     */
    <U> void setResult(U o);
    /**
     * Obtains result of this Context
     * 
     * @param <U> expected type of result
     * @return result value
     */
    <U> U getResult();
    /**
     * Retrieves local result stored in this context.
     * Difference between this method and {@link #getResult() } is that the latter
     * is not guaranteed to return locally stored result. It rather represents the
     * overall centralized result acquired during entire graph traversal.
     * 
     * @param <U> expected type of result
     * @return result value
     */
    <U> U getContextResult ();
    /**
     * Obtains initial data provided when traversal was requested.
     * Serves as the default result value in case it wasn't overwritten
     * during traversal.
     * 
     * @return initial result value provided by the client
     */
    Object initialData();
    /**
     * Context local variables used during traversal.
     * Some visitors can declare their own local variables, while others can
     * leverage this common variables map.
     * 
     * @return variables map.
     */
    Map<Class<?>, Object> getContextVars();
    /**
     * A fluent API method to set parent result
     * 
     * @param <U> expected type of result
     * @param o parent result value
     * @return this instance to allow method chaining
     */
    default<U> TraverseContext<T> parentResult(U o) {
        setParentResult(o);
        return this;
    }
    /**
     * A fluent API method to set context result
     * 
     * @param <U> expected type of result
     * @param o result to save in context
     * @return this instance to allow method chaining
     */
    default<U> TraverseContext<T> result(U o) {
        setResult(o);
        return this;
    }
    /**
     * Shortcut to read a variable associated with the provided key
     * 
     * @param <U> expected type of result
     * @param valueClass used as a key to access the variable
     * @return variable value
     */
    default <U> U getVar (Class<? super U> valueClass) {
        return (U)getContextVars().get(valueClass);
    }
    /**
     * Shortcut to associate a variable the provided key
     * 
     * @param <U> expected type of result
     * @param valueClass used as a key to access the variable
     * @param value variable value
     * @return this instance to allow method chaining
     */
    default<U> U setVar(Class<? super U> valueClass, U value) {
        return (U)getContextVars().put(valueClass, value);
    }
    /**
     * A fluent API method to set a variable value
     * 
     * @param <U>  type of a variable
     * @param valueClass implementing class
     * @param value value
     * @return current context
     */
    default <U> TraverseContext<T> var (Class<? super U> valueClass, U value) {
        setVar(valueClass, value);
        return this;
    }
    /**
     * Checks if this context wraps one of the roots passed to traverse method
     * 
     * @see Traverser
     * 
     * @return {@code true} in case of root context
     */
    default boolean isRoot () {
        return !Optional
            .ofNullable(parentContext())
            .flatMap(ctx -> Optional.ofNullable(ctx.thisNode()))
            .isPresent();
    }
    /**
     * Indicates that this Context is accessed <b>after</b> all its immediate children
     * have been visited. This allows for post-order operations on the associated
     * graph-element.
     * 
     * @return {@code true} if the Context is a post-order context
     */
    boolean isPostOrder();
    /**
     * Indicates that this Context wraps a graph node that had been already seen
     * during traversal process. This allows the traversal to prevent infinite
     * cyclic traversals and inform the client code about detected cycle.
     * 
     * @param visitedTracker lookup method to check if this context has been already seen
     * @return {@code true} is the graph node had been already seen
     */
    boolean isBackRef(Function<? super TraverseContext<T>, ? extends Optional<Object>> visitedTracker);
    /**
     * Obtains result value associated with the first occurrence of the graph node.
     * @param <U> expected type of result
     * @return back reference result value
     */
    <U> U getBackRefResult();  
    /**
     * Convenience method to iterate over the parents chain of this Context
     * 
     * @return iterator to iterate over the parents list starting from this object
     */
    default Iterator<TraverseContext<T>> parentsIterator () {
        return new Iterator<TraverseContext<T>>() {
            @Override
            public boolean hasNext() {
                return current != null;
            }

            @Override
            public TraverseContext<T> next() {
                if (current == null)
                    throw new NoSuchElementException("no more parents");
                
                TraverseContext<T> result = current;
                current = current.parentContext();
                return result;
            }
            
            TraverseContext<T> current = TraverseContext.this;
        };
    }
    /**
     * Convenience method to treat the parents of this context as a stream/sequence
     * of Context objects with all goodies from Stream API
     * 
     * @return stream of parent contexts starting from this object
     */
    default Stream<TraverseContext<T>> parentsStream () {
        return StreamSupport
           .stream(Spliterators.spliteratorUnknownSize(parentsIterator(), 0), false);
    }
    /**
     * Utility method for supporting type-safety
     * @param <U> a target type
     * @param castTo an implementing class
     * @return the object after casting into target type
     *
     * @throws ClassCastException  if object is not assignable to the type U
     */
    @SuppressWarnings("unchecked")
    default <U extends TraverseContext<T>> U as (Class<? super U> castTo) {
        return (U)Objects.requireNonNull(castTo).cast(this);
    }
}
