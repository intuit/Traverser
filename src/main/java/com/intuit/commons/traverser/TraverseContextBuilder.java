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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 *
 * @author gkesler
 * @param <T> type
 * @param <C> context
 * @param <B> builder
 */
public abstract class TraverseContextBuilder<T, C extends TraverseContext<T>, B extends TraverseContextBuilder<T, C, B>> 
        implements TraverseContext<T> {      
    protected TraverseContextBuilder(Traverser.ContextType contextType) {
        this.contextType = Objects.requireNonNull(contextType);
    }
    
    public B from (TraverseContext<T> other) {
        Objects.requireNonNull(other);
        
        return this
            .thisNode(other.thisNode())
            .parentContext(other.parentContext())
            .initialData(other.initialData())
            .vars(other.getContextVars());
    }
    
    @Override
    public T thisNode() {
        return node;
    }

    public B thisNode(T node) {
        this.node = node;
        return (B)this;
    }

    @Override
    public TraverseContext<T> parentContext() {
        return parentContext;
    }

    public B parentContext(TraverseContext<? super T> parentContext) {
        this.parentContext = (B)parentContext;        
        return (B)this;
    }

    @Override
    public <U> void setResult(U o) {
        contextStrategy.setResult(this, o);
    }

    @Override
    public <U> U getResult() {
        return contextStrategy.getResult(this);
    }

    @Override
    public <U> U getBackRefResult() {
        return (U)backRefResult
                .orElse(null);
    }    
    
    @Override
    public <U> U getContextResult() {
        return (U)result;
    }

    @Override
    public Object initialData() {
        return initialData;
    }
    
    public B initialData(Object initialData) {
        this.result = this.initialData = initialData;
        return (B)this;
    }

    @Override
    public Map<Class<?>, Object> getContextVars() {
        return vars;
    }

    public B vars(Map<Class<?>, ?> vars) {
        this.vars = Objects.requireNonNull((Map<Class<?>, Object>) vars);
        return (B)this;
    }

    @Override
    public <U> U getVar(Class<? super U> valueClass) {
        return contextStrategy.getVar(this, valueClass);
    }

    @Override
    public <U> U setVar(Class<? super U> valueClass, U newValue) {
        return contextStrategy.setVar(this, valueClass, newValue);
    }

    @Override
    public boolean isPostOrder() {
        return contextType == Traverser.ContextType.POST_ORDER;
    }

    @Override
    public boolean isBackRef(Function<? super TraverseContext<T>, ? extends Optional<Object>> visitedTracker) {
        this.backRefResult = Objects.requireNonNull(visitedTracker).apply(this);        
        return (Object)backRefResult != null;
    }
    
    public abstract C build (); 
    
    protected C build(Function<? super B, ? extends C> newContext) {           
        return Objects.requireNonNull(newContext)
                .compose(B::preConstruct)
                .<C>andThen(this::postConstruct)
                .apply((B)this);
    }
    
    protected B preConstruct () {  
        // mandatory pre-construct
        return this            
            .contextStrategy(
                Optional
                    .ofNullable(parentContext)
                    .map(o -> NORMAL_STRATEGY)
                    .orElse(ROOT_STRATEGY)
            )
            .vars(initVars(vars));        
    }
    
    protected C postConstruct (C result) {
        return result;
    }

    protected Map<Class<?>, Object> initVars (Map<Class<?>, Object> vars) {
        switch (contextType) {
            case PRE_ORDER:
                if (parentContext != null)
                    return new HashMap<>();
                // intentional fall through
            case POST_ORDER:
                return vars;
        };
        
        throw new IllegalStateException("Unknown contextType: " + contextType);
    }
    
    public B contextStrategy (ContextStrategy strategy) {
        this.contextStrategy = Objects.requireNonNull(strategy);
        return (B)this;
    }

    public interface ContextStrategy {
        <U> void setResult (TraverseContextBuilder<?, ?, ?> outer, U result);
        <U> U getResult (TraverseContextBuilder<?, ?, ?> outer);
        <U> U getVar (TraverseContextBuilder<?, ?, ?> outer, Class<? super U> key);
        <U> U setVar (TraverseContextBuilder<?, ?, ?> outer, Class<? super U> key, U newValue);
    }
    
    protected final Traverser.ContextType contextType;
    protected T node;
    protected B parentContext;
    protected Object result;
    protected Optional<Object> backRefResult;
    protected Object initialData;
    protected Map<Class<?>, Object> vars = Collections.emptyMap();
    protected ContextStrategy contextStrategy = ROOT_STRATEGY;
    
    private static final ContextStrategy ROOT_STRATEGY = new ContextStrategy() {
        @Override
        public <U> void setResult(TraverseContextBuilder<?, ?, ?> outer, U result) {
            outer.result = result;
        }

        @Override
        public <U> U getResult(TraverseContextBuilder<?, ?, ?> outer) {
            return (U)outer.result;
        }

        @Override
        public <U> U getVar(TraverseContextBuilder<?, ?, ?> outer, Class<? super U> key) {
            return (U)outer.vars.get(key);
        }

        @Override
        public <U> U setVar(TraverseContextBuilder<?, ?, ?> outer, Class<? super U> key, U newValue) {
            return (U)outer.vars.put(key, newValue);
        }
    };
    private static final ContextStrategy NORMAL_STRATEGY = new ContextStrategy() {
        @Override
        public <U> void setResult(TraverseContextBuilder<?, ?, ?> outer, U result) {
            outer.result = result;
            outer.parentContext.setResult(result);
        }

        @Override
        public <U> U getResult(TraverseContextBuilder<?, ?, ?> outer) {
            return outer.parentContext.getResult();
        }

        @Override
        public <U> U getVar(TraverseContextBuilder<?, ?, ?> outer, Class<? super U> key) {
            U value;
            return ((value = (U)outer.vars.get(key)) != null || outer.vars.containsKey(key))
                ? value
                : outer.parentContext.getVar(key);
        }

        @Override
        public <U> U setVar(TraverseContextBuilder<?, ?, ?> outer, Class<? super U> key, U newValue) {
            return outer.vars.containsKey(key)
                ? (U)outer.vars.put(key, newValue)
                : outer.parentContext.setVar(key, newValue);                
        }
    };
}
