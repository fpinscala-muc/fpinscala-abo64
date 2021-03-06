# Testing with ScalaTest and ScalaCheck

## Why tests?
* find bugs
* "prove" correctness (= meets expectations/specifications)
* specify behavior ("test first", TTD)
* document and communicate behavior
* fixate behavior (e.g. before refactoring code)
* acceptance criteria (e.g. for product management department)
* avoid future regressions
* explore unknown code
* good feeling (sleep better)

## [ScalaTest](http://scalatest.org/)
* DSL for "human readable" tests: [assertions](http://scalatest.org/user_guide/using_assertions),
[matchers](http://scalatest.org/user_guide/using_matchers) and more
* different [testing styles](http://scalatest.org/user_guide/selecting_a_style):  
FlatSpec, FeatureSpec, FreeSpec, WordSpec, FunSuite, GivenWhenThen, WordSpec, Eventually, BeforeAndAfter, ...
* "If you don't enjoy shopping: ... use [FlatSpec](http://doc.scalatest.org/2.2.1/#org.scalatest.FlatSpec)
for unit and integration testing
and [FeatureSpec](http://doc.scalatest.org/2.2.1/#org.scalatest.FeatureSpec) for acceptance testing."  
-> fair enough enough for me

## What is a test?
* [System Under Test](http://en.wikipedia.org/wiki/System_under_test) (SUT):
function, class, module, application, ...
* test: input -> SUT -> output -> check output against expectations
* problem: how to find good (and sufficient!?) input data? when are we done?
* Edsger W. Dijkstra: "Program testing can be used to show the presence of bugs,
but never show their absence!" -> no correctness or verification (at least for nontrivial SUTs)
* TL;DNR: quality of a test = quality of its input data!
* How can we write good tests? = How do we find good input data?

## Hand-made Testing
* idea: come up with possible input data yourself
* typical and corner cases; exceptional/"irregular" cases
* ScalaTest: individual single tests  
[Example: typical behavior](exercises/src/test/scala/fpinscala/datastructures/ListSpec.scala#L35)  
[Example: exceptional behavior](exercises/src/test/scala/fpinscala/datastructures/ListSpec.scala#L39)
* [table-driven property tests](http://scalatest.org/user_guide/table_driven_property_checks):  
[Example: table-driven test](exercises/src/test/scala/fpinscala/datastructures/ListSpec.scala#L48)  
[Example: Candy Dispenser rules](exercises/src/test/scala/fpinscala/state/StateSpec.scala#L301)
* more compact than single tests (cf. TestNG's DataProvider)
* syntax similar to generator-driven property tests
* but usually contains both input and expected output data
* problem: sufficient coverage? all "representative" cases? how can we be sure?

## [(Generator-driven) Property-based Testing](http://scalatest.org/user_guide/generator_driven_property_checks)
* idea: let the test system automagically generate random test data
* specify "Properties" on SUT: general "rules" or "laws" that our code should obey
* a law is of the form "for all x1,x2,...: f(SUT,x1,x2,...)"  
  [Example: tail and head of List](exercises/src/test/scala/fpinscala/datastructures/ListSpec.scala#L61)

### Advantages of Property-based Testing
* impartial: no (subconscious?) urge to "protect" your own code from bug detection
* potential to detect more bugs due to unforeseen input data
* laws as a kind of additional specification/documentation

## [ScalaCheck](http://www.scalacheck.org/)
* Gen: generate random test data
* Prop: specify SUT properties ("laws", "rules", "invariants")
* [User Guide](https://github.com/rickynils/scalacheck/wiki/User-Guide)

### [Gen](https://github.com/rickynils/scalacheck/blob/master/src/main/scala/org/scalacheck/Gen.scala)
* built-in generators for most Scala standard types: Boolean, String, Int, List, Option, ...  
[Example: random Ints](exercises/src/test/scala/fpinscala/gettingstarted/GettingStartedSpec.scala#L101)  
[Example: random Lists of Ints](exercises/src/test/scala/fpinscala/monoids/MonoidSpec.scala#L116)
* build your own generators with provided [Gen combinators](https://github.com/rickynils/scalacheck/blob/master/src/main/scala/org/scalacheck/Gen.scala#L161)  
[Example: custom generators](exercises/src/test/scala/fpinscala/monoids/MonoidSpec.scala#L163)  
[Example: recursive custom generators](exercises/src/test/scala/fpinscala/datastructures/TreeSpec.scala#L15)
([Arbitrary](https://github.com/rickynils/scalacheck/blob/master/src/main/scala/org/scalacheck/Arbitrary.scala)
makes a generator be usable like standard Scala types)  
Real World Example: DataStore smoketestclient generators

### [Prop](https://github.com/rickynils/scalacheck/blob/master/src/main/scala/org/scalacheck/Prop.scala)
* create Prop with [forAll](https://github.com/rickynils/scalacheck/blob/master/src/main/scala/org/scalacheck/Prop.scala#L736)
* combine Props with combinators  
[Example: Prop && combinator](exercises/src/test/scala/fpinscala/testing/GenSpec.scala#L45)

### Goodies
* Shrinking: ScalaCheck tries to reduce the failure test data to the smallest possible size
* Stateful testing  
[Example: Candy Dispenser][Example: Candy Dispenser rules](exercises/src/test/scala/fpinscala/state/StateSpec.scala#L333)