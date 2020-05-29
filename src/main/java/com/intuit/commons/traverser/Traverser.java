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

import com.intuit.commons.TriConsumer;

import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Spliterators;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Generic purpose tree traverse mechanism.
 * 
 * This class greatly simplifies [possibly cyclic] graph traversal by
 * decoupling traversing the graph from the processing code executed when
 * a graph node is visited.
 * Unifies depth-first and bread-first traversal processes, so use of each of them
 * becomes a routine rather than challenge.
 * 
 * Example usage:
 * 
 * Let's assume we need to depth-first traverse a tree composed of nodes:
 * {@code 
 * class Node<T> {
 *    Node (T data) {
 *       this.data = data;
 *    }
 * 
 *    Node<T> child (Node<T> child) {
 *       children.add(child);
 *    }
 * 
 *    T data;
 *    Collection<Node<T>> children = new ArrayList<>();
 * }
 * }
 * in order to collect all their data into a list.
 * Depth First traversal could be performed as shown below:
 * {@code 
 * Node<String> root = new Node<String>("root")
 *    .child(new Node<String>("left")
 *       .child("left-left")
 *       .child("left-right"))
 *    .child(new Node<String>("right"));
 * 
 * Traverser<Node<T>> traverser = Traverser.depthFirst(node -> node.children);
 * List<T> allData = traverser.traverse(root, new ArrayList<>(), new Visitor<Node<T>>>() {
 *    Traverser.Action enter (Context<Node<T>> context) {
 *       Node<T> node = context.thisNode();
 *       List<T> allData = context.getResult();
 *       allData.add(node.data);
 *       return Traverser.Action.CONTINUE;
 *    }
 *    Traverser.Action leave (Context<Node<T>> context) {
 *       return Traverser.Action.CONTINUE;
 *    }
 *    Traverser.Action onBackRef (Context<Node<T>> context) {
 *       return Traverser.Action.CONTINUE;
 *    }
 * });
 * }
 * And that yields result:
 * {@code 
 * "root", "left", "left-left", "left-right", "right"
 * }
 * @param <T> graph node type
 * @param <C>
 * 
 * see @linkplain https://www.oodesign.com/visitor-pattern.html to learn about Visitor Pattern.
 * 
 * @author gkesler
 */
public final class Traverser<T, C extends TraverseContext<T>> {
    private Traverser (Function<? super Traverser<T, TraverseContext<T>>, ? extends TraverseContextQueue<T>> queueSupplier, 
                       BiFunction<? super Traverser<T, TraverseContext<T>>, ? super TraverseContext<T>, ? extends Stream<TraverseContext<T>>> childrenProvider,
                       Function<? super ContextType, ? extends TraverseContextBuilder<T, TraverseContext<T>, ?>> builderFactory) {
        this.contextQueueFactory = Objects.requireNonNull(queueSupplier);
        this.childrenProvider = Objects.requireNonNull(childrenProvider);
        this.contextBuilderFactory = Objects.requireNonNull(builderFactory);
    }    

    private Traverser (Function<? super Traverser<T, TraverseContext<T>>, ? extends TraverseContextQueue<T>> queueSupplier, 
                       BiFunction<? super Traverser<T, TraverseContext<T>>, ? super TraverseContext<T>, ? extends Stream<TraverseContext<T>>> childrenProvider) {
        this(queueSupplier, childrenProvider, Traverser::newBuilder);
    }
    
    private Traverser (Function<? super Traverser<T, TraverseContext<T>>, ? extends TraverseContextQueue<T>> queueSupplier, 
                       Function<T, Collection<T>> childrenProvider) {
        this(queueSupplier, adapt(childrenProvider));
    }
    
    private static <T> BiFunction<Traverser<T, TraverseContext<T>>, TraverseContext<T>, Stream<TraverseContext<T>>> adapt (Function<? super T, ? extends Collection<T>> func) {
        return (traverser, context) -> Objects.requireNonNull(func)
                    .apply(context.thisNode())
                    .stream()
                    .map(o -> traverser.newContext(context, o));
    }
    
