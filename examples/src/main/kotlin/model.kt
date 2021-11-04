import java.util.ArrayList

/**
 * Defines the minimalistic API to allow to form
 * Company structure tree
 */
interface Node {
    /**
     * Name of a node
     */
    val name: String

    /**
     * If Node is a leaf, returns an empty list
     * Otherwise returns the list of immediate children of the current
     * node.
     * This method is crucial to navigate the tree
     */
    val children: List<Node>

    /**
     * Visitor pattern support for Nodes
     * to demonstrate double-dispatch mechanism
     *
     * @see [Visitor Pattern](https://en.wikipedia.org/wiki/Visitor_Pattern)
     *
     * @see [Double-Dispatch](https://en.wikipedia.org/wiki/Double_dispatch)
     */
    fun <U> accept(data: U, visitor: NodeVisitor<U>): U
}

/**
 * Visitor interface allows to decouple code that needs to be invoked
 * on Node instances from the various ways to enumerate Nodes in the tree
 *
 * Visitor pattern goes hand-in-hand with Double Dispatch pattern that
 * allows to call Node-specific Visitor's method in a type safe manner, without need
 * to determine the type of Node. Node type is determined automatically when
 * [&lt;][] method is called.
 * Each Node implementation knows its own type and delegates the call to the corresponding
 * Visitor's method.
 */
interface NodeVisitor<U> {
    /**
     * Called when accepting Node is Company
     */
    fun visit(company: Company?, data: U): U

    /**
     * Called when accepting Node is BusinessEntity
     */
    fun visit(businessEntity: BusinessEntity?, data: U): U

    /**
     * Called when accepting Node is Team
     */
    fun visit(team: Team?, data: U): U

    /**
     * Called when accepting Node is Member
     */
    fun visit(member: Member?, data: U): U
}

class Company(override var name: String) : Node {
    private var businessEntities: MutableList<BusinessEntity> = ArrayList()
    fun businessEntity(businessEntity: BusinessEntity): Company {
        businessEntities.add(businessEntity)
        return this
    }

    fun getBusinessEntities(): List<BusinessEntity> {
        return businessEntities
    }

    override val children: List<Node>
        get() = getBusinessEntities()

    override fun <U> accept(data: U, visitor: NodeVisitor<U>): U {
        // double-dispatch to the custom method for Company nodes
        return visitor.visit(this, data)
    }
}

class BusinessEntity(override var name: String) : Node {
    private var teams: MutableList<Team> = ArrayList()
    fun team(team: Team): BusinessEntity {
        teams.add(team)
        return this
    }

    fun getTeams(): List<Team> {
        return teams
    }

    override val children: List<Node>
        get() = getTeams()

    override fun <U> accept(data: U, visitor: NodeVisitor<U>): U {
        // double-dispatch to the custom method for BusinessEntity nodes
        return visitor.visit(this, data)
    }
}

class Team(override var name: String) : Node {
    private var members: MutableList<Member> = ArrayList()
    fun member(member: Member): Team {
        members.add(member)
        return this
    }

    fun getMembers(): List<Member> {
        return members
    }

    override val children: List<Node>
        get() = getMembers()

    override fun <U> accept(data: U, visitor: NodeVisitor<U>): U {
        // double-dispatch to the custom method for Team nodes
        return visitor.visit(this, data)
    }
}

class Member(override var name: String) : Node {
    override val children: List<Node>
        get() = emptyList()

    override fun <U> accept(data: U, visitor: NodeVisitor<U>): U {
        // double-dispatch to the custom method for Member nodes
        return visitor.visit(this, data)
    }
}