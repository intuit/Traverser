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

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 *
 * @author gkesler
 */
public class TraverserTest {

    public TraverserTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }
    
    private interface TestVisitor<T> extends TraverseVisitor<T, TraverseContext<T>> {
    }
    
    static class Node<T> {
        T getData () {
            return data;
        }
        
        List<Node<T>> getChildren () {
            return children;
        }
        
        Node<T> data (T data) {
            this.data = data;
            return this;
        }
        
        Node<T> children (List<Node<T>> children) {
            this.children = children;
            return this;
        }
        
        Node<T> child (Node<T> child) {
            children.add(child);
            return this;
        }
        
        T data;
        List<Node<T>> children = new ArrayList<>();
    }
    
    @Test
    public void testSingleDepthFirst () {
        Node<String> root = new Node<String>()
            .data("Hello, world");                
        
        String result = Traverser
            .<Node<String>>depthFirst(Node::getChildren)
            .traverse(root, null, new TestVisitor<Node<String>>() {
                @Override
                public Traverser.Action enter(TraverseContext<Node<String>> context) {
                    context.setResult(context.thisNode().getData());
                    return Traverser.Action.CONTINUE;
                }

                @Override
                public Traverser.Action leave(TraverseContext<Node<String>> context) {
                    return Traverser.Action.CONTINUE;
                }

                @Override
                public Traverser.Action onBackRef(TraverseContext<Node<String>> context) {
                    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                }
            });
    
        assertEquals(result, root.getData());
    }
    
    @Test
    public void testTreeDepthFirst () {
        Node<String> root = new Node<String>()
            .data("root")
            .child(new Node<String>()
                .data("left"))
            .child(new Node<String>()
                .data("right"));                
        
        String result = Traverser
            .<Node<String>>depthFirst(Node::getChildren)
            .traverse(root, null, new TestVisitor<Node<String>>() {
                @Override
                public Traverser.Action enter(TraverseContext<Node<String>> context) {
                    context.setResult(context.thisNode().getData());
                    return Traverser.Action.CONTINUE;
                }

                @Override
                public Traverser.Action leave(TraverseContext<Node<String>> context) {
                    return Traverser.Action.CONTINUE;
                }

                @Override
                public Traverser.Action onBackRef(TraverseContext<Node<String>> context) {
                    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                }
            });
    
        assertEquals("right", result);
    }    
    
    @Test
    public void testTreeWithCycleDepthFirst () {
        Node<String> root = new Node<String>()
            .data("root");
        
        Node<String> left, right;
        root.child(left = new Node<String>()
                .data("left")
                .child(root))
            .child(right = new Node<String>()
                .data("right")
                .child(left));                
        
        List<String> result = Traverser
            .<Node<String>>depthFirst(Node::getChildren)
            .traverse(root, new ArrayList<>(), new TestVisitor<Node<String>>() {
                @Override
                public Traverser.Action enter(TraverseContext<Node<String>> context) {
                    List<String> result = context.getResult();
                    result.add(context.thisNode().getData());
                    return Traverser.Action.CONTINUE;
                }

                @Override
                public Traverser.Action leave(TraverseContext<Node<String>> context) {
                    return Traverser.Action.CONTINUE;
                }

                @Override
                public Traverser.Action onBackRef(TraverseContext<Node<String>> context) {
                    List<String> result = context.getResult();
                    result.add(context.thisNode().getData());
                    return Traverser.Action.CONTINUE;
                }
            });
    
        assertEquals(Arrays.asList("root", "left", "root", "right", "left"), result);
    }    
    
    @Test
    public void testQuitAction () {
        Node<String> root = new Node<String>()
            .data("root");
        
        Node<String> left, right;
        root.child(left = new Node<String>()
                .data("left")
                .child(root))
            .child(right = new Node<String>()
                .data("right")
                .child(left));                
        
        List<String> result = Traverser
            .<Node<String>>depthFirst(Node::getChildren)
            .traverse(root, new ArrayList<>(), new TestVisitor<Node<String>>() {
                @Override
                public Traverser.Action enter(TraverseContext<Node<String>> context) {
                    List<String> result = context.getResult();
                    result.add(context.thisNode().getData());
                    return result.contains("left")
                        ? Traverser.Action.QUIT
                        : Traverser.Action.CONTINUE;
                }

                @Override
                public Traverser.Action leave(TraverseContext<Node<String>> context) {
                    return Traverser.Action.CONTINUE;
                }

                @Override
                public Traverser.Action onBackRef(TraverseContext<Node<String>> context) {
                    List<String> result = context.getResult();
                    result.add(context.thisNode().getData());
                    return Traverser.Action.CONTINUE;
                }
            });
    
        assertEquals(Arrays.asList("root", "left"), result);
    }    
    
    @Test
    public void testSkipAction () {
        Node<String> root = new Node<String>()
            .data("root");
        
        Node<String> left, right;
        root.child(left = new Node<String>()
                .data("left")
                .child(root))
            .child(right = new Node<String>()
                .data("right")
                .child(left));                
        
        List<String> result = Traverser
            .<Node<String>>depthFirst(Node::getChildren)
            .traverse(root, new ArrayList<>(), new TestVisitor<Node<String>>() {
                @Override
                public Traverser.Action enter(TraverseContext<Node<String>> context) {
                    List<String> result = context.getResult();
                    result.add(context.thisNode().getData());
                    return result.contains("left") || result.contains("right")
                        ? Traverser.Action.SKIP
                        : Traverser.Action.CONTINUE;
                }

                @Override
                public Traverser.Action leave(TraverseContext<Node<String>> context) {
                    return Traverser.Action.CONTINUE;
                }

                @Override
                public Traverser.Action onBackRef(TraverseContext<Node<String>> context) {
                    List<String> result = context.getResult();
                    result.add(context.thisNode().getData());
                    return Traverser.Action.CONTINUE;
                }
            });
    
        assertEquals(Arrays.asList("root", "left", "right"), result);
    }    
    
    @Test
    public void testPreOrderIterator () {
        Node<String> root = new Node<String>()
            .data("root");
        
        Node<String> left, right;
        root.child(left = new Node<String>()
                .data("left")
                .child(root))
            .child(right = new Node<String>()
                .data("right")
                .child(left));                
        
        Iterator<Node<String>> preOrder = Traverser
            .<Node<String>>depthFirst(Node::getChildren)
            .preOrderIterator(root);
    
        List<String> result = StreamSupport
            .stream(Spliterators.spliteratorUnknownSize(preOrder, 0), false)
            .map(Node::getData)
            .collect(Collectors.toList());
        
        assertEquals(Arrays.asList("root", "left", "right"), result);
    }    
    
    @Test
    public void testPostOrderIterator () {
        Node<String> root = new Node<String>()
            .data("root");
        
        Node<String> left, right;
        root.child(left = new Node<String>()
                .data("left")
                .child(root))
            .child(right = new Node<String>()
                .data("right")
                .child(left));                
        
        Iterator<Node<String>> postOrder = Traverser
            .<Node<String>>depthFirst(Node::getChildren)
            .postOrderIterator(root);
    
        List<String> result = StreamSupport
            .stream(Spliterators.spliteratorUnknownSize(postOrder, 0), false)
            .map(Node::getData)
            .collect(Collectors.toList());
        
        assertEquals(Arrays.asList("left", "right", "root"), result);
    }    
    
    @Test
    public void testPreOrderPath () {
        Node<String> root = new Node<String>()
            .data("root");
        
        Node<String> left, right;
        root.child(left = new Node<String>()
                .data("left")
                .child(new Node<String>()
                    .data("left-left")
                    .child(root)))
            .child(right = new Node<String>()
                .data("right")
                .child(left));                
        
        TraversingIterator<Node<String>> preOrder = Traverser
            .<Node<String>>depthFirst(Node::getChildren)
            .preOrderIterator(root);

        while (preOrder.hasNext()) {
            Node<String> node = preOrder.next();
            if ("left-left".equals(node.getData())) {
                List<String> path = preOrder
                    .path()
                    .map(Node::getData)
                    .collect(Collectors.toList());
                    
                assertEquals(Arrays.asList("left-left", "left", "root"), path);
            }
        }
    }    
    
    @Test
    public void testPostOrderPath () {
        Node<String> root = new Node<String>()
            .data("root");
        
        Node<String> left, right;
        root.child(left = new Node<String>()
                .data("left")
                .child(new Node<String>()
                    .data("left-left")
                    .child(root)))
            .child(right = new Node<String>()
                .data("right")
                .child(left));                
        
        TraversingIterator<Node<String>> postOrder = Traverser
            .<Node<String>>depthFirst(Node::getChildren)
            .postOrderIterator(root);

        while (postOrder.hasNext()) {
            Node<String> node = postOrder.next();
            if ("left-left".equals(node.getData())) {
                List<String> path = postOrder
                    .path()
                    .map(Node::getData)
                    .collect(Collectors.toList());
                    
                assertEquals(Arrays.asList("left-left", "left", "root"), path);
            }
        }
    }    
    
    @Test
    public void testSingleBreadthFirst () {
        Node<String> root = new Node<String>()
            .data("Hello, world");                
        
        String result = Traverser
            .<Node<String>>breadthFirst(Node::getChildren)
            .traverse(root, null, new TestVisitor<Node<String>>() {
                @Override
                public Traverser.Action enter(TraverseContext<Node<String>> context) {
                    context.setResult(context.thisNode().getData());
                    return Traverser.Action.CONTINUE;
                }

                @Override
                public Traverser.Action leave(TraverseContext<Node<String>> context) {
                    return Traverser.Action.CONTINUE;
                }

                @Override
                public Traverser.Action onBackRef(TraverseContext<Node<String>> context) {
                    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                }
            });
    
        assertEquals(result, root.getData());
    }
    
    @Test
    public void testTreeBreadthFirst () {
        Node<String> root = new Node<String>()
            .data("root")
            .child(new Node<String>()
                .data("left"))
            .child(new Node<String>()
                .data("right"));                
        
        String result = Traverser
            .<Node<String>>breadthFirst(Node::getChildren)
            .traverse(root, null, new TestVisitor<Node<String>>() {
                @Override
                public Traverser.Action enter(TraverseContext<Node<String>> context) {
                    context.setResult(context.thisNode().getData());
                    return Traverser.Action.CONTINUE;
                }

                @Override
                public Traverser.Action leave(TraverseContext<Node<String>> context) {
                    return Traverser.Action.CONTINUE;
                }

                @Override
                public Traverser.Action onBackRef(TraverseContext<Node<String>> context) {
                    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                }
            });
    
        assertEquals("right", result);
    }    
    
    @Test
    public void testTreeWithCycleBreadthFirst () {
        Node<String> root = new Node<String>()
            .data("root");
        
        Node<String> left, right;
        root.child(left = new Node<String>()
                .data("left")
                .child(root))
            .child(right = new Node<String>()
                .data("right")
                .child(left));                
        
        List<String> result = Traverser
            .<Node<String>>breadthFirst(Node::getChildren)
            .traverse(root, new ArrayList<>(), new TestVisitor<Node<String>>() {
                @Override
                public Traverser.Action enter(TraverseContext<Node<String>> context) {
                    List<String> result = context.getResult();
                    result.add(context.thisNode().getData());
                    return Traverser.Action.CONTINUE;
                }

                @Override
                public Traverser.Action leave(TraverseContext<Node<String>> context) {
                    return Traverser.Action.CONTINUE;
                }

                @Override
                public Traverser.Action onBackRef(TraverseContext<Node<String>> context) {
                    List<String> result = context.getResult();
                    result.add(context.thisNode().getData());
                    return Traverser.Action.CONTINUE;
                }
            });
    
        assertEquals(Arrays.asList("root", "left", "right", "root", "left"), result);
    }    
    
    @Test
    public void testBreadthFirstIterator () {
        Node<String> root = new Node<String>()
            .data("root");
        
        Node<String> left, right;
        root.child(left = new Node<String>()
                .data("left")
                .child(root))
            .child(right = new Node<String>()
                .data("right")
                .child(left));                
        
        Iterator<Node<String>> preOrder = Traverser
            .<Node<String>>breadthFirst(Node::getChildren)
            .preOrderIterator(root);
    
        List<String> result = StreamSupport
            .stream(Spliterators.spliteratorUnknownSize(preOrder, 0), false)
            .map(Node::getData)
            .collect(Collectors.toList());
        
        assertEquals(Arrays.asList("root", "left", "right"), result);
    }    
    
    @Test
    public void testBreadthFirstPreOrderPath () {
        Node<String> root = new Node<String>()
            .data("root");
        
        Node<String> left, right;
        root.child(left = new Node<String>()
                .data("left")
                .child(new Node<String>()
                    .data("left-left")
                    .child(root)))
            .child(right = new Node<String>()
                .data("right")
                .child(left));                
        
        TraversingIterator<Node<String>> preOrder = Traverser
            .<Node<String>>breadthFirst(Node::getChildren)
            .preOrderIterator(root);

        while (preOrder.hasNext()) {
            Node<String> node = preOrder.next();
            if ("left-left".equals(node.getData())) {
                List<String> path = preOrder
                    .path()
                    .map(Node::getData)
                    .collect(Collectors.toList());
                    
                assertEquals(Arrays.asList("left-left", "left", "root"), path);
            }
        }
    }    
    
    @Test
    public void testBreadthFirstPostOrderPath () {
        Node<String> root = new Node<String>()
            .data("root");
        
        Node<String> left, right;
        root.child(left = new Node<String>()
                .data("left")
                .child(new Node<String>()
                    .data("left-left")
                    .child(root)))
            .child(right = new Node<String>()
                .data("right")
                .child(left));                
        
        TraversingIterator<Node<String>> postOrder = Traverser
            .<Node<String>>breadthFirst(Node::getChildren)
            .postOrderIterator(root);

        while (postOrder.hasNext()) {
            Node<String> node = postOrder.next();
            if ("left-left".equals(node.getData())) {
                List<String> path = postOrder
                    .path()
                    .map(Node::getData)
                    .collect(Collectors.toList());
                    
                assertEquals(Arrays.asList("left-left", "left", "root"), path);
            }
        }
    }    
    
    @Test
    public void testSetVar () {
        Node<String> root = new Node<String>()
            .data("root")
            .child(new Node<String>()
                .data("left"))
            .child(new Node<String>()
                .data("right"));
        
        List<String> result = Traverser
            .<Node<String>>depthFirst(Node::getChildren)
            .traverse(root, new ArrayList<>(), new TestVisitor<Node<String>>() {
                @Override
                public Traverser.Action enter(TraverseContext<Node<String>> context) {
                    Node<String> node = context.thisNode();
                    if ("root".equals(node.getData())) {
                        // define local var
                        context.getContextVars().put(String.class, null);
                        // set "String" var value
                        context.setVar(String.class, "rootVar");
                    } else {
                        // verify we have a Var propagated up from the parent
                        String var = context.getVar(String.class);
                        assertEquals(var, "rootVar");
                        // verify local vars don't contain this variable
                        assertNull(context.getContextVars().get(String.class));
                    }
                    
                    return Traverser.Action.CONTINUE;
                }

                @Override
                public Traverser.Action leave(TraverseContext<Node<String>> context) {
                    // Make sure the var defined in our context is available
                    Node<String> node = context.thisNode();
                    if ("root".equals(node.getData())) {
                        assertEquals(context.getVar(String.class), "rootVar");
                        assertEquals(context.getContextVars().get(String.class), "rootVar");
                    }
                    
                    return Traverser.Action.CONTINUE;
                }

                @Override
                public Traverser.Action onBackRef(TraverseContext<Node<String>> context) {
                    return Traverser.Action.CONTINUE;
                }
            });
    }
}