    /**
     * Creates a Traverser suitable to perform DFS traversal.
     * 
     * @param <T> graph node type
     * 
     * @param childrenProvider function that obtains children to recurse down
     * @return Traverser instance configured to perform DFS traversal
     * @throws NullPointerException if {@code childrenProvider} is {@code null};
     */
    public static <T> Traverser<T, TraverseContext<T>> depthFirst (
            BiFunction<? super Traverser<T, TraverseContext<T>>, ? super TraverseContext<T>, ? extends Stream<TraverseContext<T>>> childrenProvider) {
        return new Traverser<>(DepthFirstQueue::new, (BiFunction<Traverser<T, TraverseContext<T>>, TraverseContext<T>, Stream<TraverseContext<T>>>)childrenProvider);
    }
    /**
     * Creates a Traverser suitable to perform DFS traversal.
     * 
     * @param <T> graph node type
     * @param <C> traverse context type
     * @param <B> builder
     * 
     * @param builderFactory   function that creates {@link TraverseContext} instances
     * @param childrenProvider function that obtains children to recurse down
     * @return Traverser instance configured to perform DFS traversal
     * @throws NullPointerException if {@code childrenProvider} is {@code null};
     */
    public static <T, C extends TraverseContext<T>, B extends TraverseContextBuilder<T, C, B>> Traverser<T, C> depthFirst (
            Function<? super ContextType, ? extends B> builderFactory,
            BiFunction<? super Traverser<T, C>, ? super C, ? extends Stream<C>> childrenProvider) {
        return depthFirst((BiFunction<? super Traverser<T, TraverseContext<T>>, ? super TraverseContext<T>, ? extends Stream<TraverseContext<T>>>)childrenProvider)
                .contextBuilderFactory(builderFactory);
    }
    /**
     * Creates a Traverser suitable to perform DFS traversal.
     * 
     * @param <T> graph node type
     * 
     * @param childrenProvider  function that obtains children to recurse down
     * @return Traverser instance configured to perform DFS traversal
     * @throws NullPointerException if {@code childrenProvider} is {@code null};
     */
    public static <T> Traverser<T, TraverseContext<T>> depthFirst (Function<? super T, ? extends Collection<T>> childrenProvider) {
        return depthFirst(adapt(childrenProvider));
    }    
    /**
     * Creates a Traverser suitable to perform BFS traversal.
     * 
     * @param <T> graph node type
     * 
     * @param childrenProvider  function that obtains children to recurse down
     * @return this instance configured to perform BFS traversal
     * @throws NullPointerException if {@code childrenProvider} is {@code null};
     */
    public static <T> Traverser<T, TraverseContext<T>> breadthFirst (
            BiFunction<? super Traverser<T, TraverseContext<T>>, ? super TraverseContext<T>, ? extends Stream<TraverseContext<T>>> childrenProvider) {
        return new Traverser<>(BreadthFirstQueue::new, (BiFunction<Traverser<T, TraverseContext<T>>, TraverseContext<T>, Stream<TraverseContext<T>>>)childrenProvider);
    }
    /**
    /**
     * Creates a Traverser suitable to perform BFS traversal.
     * 
     * @param <T> graph node type
     * @param <C> traverse context type
     * @param <B> builder
     * 
     * @param builderFactory   function that creates {@link TraverseContext} instances
     * @param childrenProvider function that obtains children to recurse down
     * @return Traverser instance configured to perform BFS traversal
     * @throws NullPointerException if {@code childrenProvider} is {@code null};
     */
    public static <T, C extends TraverseContext<T>, B extends TraverseContextBuilder<T, C, B>> Traverser<T, C> breadthFirst (
            Function<? super ContextType, ? extends B> builderFactory,
            BiFunction<? super Traverser<T, C>, ? super C, ? extends Stream<C>> childrenProvider) {
        return breadthFirst((BiFunction<? super Traverser<T, TraverseContext<T>>, ? super TraverseContext<T>, ? extends Stream<TraverseContext<T>>>)childrenProvider)
                .contextBuilderFactory(builderFactory);
    }
    /**
     * Creates a Traverser suitable to perform BFS traversal.
     * 
     * @param <T> graph node type
     * 
     * @param childrenProvider  {lambda expression that obtains children to recurse down}
     * @return this instance configured to perform BFS traversal
     * @throws NullPointerException if {@code childrenProvider} is {@code null};
     */
    public static <T> Traverser<T, TraverseContext<T>> breadthFirst (Function<? super T, ? extends Collection<T>> childrenProvider) {
        return breadthFirst(adapt(childrenProvider));
    }
    
