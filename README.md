# JaSoMe: Java Source Metrics [![Build Status](https://travis-ci.org/rodhilton/jasome.svg?branch=master)](https://travis-ci.org/rodhilton/jasome)  [![GitHub issues](https://img.shields.io/github/issues/rodhilton/jasome.svg)](https://github.com/rodhilton/jasome/issues) [![Latest release](https://img.shields.io/github/release/rodhilton/jasome.svg?colorB=dfb430)](https://github.com/rodhilton/jasome/releases/latest) [![Github All Releases](https://img.shields.io/github/downloads/rodhilton/jasome/total.svg?colorB=0083c3)](https://github.com/rodhilton/jasome/releases)

Jasome (JAH-suhm, rhymes with awesome) is a source code analyzer that mines 
internal quality metrics from projects based on source code alone.  This 
distinguishes Jasome from similar tools by not requiring the project first be
compiled, or even compilable.
 
Most analyzers only work on projects that successfully compile, which in the
case of Java projects means all dependencies must be satisfied, any external
libraries are properly on the classpath, any code generation utilities must
properly run, and so on.  However, when all you need is the quality metrics
for Java source code, this compilation requirement is unnecessary since most
metrics can be derived from the source code alone.

Jasome scans a given directory for .java files and performs Best Guess Metrics
Analysis to derive various quality metrics, and then outputs those metrics on a
a per-file basis as XML.  Each XML output will contain the name/path of the file
analyzed, and the values of its per-class and per-method metrics. It will also
list directory structures and their per-package metrics.

Best Guess Metrics Analysis means Jasome will continue collecting metrics even
when individual files make it difficult.  When code references methods or objects
defined in dependencies that may or may not be on the class path, Jasome will
assume those dependencies are present and properly compilable.  If Jasome cannot
make a clear determination for analysis, it will make a best guess and output
metrics whose values are as close as possible to the actual metrics values one
would achieve if analyzing the data with a metrics engine that relies on proper
compilation.  When a java class is syntactically invalid and would not compile,
Jasome will either skip the offending methods or classes, or skip the file entirely
but continue processing the remaining files.  In other words, there are definitely
ways someone can manipulate their code to intentionally change the measurements
for metrics, but by and large on a normal project Jasome will be accurate.

# Getting Started

Download the latest distribution and unzip, change into directory, then run:

  ```
  bin/jasome <directory to analyze>
  ```
  
JaSoMe will gather metrics and output them to the console.  You can save the XML
to a file using the `--output <file>` option.

# Metrics

