<p align="center"><img src="logo.png" height="120" width="120"/>
 
[![Build Status](https://circleci.com/gh/intuit/Traverser.svg?style=shield&circle-token=536af10b7afa24ed4989946d5236d9776783aaac)](https://app.circleci.com/pipelines/github/intuit/Traverser)
[![Join the community on Spectrum](https://withspectrum.github.io/badge/badge.svg)](https://spectrum.chat/traverser)
[![Artifact](https://maven-badges.herokuapp.com/maven-central/com.intuit.commons/traverser/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.intuit.commons/traverser/) 
</p>
<hr />


 
# Traverser: java library to walk object graph

<!-- add badges -->

Traverser solves a one of the most common tasks to operate on tree or graph data structure: 

- enumerate tree or graph nodes into a traverse sequence
- execute client actions on each enumerated node 
- fine grained control of the traversal loop (continue, break, skip nodes)

It exposes rich and fine level of capabilities like:
- iterators 
- both depth- and breadth- first search (DFS/BFS)
- pre- and post- order actions
- cycle detection
- visitors
- global and local node contexts

It helps in several areas:
- speed up implementation by re-using generic  solution (stable, tested, well-performant solution)
- reduce codebase
- expand and adjust use cases with simple changes
- decouples traversing routine from data structure and client actions, which boosts maintenability

## Getting Started

Add dependency on this module and traverse any complex data structure.
Gradle: 
```
 compile 'com.intuit.commons:traverser:1.0.0'
```
Maven:
```
<dependency>
    <groupId>com.intuit.commons</groupId>
    <artifactId>traverser</artifactId>
    <version>1.0.0</version>
    <type>pom</type>
</dependency>
```

## Learn by example 

Representing hierarchy of a medium-sized company is good example to illustrate key features of the traverser.  
```
Company 
    Bussiness Entity
          Teams
                Members
```

Snapshot of a virtual company staff:

```
Company
     BU-Pacific  
         Product Development
              Sylvester Moonstone
              John Smith
              Lucy Gold
         Sales Department
              Nick Citrine 
              Kleo Ruby 
         Management
              Anna Peridot                  
```

_Disclaimer: Names, characters are businesses are used in fictitious manner. Any resemblance to actual persons, living or dead is purely coincidental._


## Explore 

### Integration: children provider  

One of benefits of using generic traversing mechanism is to decouple actual traversing from data structure it is being operated on.
Regardless of the way data is organized, core algorithms does not change. But data, root element(s) as well as children of given parents must be be known to traverser in some way, so the algorithm can move to next iteration.  

Children provider is a *\<FUNCTION\>* that obtains children of a given parent element.
It could be passed as lamda-function or method reference or any other applicable means. 

In the simple form, children provider should expect a given node or parent object and output is a collection of child nodes.
```java
Function<T, Collection<T>>
```

In more general case, children provider obtains a stream of children ```TraverseContext``` for a given parent one. This allows to achieve great customization of the traversal sequence as well as ```TraverseContext``` representations in it. 
```java
BiFunction<Traverser<T, TraverseContext<T>, TraverseContext<T>, Stream<TraverseContext<T>>>>
```
Given tree node class definition below, the simplest way to provide children is to use method reference (`Node::getChildren`):

```java

// Tree node 
 class Node<T> {
        T data;
        List<Node<T>> children = new ArrayList<>();
        List<Node<T>> getChildren () {
            return children;
        }
  }
...
// Traverser for the tree with Node<T> nodes
 Traverser<Node<String>, TraverseContext<Node<String>>>  traveser = Traverser.depthFirst(Node<String>::getChildren);
```

### Iterator

Iterator is build on top of graph/tree traversal and shares common API.
There are 2 flavors (go deep or go broad) of direction and 2 flavours (before or after) of invocation.
This yields 4 total possible combinations of how iteration can be performed. 

Iterators allow to traverse the underlying tree or graph in a simple loop as if they were iterating over a sequence of tree or graph nodes.  

#### Depth-First traversing   

For a given tree or graph node, depth-first traversal always follows children before considering its siblings:

##### perform an action, then move to next

Nodes are enumerated in the depth-first order 

```java
Company c = new Company(); //
// ...
TraversingIterator<Company> i = Traverser.depthFirst(<FUNCTION>).preOrderIterator(c);
// ...
```

Output:
```
Company
BU-Pacific
PD=Product Development
Sylvester Moonstone
John Smith
Lucy Gold
SL=Sales Department
Nick Citrine
Kleo Ruby
MG=Management
Anna Peridot
```

##### move to next, then perform an action:

Nodes are enumerated in the depth-first reverse order 

```java
Company c = new Company(); //
// ...
TraversingIterator<Company> i = Traverser.depthFirst(<FUNCTION>).postOrderIterator(c);
// ...
``` 

Output:
```
Sylvester Moonstone
John Smith
Lucy Gold
PD=Product Development
Nick Citrine
Kleo Ruby
SL=Sales Department
Anna Peridot
MG=Management
BU-Pacific
Company
```


#### Breadth-First traversing

For a given tree or graph node, breadth-first always follows siblings, before it considers its children.

##### Perform an action, then move to next:

Nodes are enumerated in the breadth-first order

```java
Company c = new Company(); //
// ...
TraversingIterator<Company> i = Traverser.breadthFirst(<FUNCTION>).preOrderIterator(c);
// ...
``` 
Output:
```
Company
BU-Pacific
PD=Product Development
SL=Sales Department
MG=Management
Sylvester Moonstone
John Smith
Lucy Gold
Nick Citrine
Kleo Ruby
Anna Peridot
```


##### Move to next, then perform an action:

Nodes are enumerated in breadth-first opposite order (same as breadth-first order)

```java
Company c = new Company(); //
// ...
TraversingIterator<Company> i = Traverser.breadthFirst(<FUNCTION>).postOrderIterator(c);
// ...
``` 
Output
```
Company
BU-Pacific
PD=Product Development
SL=Sales Department
MG=Management
Sylvester Moonstone
John Smith
Lucy Gold
Nick Citrine
Kleo Ruby
Anna Peridot
```
### Traversal loop control

At the core, Traverser enumerates nodes in the internal traversal loop.       
Traveser uses [visitors](https://en.wikipedia.org/wiki/Visitor_pattern) to take actions for each enumerated node.
Result of these visitor's actions controls Traverser internal loop.

* CONTINUE
  
  discover children of the current node and continue the loop
    
* SKIP

  skip children of the current node, but continue the loop
  
* QUIT

  quit (break) the loop


#### Configuration

It is possible to override `version` and `group` of the artifact:
```
./gradlew clean -Pproject.version=1.0.0-SNAPSHOT -Pproject.group=com.intuit.commons  publishToMavenLocal
```
where `project.version` and `project.group` are used to control desired group and version. 
Artifact name is configured with `rootProject.name` property in *settings.gradle*.

## Technologies Used

Minimum required: Java 8

This library depends on Apache `commons-collections` and `slf4j`. 

## Contributing Guidelines

Welcome contributors!
 [CONTRIBUTING.md](.github/CONTRIBUTING.md).

## Local Development

```
./gradlew test
```


## Support

About [opensource](https://opensource.intuit.com) at Intuit.

## Legal 

Read more about license for this software [License](LICENSE).