    /**
     * Creates a new Traverser that uses alternative type of traverse context queue.
     * 
     * @param queueSupplier traverse context queue factory
     * @return a new instance of Traverser that uses provided traverse context queue factory.
     */
    public Traverser<T, C> contextQueueFactory (Function<? super Traverser<T, TraverseContext<T>>, ? extends TraverseContextQueue<T>> queueSupplier) {
        return new Traverser<>(queueSupplier, childrenProvider, contextBuilderFactory);
    }

    /**
     * Creates a new Traverser that uses alternative context builder
     * 
     * @param <U> target type of TraverseContext used by the new Traverser
     * 
     * @param builderFactory functor that creates a TraverseContextBuilder producing TraverseContext objects of type U 
     * @return a new Traverser instance that uses new type of TraverseContext
     */
    public <U extends TraverseContext<T>> Traverser<T, U> contextBuilderFactory (Function<? super ContextType, ? extends TraverseContextBuilder<T, U, ?>> builderFactory) {
        return new Traverser<>(contextQueueFactory, childrenProvider, (Function<? super ContextType, ? extends TraverseContextBuilder<T, TraverseContext<T>, ?>>)builderFactory);
    }
    /**
     * Traverses the graph starting from the only root.
     * 
     * @param <U>       type of result to accumulate during the traversal
     * @param root      element (node) in the graph to start traversing
     * @param seed      initial result value
     * @param visitor   GoF Visitor to be called when a graph element (node)
     * is being entered (pre-order) or left (post-order).
     * @return          accumulated result
     */    
    public <U> U traverse (T root, U seed, TraverseVisitor<? super T, ? super C> visitor) {
        return traverse(root, seed, Collections.emptyMap(), visitor);
    }
    /**
     * Traverses the graph starting from the only root.
     * 
     * @param <U>       type of result to accumulate during the traversal
     * @param root      element (node) in the graph to start traversing
     * @param seed      initial result value
     * @param vars      a map of variables with their names to be used while traversing
     * @param visitor   GoF Visitor to be called when a graph element (node)
     * is being entered (pre-order) or left (post-order).
     * @return          accumulated result
     */    
    public <U> U traverse (T root, U seed, Map<Class<?>, ?> vars, TraverseVisitor<? super T, ? super C> visitor) {
        return traverse(root, seed, vars, new IdentityHashMap<>(), visitor);
    }
    /**
     * Traverses the graph starting from the only root.
     * 
     * @param <U>       type of result to accumulate during the traversal
     * @param root      element (node) in the graph to start traversing
     * @param seed      initial result value
     * @param vars      a map of variables with their names to be used while traversing
     * @param visitedMap a map to keep track of visited nodes and the result at the moment of recording
     * @param visitor   GoF Visitor to be called when a graph element (node)
     * is being entered (pre-order) or left (post-order).
     * @return          accumulated result
     */    
    public <U> U traverse (T root, U seed, Map<Class<?>, ?> vars, Map<? super T, ? super Object> visitedMap, TraverseVisitor<? super T, ? super C> visitor) {
        return traverse(root, seed, vars, newVisitTracker(visitedMap), visitor);
    }
    /**
     * Traverses the graph starting from the only root.
     * 
     * @param <U>       type of result to accumulate during the traversal
     * @param root      element (node) in the graph to start traversing
     * @param seed      initial result value
     * @param vars      a map of variables with their names to be used while traversing
     * @param visitTracker a functor to lookup visited node
     * @param visitor   GoF Visitor to be called when a graph element (node)
     * is being entered (pre-order) or left (post-order).
     * @return          accumulated result
     */    
    public <U> U traverse (T root, U seed, Map<Class<?>, ?> vars, Function<? super TraverseContext<T>, ? extends Optional<Object>> visitTracker, TraverseVisitor<? super T, ? super C> visitor) {
        return traverse(Collections.singleton(root), seed, vars, visitTracker, visitor);
    }
    /**
     * Traverses the graph starting from multiple roots.
     * 
     * @param <U>       type of result to accumulate during the traversal
     * @param roots     elements (nodes) in the graph to start traversing
     * @param seed      initial result value
     * @param visitor   GoF Visitor to be called when a graph element (node)
     * is being entered (pre-order) or left (post-order).
     * @return          accumulated result
     */    
    public <U> U traverse (Iterable<? super T> roots, U seed, TraverseVisitor<? super T, ? super C> visitor) {
        return traverse(roots, seed, Collections.emptyMap(), visitor);
    }
    
