import com.intuit.commons.traverser.Traverser
import com.intuit.commons.traverser.TraversingIterator

fun main() {
    // populate the company according example at the top of this discussion
    val c = Company("Company")
            .businessEntity(BusinessEntity("BU-Pacific")
                    .team(Team("PD=Product Development")
                            .member(Member("Sylvester Moonstone"))
                            .member(Member("John Smith"))
                            .member(Member("Lucy Gold")))
                    .team(Team("SL=Sales Department")
                            .member(Member("Nick Citrine"))
                            .member(Member("Kleo Ruby")))
                    .team(Team("MG=Management")
                            .member(Member("Anna Peridot"))))

    // create depth-first post-order iterator
    val i: TraversingIterator<Node> = Traverser
            .depthFirst(Node::children)
            .postOrderIterator(c)

    // print names of the nodes in DFS sequence
    while (i.hasNext()) {
        val node: Node = i.next()
        println(node.name)
    }
}
