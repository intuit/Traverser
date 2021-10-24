import com.intuit.commons.traverser.TraverseContext
import com.intuit.commons.traverser.TraverseVisitor
import com.intuit.commons.traverser.Traverser

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

    // create depth-first traverser and execute it
    val foundKleoRuby = Traverser
            .depthFirst(Node::children)
            .traverse(c, false, object : TraverseVisitor<Node, TraverseContext<Node>> {

                override fun enter(context: TraverseContext<Node>): Traverser.Action {
                    val node = context.thisNode()
                    println("enter: " + node?.name)
                    return if (node.name == "Kleo Ruby") {
                        context.setResult(true)
                        // request to abort the loop
                        println("Found. Quitting.")
                        Traverser.Action.QUIT
                    } else {
                        // allow advancing to the children of this node if any
                        Traverser.Action.CONTINUE
                    }
                }

                override fun leave(context: TraverseContext<Node>): Traverser.Action {
                    val node = context.thisNode()
                    println("leave: " + node.name)
                    // in post-process CONTINUE and SKIP are treated equally
                    // to allow advancing to the next discovered nodes, but
                    // do not discover children of this node anymore
                    return Traverser.Action.CONTINUE
                }

                override fun onBackRef(context: TraverseContext<Node>): Traverser.Action {
                    val node = context.thisNode()
                    println("onBackRef: " + node.name + ". What happened?")
                    return Traverser.Action.CONTINUE
                }
            })

    println("foundKleoRuby: $foundKleoRuby")
}