    /**
     * Traverses the graph starting from multiple roots.
     * 
     * @param <U>       type of result to accumulate during the traversal
     * @param roots     elements (nodes) in the graph to start traversing
     * @param seed      initial result value
     * @param vars      a map of variables with their names to be used while traversing
     * @param visitor   GoF Visitor to be called when a graph element (node)
     * is being entered (pre-order) or left (post-order).
     * @return          accumulated result
     */
    public <U> U traverse (Iterable<? super T> roots, U seed, Map<Class<?>, ?> vars, TraverseVisitor<? super T, ? super C> visitor) {
        return traverse(roots, seed, vars, new IdentityHashMap<>(), visitor);
    }
    
    /**
     * Traverses the graph starting from multiple roots.
     * 
     * @param <U>       type of result to accumulate during the traversal
     * @param roots     elements (nodes) in the graph to start traversing
     * @param seed      initial result value
     * @param vars      a map of variables with their names to be used while traversing
     * @param visitedMap a map to keep track of visited nodes and the result at the moment of recording
     * @param visitor   GoF Visitor to be called when a graph element (node)
     * is being entered (pre-order) or left (post-order).
     * @return          accumulated result
     */
    public <U> U traverse (Iterable<? super T> roots, U seed, Map<Class<?>, ?> vars, Map<? super T, ? super Object> visitedMap, TraverseVisitor<? super T, ? super C> visitor) {
        return traverse(roots, seed, vars, newVisitTracker(visitedMap), visitor);
    }
    
    /**
     * Traverses the graph starting from multiple roots.
     * 
     * @param <U>       type of result to accumulate during the traversal
     * @param roots     elements (nodes) in the graph to start traversing
     * @param seed      initial result value
     * @param vars      a map of variables with their names to be used while traversing
     * @param visitTracker a functor to lookup visited node
     * @param visitor   GoF Visitor to be called when a graph element (node)
     * is being entered (pre-order) or left (post-order).
     * @return          accumulated result
     */    
    public <U> U traverse (Iterable<? super T> roots, U seed, Map<Class<?>, ?> vars, Function<? super TraverseContext<T>, ? extends Optional<Object>> visitTracker, TraverseVisitor<? super T, ? super C> visitor) {
        Objects.requireNonNull(roots);
        Objects.requireNonNull(vars);
        Objects.requireNonNull(visitTracker);
        Objects.requireNonNull(visitor);
        
        C rootContext = newRootContext(seed, vars);
        TraverseContextQueue<T> contextQueue = newContextQueue(rootContext, (Iterable<T>)roots);
        
        Action action = Action.CONTINUE;
        while (!contextQueue.isDone(action)) {
            action = traverseOne(contextQueue, visitTracker, visitor);
        }
        
        return (U)Optional
                .ofNullable(contextQueue.peek())
                .orElse(rootContext)
                .getResult();
    }
    
    private Action traverseOne (TraverseContextQueue<T> contextQueue, Function<? super TraverseContext<T>, ? extends Optional<Object>> visitTracker, TraverseVisitor<? super T, ? super C> visitor) {
        C context = pop(contextQueue);
        
        if (context.isPostOrder()) {
            return visitor
                .leave(context);
        } else if (context.isBackRef(visitTracker)) {
            return visitor
                .onBackRef(context);
        } else {
            return visitor
                .enter(context)
                .prepareNext(this, contextQueue, context);
        }
    }

    /**
     * Creates a new Iterator for the currently configured traversal type
     * 
     * @param roots         root objects to begin traversal with
     * @param visitor       a Visitor object that helps to select nodes to return in {@link java.util.Iterator#next() } call
     * @return new iterator instance
     */
    public TraversingIterator<T> newIterator (Iterable<? super T> roots, TraverseVisitor<? super T, ? super C> visitor) {
        return newIterator(roots, newVisitTracker(new IdentityHashMap<>()), visitor);
    }
    
