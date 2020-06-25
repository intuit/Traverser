<p align="center"><img src="logo.png" height="120" width="120"/>
 
[![Build Status](https://circleci.com/gh/intuit/Traverser.svg?style=shield&circle-token=536af10b7afa24ed4989946d5236d9776783aaac)](https://app.circleci.com/pipelines/github/intuit/Traverser) [![Join the community on Spectrum](https://withspectrum.github.io/badge/badge.svg)](https://spectrum.chat/traverser)
</p>
<hr />


 
# Traverser: java library to walk object graph

<!-- add badges -->

Traverser solves a one of the most common tasks to operate on tree or graph data structure: 

- flatten into collection or stream
- perform an action during traversal 
- control traversal flow 

It exposes rich and fine level of capabilities like:
- iterators 
- both depth- and breadth- first search (DFS/BFS)
- visitors
- local / global context 

It helps in several areas:
- speed up implementation by re-using generic  solution (stable, tested, well-performant solution)
- reduce codebase
- expand and adjust use cases with simple changes
- decouples traversing from data structure, which boosts maintenability

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
              John Smith
              Lucy Gold
              Sylvester Moonstone
         Sales Department
              Nick Citrine 
              Kleo Ruby 
         Management
              Anna Peridot                  
```

_Disclaimer: Names, characters are businesses are used in fictitious manner. Any resemblance to actual persons, living or dead is purely coincidental._


## Explore 

### Integration: children provider  

Once of benefits of using generic traversing mechanism is to decouple actual traversing from data structure it is being operated on.
Regardless of the way data is organized, core algorithms does not change. But data, root element(s) as well as immediate children for a given elements must be be known to traverser in some way, so the algorithm can move to next iteration.  

Children provider or simply *\<FUNCTION\>* indicates such function, which feeds traverser with children of current given element.
It could be passed as lamda-function or method reference or any other applicable means. 

Children provider should expect a given node or parent object and output is a collection of child nodes.

In more general case, it is possible for child provider to create and return traverser context based on current item at hand.

The simplest way to provide children function (`Node::getChildren`):

```java

Traverser<Node<String>, TraverseContext<Node<String>>>  traveser = Traverser.<Node<String>>depthFirst(Node::getChildren);
//.....
 class Node<T> {
        T data;
        List<Node<T>> children = new ArrayList<>();
        List<Node<T>> getChildren () {
            return children;
        }
  }

```

### Iterator
Iteration is build on top of graph/tree traversal and shares common API.
There are 2 flavors (go deep or go broad) of direction and 2 flavours (before or after) of invocation.
It give 4 total possible combinations of how iteration can be performed. 



Iterators are useful to "flatten" object structure into stream of objects.  

#### Depth-First traversing   

Depth-first always follows reference to child, when next item on current level:


##### Perform an action, when move to next:
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

##### Move to next, perform an action: 
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

Breadth-first always follows next item on current level, when goes to reference to child.
##### Perform an action, when move to next:
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


##### Move to next, perform an action:
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
### Manipulating traversal flow 
Traveser accepts [visitors](https://en.wikipedia.org/wiki/Visitor_pattern) which can be used to take action on a given node.


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
