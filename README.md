# JaSoMe: Java Source Metrics [![Build Status](https://travis-ci.org/rodhilton/jasome.svg?branch=master)](https://travis-ci.org/rodhilton/jasome)

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

## Metrics

Jasome is currently tracking the following metrics:
   
 * **Raw Total Lines of Code (RTLOC)** - The actual number of lines of code in a
   class, using the line numbers of the file itself.  Comments, whitespace, and
   everything else is counted. _(class)_
 * **Total Lines of Code (TLOC)** - The total number of lines of code, ignoring
   comments, whitespace, and formatting differences _(package, class, method)_
 * **Number of Attributes (NF)** - The number of fields/attributes _(class)_
 * **Number of Static Attributes (NSF)** - The number of static attributes _(class)_
 * **Number of Public Attributes (NPF)** - The number of public attributes _(class)_
 * **Number of Methods (NM, NOM)** - The number of methods _(class)_
 * **Number of Static Methods (NSM)** - The number of static methods _(class)_
 * **Number of Public Methods (NPM)** - The number of public methods _(class)_
 * **Number of Classes (NOC)** - The number of classes within a package _(package)_
 * **Number of Parameters (NOP)** - The number of parameters a method takes _(method)_ 
 * **Depth of Inheritance Tree (DIT)** - The maximum depth of the inheritance
   hierarchy for a class.  _(class)_
 * **Number of Overridden Methods (NORM)** - The number of methods a class overrides
   or implements from a parent class _(class)_
 * **Number of Inherited Methods (NMI)** - The number of methods a class inherits
   from parent classes _(class)_
 * **Number of Methods Added to Inheritance (NMA)** - The number of methods a
   class inherits adds to the inheritance hierarchy; methods defined on the class
   that it did not override or inherit _(class)_
 * **Specialization Index (SIX)** - How specialized a class is, defined as (DIT * NORM) / (NOM) _(class)_
  
More metrics coming soon, I plan to gather every metric outlined in the following sources:

 * http://support.objecteering.com/objecteering6.1/help/us/metrics/toc.htm
 * http://metrics.sourceforge.net/
 * http://www.cs.kent.edu/~jmaletic/cs63901/lectures/SoftwareMetrics.pdf
 * http://www.alibris.com/Object-Oriented-Metrics-Measures-of-Complexity-Brian-Henderson-Sellers/book/29695100
 * http://www.objectmentor.com/resources/articles/oodmetrc.pdf
 
And a lot more
  
# Running

Download and install the latest distribution and unzip, then run:

  ```
  ./bin/jasome <directory to analyze>
  ```
  
JaSoMe will gather metrics and output them to the console.  You can save the XML
to a file using the `--output <file>` option.