    /**
     * Creates a new Iterator for the currently configured traversal type
     * 
     * @param roots         root objects to begin traversal with
     * @param visitedMap a map to keep track of visited nodes and the result at the moment of recording
     * @param visitor       a Visitor object that helps to select nodes to return in {@link java.util.Iterator#next() } call
     * @return new iterator instance
     */
    public TraversingIterator<T> newIterator (Iterable<? super T> roots, Map<? super T, ? super Object> visitedMap, TraverseVisitor<? super T, ? super C> visitor) {
        return newIterator(roots, newVisitTracker(visitedMap), visitor);
    }
    
    /**
     * Creates a new Iterator for the currently configured traversal type
     * 
     * @param roots         root objects to begin traversal with
     * @param visitTracker a functor to lookup visited node
     * @param visitor       a Visitor object that helps to select nodes to return in {@link java.util.Iterator#next() } call
     * @return new iterator instance
     */
    public TraversingIterator<T> newIterator (Iterable<? super T> roots, Function<? super TraverseContext<T>, ? extends Optional<Object>> visitTracker, TraverseVisitor<? super T, ? super C> visitor) {
        Objects.requireNonNull(roots);
        Objects.requireNonNull(visitTracker);
        Objects.requireNonNull(visitor);
        
        C rootContext = newRootContext(null, Collections.emptyMap());
        TraverseContextQueue<T> contextQueue = newContextQueue(rootContext, (Iterable<T>)roots);
        
        return new TraversingIterator<T>() {
            @Override
            public <E> Stream<E> path(Function<? super TraverseContext<T>, E> func) {
                Objects.requireNonNull(func);
                
                return last
                    .orElseThrow(() -> new IllegalStateException("no results"))
                    .parentsStream()
                    .filter(context -> context.thisNode() != null)
                    .map(func);
            }

            Optional<C> getNext(Optional<C> next) {
                Action action = Action.CONTINUE;                
                while (!(next.isPresent() || contextQueue.isDone(action))) {
                    TraverseContext<T> current = contextQueue.peek();
                    action = traverseOne(contextQueue, visitTracker, visitor);                    
                    next = Optional.ofNullable(current.getResult());
                }
                
                return next;
            }

            @Override
            public boolean hasNext() {
                return (next = next
                            .map(o -> next)
                            .orElseGet(() -> getNext(next)))
                        .isPresent();
            }

            @Override
            public T next() {
                T result = (last = next)
                    .flatMap(ctx -> Optional.ofNullable(ctx.thisNode()))
                    .orElseThrow(() -> new NoSuchElementException("no more results"));
                
                next = last
                    .map(ctx -> ctx.result(null))
                    .flatMap(ctx -> Optional.empty());                
                return result;
            }

            @Override
            public <R> R replace(Object newValue, BiFunction<? super TraverseContext<T>, ? super Object, ? extends R> replaceFunction) {
                return Objects.requireNonNull(replaceFunction).apply(last.orElseThrow(() -> new IllegalArgumentException("no results")), newValue);
            }
            
            Optional<C> next = Optional.empty();
            Optional<C> last = next;
        };
    }
    /**
     * Creates a pre-order iterator that iterates over the graph elements <b>before</b>
     * recursing to their children.
     * 
     * @param roots a collection of graph elements to start traversing
     * @param delegate a visitor to be called when an element is being entered
     * @return new TraversingIterator suitable for pre-order graph traversal.
     */
    public TraversingIterator<T> preOrderIterator (Iterable<? super T> roots, TraverseVisitor<? super T, ? super C> delegate) {
        Objects.requireNonNull(delegate);
        
        return newIterator(roots, new TraverseVisitor.Builder<T, C>()
            .onEnter(context -> ((TraverseVisitor<T, C>)delegate).enter((C)context.result(context)))
            .build());
    }
    /**
     * Creates a pre-order iterator that iterates over the graph elements <b>before</b>
     * recursing to their children.
     * 
     * @param root a graph elements to start traversing
     * @param delegate a visitor to be called when an element is being entered
     * @return new TraversingIterator suitable for pre-order graph traversal.
     */
    public TraversingIterator<T> preOrderIterator (T root, TraverseVisitor<? super T, ? super C> delegate) {
        return preOrderIterator(Collections.singleton(root), delegate);
    }
    /**
     * Creates a pre-order iterator that iterates over the graph elements <b>before</b>
     * recursing to their children.
     * 
     * @param roots a collection of graph elements to start traversing
     * @return new TraversingIterator suitable for pre-order graph traversal.
     */
    public TraversingIterator<T> preOrderIterator (Iterable<? super T> roots) {
        return preOrderIterator(roots, (TraverseVisitor<T, C>)TraverseVisitor.NOOP_VISITOR);
    }
    /**
     * Creates a pre-order iterator that iterates over the graph elements <b>before</b>
     * recursing to their children.
     * 
     * @param root a graph element to start traversing
     * @return new TraversingIterator suitable for pre-order graph traversal.
     */
    public TraversingIterator<T> preOrderIterator (T root) {
        return preOrderIterator(Collections.singleton(root));
    }
    /**
     * Creates a post-order iterator that iterates over the graph elements <b>after</b>
     * recursing to their children.
     * 
     * @param roots a collection of graph elements to start traversing
     * @param delegate a visitor to be called when an element is being entered
     * @return new TraversingIterator suitable for post-order graph traversal.
     */
    public TraversingIterator<T> postOrderIterator (Iterable<? super T> roots, TraverseVisitor<? super T, ? super C> delegate) {
        Objects.requireNonNull(delegate);
        
        return newIterator(roots, new TraverseVisitor.Builder<T, C>()
            .onLeave(context -> ((TraverseVisitor<T, C>)delegate).enter((C)context.result(context)))
            .build());
    }
    /**
     * Creates a post-order iterator that iterates over the graph elements <b>after</b>
     * recursing to their children.
     * 
     * @param root a graph element to start traversing
     * @param delegate a visitor to be called when an element is being entered
     * @return new TraversingIterator suitable for post-order graph traversal.
     */
    public TraversingIterator<T> postOrderIterator (T root, TraverseVisitor<? super T, ? super C> delegate) {
        return postOrderIterator(Collections.singleton(root), delegate);
    }
    /**
     * Creates a post-order iterator that iterates over the graph elements <b>after</b>
     * recursing to their children.
     * 
     * @param roots a collection of graph elements to start traversing
     * @return new TraversingIterator suitable for post-order graph traversal.
     */
    public TraversingIterator<T> postOrderIterator (Iterable<? super T> roots) {
        return postOrderIterator(roots, (TraverseVisitor<T, C>)TraverseVisitor.NOOP_VISITOR);
    }
    /**
     * Creates a post-order iterator that iterates over the graph elements <b>after</b>
     * recursing to their children.
     * 
     * @param root graph elements to start traversing
     * @return new TraversingIterator suitable for post-order graph traversal.
     */
    public TraversingIterator<T> postOrderIterator (T root) {
        return postOrderIterator(Collections.singleton(root));
    }
    
