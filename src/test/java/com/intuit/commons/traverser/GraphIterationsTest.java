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

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Iterating over complete graph
 *
 * https://en.wikipedia.org/wiki/Complete_graph
 */

public class GraphIterationsTest {

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

	@Test(expected = NullPointerException.class)
	public void testNullGraph() {

		TraversingIterator<Node<String>> preOrder = preOrder((Node<String>)null);
		assertFalse(preOrder.hasNext());
		preOrder.next();
	}

	@Test
	public void testKn1() {
		Node<String> v = new Node<String>().data("vertexK1");
		assertOrder(preOrder(v), "vertexK1");
	}

	@Test
	public void testKn2() {
		Node<String> v1 = new Node<String>().data("vertexK1");
		Node<String> v2 = new Node<String>().data("vertexK2");
		v1.child(v2);
		v2.child(v1);

		assertOrder(preOrder(v1), "vertexK1", "vertexK2");
		assertOrder(preOrder(v2), "vertexK2", "vertexK1");
		assertOrder(preOrder(Arrays.asList(v1,v2)), "vertexK1", "vertexK2");
		assertOrder(preOrder(Arrays.asList(v2,v1)), "vertexK2", "vertexK1");
	}



	private TraversingIterator<Node<String>> preOrder(Node<String> node) {
		return Traverser
				.<Node<String>>breadthFirst(Node::getChildren)
				.preOrderIterator(node);
	}

	private TraversingIterator<Node<String>> preOrder(List<Node<String>> list) {
		return Traverser
				.<Node<String>>breadthFirst(Node::getChildren)
				.preOrderIterator(list);
	}

	private void assertOrder(TraversingIterator<Node<String>> preOrder, String... data) {
		for(String datum : data) {
			assertTrue(preOrder.hasNext());
			assertEquals(datum, preOrder.next().getData());
		}
		assertFalse(preOrder.hasNext());
	}
}