# Kotlin Example of Traverser Library Integration

This example demonstrates the traversal of a cyclic graph using the Traverser library in Kotlin. It includes examples of Depth-First Search (DFS) and Breadth-First Search (BFS) with both pre-order and post-order traversal modes.

## Code Example

#### Object structure

```kotlin
/**
 * Defines the structure of a graph node.
 */
interface GraphNode {
    fun getIndex(): Int
    fun getConnectedNodes(): List<GraphNode>
    fun addNodes(connectedNodes: List<GraphNode>)
}
```

Sample Implementation of the above Interface

```kotlin

class GraphNodeImpl(private val vertexIndex: Int) : GraphNode {
    private var connectedNodes: List<GraphNode> = emptyList()

    override fun getIndex(): Int {
        return vertexIndex
    }

    override fun getConnectedNodes(): List<GraphNode> {
        return connectedNodes
    }

    override fun addNodes(connectedNodes: List<GraphNode>) {
        this.connectedNodes = connectedNodes
    }
}

```

Usage of the Traverser library

```java
import com.intuit.commons.traverser.Traverser
import com.intuit.commons.traverser.TraversingIterator
import java.util.function.Function

class TraverserKotlinExample {
    fun preOrderDFS(inputNode: GraphNode) {
        val graphNodeTraversingIterator: TraversingIterator<GraphNodeImpl> =
            Traverser.depthFirst(Function { node: GraphNode -> node.getConnectedNodes() })
                .preOrderIterator(inputNode)

        println("Following is Pre Order Depth First Traversal starting from inputNode = ${inputNode.getIndex()}")
        while (graphNodeTraversingIterator.hasNext()) {
            val graphNode = graphNodeTraversingIterator.next()
            println(graphNode.getIndex())
        }
    }

    fun postOrderDFS(inputNode: GraphNode) {
        val graphNodeTraversingIterator: TraversingIterator<GraphNodeImpl> =
            Traverser.depthFirst(Function { node: GraphNode -> node.getConnectedNodes() })
                .postOrderIterator(inputNode)

        println("Following is Post Order Depth First Traversal starting from inputNode = ${inputNode.getIndex()}")
        while (graphNodeTraversingIterator.hasNext()) {
            val graphNode = graphNodeTraversingIterator.next()
            println(graphNode.getIndex())
        }
    }

    fun preOrderBFS(inputNode: GraphNode) {
        val graphNodeTraversingIterator: TraversingIterator<GraphNodeImpl> =
            Traverser.breadthFirst(Function { node: GraphNode -> node.getConnectedNodes() })
                .preOrderIterator(inputNode)

        println("Following is Pre Order Breadth First Traversal starting from inputNode = ${inputNode.getIndex()}")
        while (graphNodeTraversingIterator.hasNext()) {
            val graphNode = graphNodeTraversingIterator.next()
            println(graphNode.getIndex())
        }
    }

    fun postOrderBFS(inputNode: GraphNode) {
        val graphNodeTraversingIterator: TraversingIterator<GraphNodeImpl> =
            Traverser.breadthFirst(Function { node: GraphNode -> node.getConnectedNodes() })
                .postOrderIterator(inputNode)

        println("Following is Post Order Breadth First Traversal starting from inputNode = ${inputNode.getIndex()}")
        while (graphNodeTraversingIterator.hasNext()) {
            val graphNode = graphNodeTraversingIterator.next()
            println(graphNode.getIndex())
        }
    }
}

fun main() {
    val traverserKotlinExample = TraverserKotlinExample()
    val graphNode0 = GraphNodeImpl(0)
    val graphNode1 = GraphNodeImpl(1)
    val graphNode2 = GraphNodeImpl(2)
    val graphNode3 = GraphNodeImpl(3)

    graphNode0.addNodes(listOf(graphNode1, graphNode2))
    graphNode1.addNodes(listOf(graphNode2))
    graphNode2.addNodes(listOf(graphNode3, graphNode0))
    graphNode3.addNodes(listOf(graphNode3))

    traverserKotlinExample.preOrderDFS(graphNode2)
    traverserKotlinExample.postOrderDFS(graphNode2)
    traverserKotlinExample.preOrderBFS(graphNode2)
    traverserKotlinExample.postOrderBFS(graphNode2)
}

```

Output

```
Following is Pre Order Depth First Traversal starting from inputNode = 2
2
3
0
1
Following is Post Order Depth First Traversal starting from inputNode = 2
3
1
0
2
Following is Pre Order Breadth First Traversal starting from inputNode = 2
2
3
0
1
Following is Post Order Breadth First Traversal starting from inputNode = 2
2
3
0
1
```