    private C pop (TraverseContextQueue<T> queue) {
        return (C)queue.pop();
    }
    
    private void pushChildren (TraverseContextQueue<T> queue, C parent) {
        enqueWithChildren(parent, childrenProvider, (factory) -> queue, Optional.of(parent));
    }
    
    private <U> TraverseContextQueue<T> newContextQueue (C rootContext, Iterable<T> roots) {
        return enqueWithChildren(rootContext, 
            (builder, context) -> StreamSupport
                .stream(Spliterators.spliteratorUnknownSize(roots.iterator(), 0), false)
                .map(o -> builder.newContext(context, o)), 
            contextQueueFactory, 
            Optional.empty());
    }

    private TraverseContextQueue<T> enqueWithChildren (C parent, 
            BiFunction<? super Traverser<T, TraverseContext<T>>, ? super TraverseContext<T>, ? extends Stream<TraverseContext<T>>> childrenProvider, 
            Function<? super Traverser<T, TraverseContext<T>>, ? extends TraverseContextQueue<T>> queueFactory, 
            Optional<TraverseContext<? super T>> listParent) {
        TraverseContextQueue<T> contextQueue = queueFactory.apply((Traverser<T, TraverseContext<T>>)this);
        
        contextQueue.pushAll(
            listParent, 
            childrenProvider.apply((Traverser<T, TraverseContext<T>>)this, parent)
        );
        
        return contextQueue;
    }
    
