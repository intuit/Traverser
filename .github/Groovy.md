# Groovy example of Traverser Library Integration

Example taken here is the traversal of a cyclic Graph. Following examples are there :
- DFS (PreOrder & PostOrder)
- BFS (PreOrder & PostOrder)

## Code Example

#### Object structure

```java
/**
 * Defines a structure of a graph node
 */
interface GraphNode {

    /**
     *  Vertex Index of a node
     * @return Integer
     */
    Integer getIndex()

    /**
     * If Node is a leaf, returns an empty list
     * Otherwise returns the list of immediate nodes connected to this current
     * node.
     * @return List<GraphNode>
     */
    List<GraphNode> getConnectedNodes()

    /**
     * List of nodes this nodes connects to
     * @param connectedNodes
     */
    void addNodes(List<GraphNode> connectedNodes)
}
```

Sample Implementation of the above Interface
```java
class GraphNodeImpl implements GraphNode {

    Integer vertexIndex
    LinkedList<GraphNodeImpl> connectedNodes

    GraphNodeImpl(vertexIndex) {
        this.vertexIndex = vertexIndex
    }

    void addNodes(List<GraphNode> connectedNodes) {
        this.connectedNodes = connectedNodes
    }

    Integer getIndex() {
        return this.vertexIndex
    }

    List<GraphNode> getConnectedNodes() {
        return this.connectedNodes
    }
}
```

Usage of the Traverser library
```java
import com.intuit.commons.traverser.Traverser
import com.intuit.commons.traverser.TraversingIterator

import java.util.function.Function

class TraverserGroovyExample {

    def preOrderDFS(GraphNode inputNode) {
        TraversingIterator<GraphNodeImpl> graphNodeTraversingIterator = Traverser
                .depthFirst((Function) { node -> node.getConnectedNodes()
                	})
                .preOrderIterator(inputNode)

        println("Following is Pre Order Depth First Traversal starting from inputNode = " + inputNode.getIndex())
        while (graphNodeTraversingIterator.hasNext()) {
            GraphNode graphNode = graphNodeTraversingIterator.next()
            println(graphNode.getIndex())
        }
    }

    def postOrderDFS(GraphNode inputNode) {
        TraversingIterator<GraphNodeImpl> graphNodeTraversingIterator = Traverser
                .depthFirst((Function) { node -> node.getConnectedNodes() })
                .postOrderIterator(inputNode)

        println("Following is Post Order Depth First Traversal starting from inputNode = " + inputNode.getIndex())
        while (graphNodeTraversingIterator.hasNext()) {
            GraphNode graphNode = graphNodeTraversingIterator.next()
            println(graphNode.getIndex())
        }
    }


    def preOrderBFS(GraphNode inputNode) {
        TraversingIterator<GraphNodeImpl> graphNodeTraversingIterator = Traverser
                .breadthFirst((Function) { node -> node.getConnectedNodes() })
                .preOrderIterator(inputNode)

        println("Following is Pre Order Breadth First Traversal starting from inputNode = " + inputNode.getIndex())
        while (graphNodeTraversingIterator.hasNext()) {
            GraphNode graphNode = graphNodeTraversingIterator.next()
            println(graphNode.getIndex())
        }
    }

    def postOrderBFS(GraphNode inputNode) {
        TraversingIterator<GraphNodeImpl> graphNodeTraversingIterator = Traverser
                .breadthFirst((Function) { node -> node.getConnectedNodes() })
                .postOrderIterator(inputNode)

        println("Following is Post Order Breadth First Traversal starting from inputNode = " + inputNode.getIndex())
        while (graphNodeTraversingIterator.hasNext()) {
            GraphNode graphNode = graphNodeTraversingIterator.next()
            println(graphNode.getIndex())
        }
    }
    static void main(def args) {
        def traverserGroovyExample = new TraverserGroovyExample();
        def graphNode0 = new GraphNodeImpl(0)
        def graphNode1 = new GraphNodeImpl(1)
        def graphNode2 = new GraphNodeImpl(2)
        def graphNode3 = new GraphNodeImpl(3)

        graphNode0.addNodes([graphNode1, graphNode2])
        graphNode1.addNodes([graphNode2])
        graphNode2.addNodes([graphNode3, graphNode0])
        graphNode3.addNodes([graphNode3])

        traverserGroovyExample.preOrderDFS(graphNode2)
        traverserGroovyExample.postOrderDFS(graphNode2)
        traverserGroovyExample.preOrderBFS(graphNode2)
        traverserGroovyExample.postOrderBFS(graphNode2)

    }

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
Following is Pre Order Breadth First Traversal starting from inputNode = 2
2
3
0
1

```