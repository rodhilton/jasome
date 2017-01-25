# JaSoMe: Java Source Metrics

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

 * Package Level:
   * List
   * Coming
   * Soon
 * Class Level:
   * Coming
   * Soon
 * Method Level:
   * Coming
   * Soon

It also outputs a best guess project diagram of packages, which classes are in those packages, how classes relate to each other (is-a vs has-a), and what methods are in each class.