Jasome is either currently tracking or planning to track the following metrics:
   
 - [x] **Raw Total Lines of Code (RTLOC)** - The actual number of lines of code in a
   class, using the line numbers of the file itself.  Comments, whitespace, and
   everything else is counted. _(class)_
 - [x] **Total Lines of Code (TLOC)** - The total number of lines of code, ignoring
   comments, whitespace, and formatting differences _(project, package, class, method)_
 - [x] **Number of Attributes (NF)** - The number of fields/attributes _(class)_
 - [x] **Number of Static Attributes (NSF)** - The number of static attributes _(class)_
 - [x] **Number of Public Attributes (NPF)** - The number of public attributes _(class)_
 - [x] **Number of Methods (NM)** - The number of methods _(class)_
 - [x] **Number of Static Methods (NSM)** - The number of static methods _(class)_
 - [x] **Number of Public Methods (NPM)** - The number of public methods _(class)_
 - [x] **Number of Classes (NOC)** - The number of classes within a package _(package)_
 - [x] **Number of Parameters (NOP)** - The number of parameters a method takes _(method)_ 
 - [x] **Depth of Inheritance Tree (DIT)** - The maximum depth of the inheritance
     hierarchy for a class.  _(class)_
 - [x] **Number of Overridden Methods (NORM)** - The number of methods a class overrides
     or implements from a parent class _(class)_
 - [x] **Number of Inherited Methods (NMI)** - The number of methods a class inherits
   from parent classes _(class)_
 - [x] **Number of Methods Added to Inheritance (NMA)** - The number of methods a
   class inherits adds to the inheritance hierarchy; methods defined on the class
   that it did not override or inherit _(class)_
 - [x] **Specialization Index (SIX)** - How specialized a class is, defined as ![(DIT * NORM) / NOM](https://latex.codecogs.com/gif.latex?%5Cinline%20%5Cfrac%7BDIT%20*%20NORM%7D%7BNOM%7D) _(class)_
 - [x] **McCabe Cyclomatic Complexity (VG)** - The number of unique possible paths
     through code _(method)_
 - [x] **Weighed Methods per Class (WMC)** - The summation of all of the cyclomatic
       complexities of all methods on a class _(class)_
 - [x] **Lack of Cohesion Methods (LCOM)** - A measure for the Cohesiveness of a class.
       Calculated with the Henderson-Sellers method, based on the number of disjoint sets
       formed by comparing methods with the attributes they use _(class)_
 - [x] **Number of Interfaces (NOI)** - The number of abstract classes (and interfaces) in a package _(package)_
 - [x] **Afferent Coupling (Ca)** - Number of classes outside a package that depend on it _(package)_
 - [x] **Efferent Coupling (Ca)** - Number of classes inside a package that depend on classes outside of it _(package)_
 - [x] **Instability (I)** - Effectively the riskiness of a package, how often it has a reason to change, ![Ce/(Ce+Ca)](https://latex.codecogs.com/gif.latex?%5Cinline%20%5Cfrac%7BCe%7D%7BCa&plus;Ce%7D) _(package)_
 - [x] **Abstractness (A)** - The number of abstract classes (and interfaces) divided by the total number of types in a package, NOI / NOC _(package)_
 - [x] **Normalized Distance from Main Sequence (DMS)** - Robert Martin's metric for a packages distance from ideal,  ![| A + I - 1 |](https://latex.codecogs.com/gif.latex?%5Cinline%20%7C%20A%20&plus;%20I%20-%201%20%7C), _(package)_
 - [x] **Nested Block Depth (NBD)** - The maximum depth of the deepest level of nesting within a method _(method)_
 - [x] **Number of Children (NOCh)** - Number of classes that directly extend this class _(class)_
 - [x] **Number of Parents (NOPa)** - Number of classes that this class directly extends _(class)_
 - [x] **Number of Descendants (NOD)** - Total number of classes that have this class as an ancestor _(class)_
 - [x] **Number of Ancestors (NOA)** - Total number of classes that have this class as a descendant _(class)_
 - [ ] **Number of Links (NOL)** - Number of links (associations, generalizations, use links) between _(package)_
 - [ ] **Class Category Relational Cohesion (CCRC)** - The rate of cohesion between a package's classes. NOL / NOC _(package)_
 - [ ] **Number of Comparisons (NCOMP)** - Number of comparisons in a method _(method)_
 - [ ] **Number of Control Variables (NVAR)** - Number of control variables referenced in a method _(method)_
 - [ ] **McClure’s Complexity Metric (MCLC)** - NCOMP + NVAR _(method)_
 - [ ] **Fan-out (Fout)** - The number of methods immediately subordinate to a method _(method)_
 - [ ] **Fan-in (Fin)** - The number of methods that invoke a method _(method)_
 - [ ] **Structural Complexity (Si)** - Fout^2 _(method)_
 - [ ] **Data Complexity (Di)** - (NOP+1)/(Fout+1) _(method)_
 - [ ] **System Complexity (Ci)** - Si + Di _(method)_
 - [ ] **Number of Collaborations (CRC)** - The number of collaborations between a class and all others _(class)_
 - [ ] **Method Inheritance Factor (MIF)** - see http://www.cs.kent.edu/~jmaletic/cs63901/lectures/SoftwareMetrics.pdf _(class)_
 - [ ] **Coupling Factor (CF)** - see http://www.cs.kent.edu/~jmaletic/cs63901/lectures/SoftwareMetrics.pdf _(class)_
 - [ ] **Polymorphism Factor (PF)** - see http://www.cs.kent.edu/~jmaletic/cs63901/lectures/SoftwareMetrics.pdf _(class)_
 
  
More metrics coming soon, I plan to gather every metric outlined in the following sources:

 * http://support.objecteering.com/objecteering6.1/help/us/metrics/toc.htm
 * http://metrics.sourceforge.net/
 * http://www.cs.kent.edu/~jmaletic/cs63901/lectures/SoftwareMetrics.pdf
 * http://www.alibris.com/Object-Oriented-Metrics-Measures-of-Complexity-Brian-Henderson-Sellers/book/29695100
 * http://www.objectmentor.com/resources/articles/oodmetrc.pdf
 
And a lot more