    private static <T> Function<TraverseContext<T>, Optional<Object>> newVisitTracker (Map<? super T, ? super Object> visitedMap) {
        return context -> (Optional<Object>)Optional
            .ofNullable(context.thisNode())
            .map(node -> lookupBackRefResult(visitedMap, node, context.getResult()))
            .orElse(null);
    }

    private static <T> Object lookupBackRefResult (Map<? super T, ? super Object> visitedMap, T node, Object result) {
        return visitedMap.containsKey(node) 
            ? Optional.ofNullable(visitedMap.get(node))
            : visitedMap.put(node, result);
    }
    
    private C newRootContext (Object seed, Map<Class<?>, ?> vars) {
        return newPreOrderContext(null, null, seed, vars);
    }
    
    public C newContext (TraverseContext<? super T> parent, T child) {
        return newPreOrderContext(Objects.requireNonNull(parent), child, parent.initialData(), parent.getContextVars());
    }
    
    protected C newPreOrderContext (TraverseContext<? super T> context, T node, Object initialData, Map<Class<?>, ?> vars) {
        C parent = (C)context;
        
        return (C)contextBuilderFactory.apply(ContextType.PRE_ORDER)
            .thisNode(node)
            .parentContext(parent)
            .initialData(initialData)
            .vars(vars)
            .build();                
    }

    protected C newPostOrderContext (TraverseContext<? super T> context) {
        C preOrder = (C)Objects.requireNonNull(context);
        
        return (C)contextBuilderFactory.apply(ContextType.POST_ORDER)
            .from(preOrder)
            .build();                    
    }
    
    private static class ContextBuilder<T> 
            extends TraverseContextBuilder<T, TraverseContext<T>, ContextBuilder<T>> {
        public ContextBuilder(ContextType contextType) {
            super(contextType);
        }

        @Override
        public TraverseContext<T> build() {
            return build(UnaryOperator.identity());
        }
    }
    
    private static <T> TraverseContextBuilder<T, TraverseContext<T>, ?> newBuilder (ContextType contextType) {
        return new ContextBuilder<>(contextType);
    }
        
    /**
     * Enumerates actions a client visitor can return from its functions to
     * control overarching traversal loop.
     */
    public enum Action {
        /**
         * indicates Visitor's desire to continue current traversal
         */
        CONTINUE(Traverser::pushChildren), 
        /**
         * indicates Visitor's desire to skip traversing children of the current element
         */
        SKIP(Action::noop), 
        /**
         * indicates Visitor's desire to stop traversing
         */
        QUIT(Action::noop);
        
        private <T, C extends TraverseContext<T>> Action (TriConsumer<? super Traverser<T, C>, ? super TraverseContextQueue<T>, ? super C> delegate) {
            this.delegate = Objects.requireNonNull((TriConsumer<Traverser<?, ?>, TraverseContextQueue<?>, TraverseContext<?>>)delegate);
        }
        
        private <T, C extends TraverseContext<T>> Action prepareNext (Traverser<T, C> outer, TraverseContextQueue<? super T> contextQueue, C context) {
            delegate.accept(outer, contextQueue, context);
            return this;
        }
        
        private static <T, C extends TraverseContext<T>> void noop (Traverser<T, C> outer, TraverseContextQueue<? super Object> state, C parent) {
        }
        
        private final TriConsumer<Traverser<?, ?>, TraverseContextQueue<?>, TraverseContext<?>> delegate;
    }
    
    /**
     * Enumerates possibly context types to be created by the ContextFactory
     */
    public enum ContextType {
        /**
         * Indicates the requested Context MUST be a regular Context
         */
        PRE_ORDER,
        /**
         * Indicates the requested Context MUST be an end-of-list Context
         */
        POST_ORDER
    }
    
    private final Function<? super Traverser<T, TraverseContext<T>>, ? extends TraverseContextQueue<T>> contextQueueFactory;
    private final BiFunction<? super Traverser<T, TraverseContext<T>>, ? super TraverseContext<T>, ? extends Stream<TraverseContext<T>>> childrenProvider;    
    private final Function<? super ContextType, ? extends TraverseContextBuilder<T, TraverseContext<T>, ?>> contextBuilderFactory;
}
