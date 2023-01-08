# Thesis code additions
The following files have been added or modified
1. /src/main/java/beamline/dcr/model/relations
  * DcrModelExecution.java
2. /src/main/java/beamline/dcr/modeltomodel/
  * DcrSimilarity.java
  * DriftDetector.java
  * ModelRepository.java
  * testrunners/
    * BasicStreamDriftDetection.java
    * DriftDetectionTester.java
    * ModelGenerator.java
    * PatternChangeSystematicRandom.java
    * PatternChangeTrueRandom.java
    * StreamDriftDetection.java
    * TraceGenerator.java
3. /src/main/java/beamline/dcr/testsoftware/
  * ModelAdaption.java
4. /src/main/java/beamline/dcr/testrunners/
  * PatternChangeComparison.java
5. /src/main/java/distancemetrics
  * CommonNodesAndEdges.java
  * GraphEditDistance.java
  * JaccardDistance.java
6. /src/main/java/helper
  * LinearRegression.java
  * Pair.java

# DCR miner test
This miner is constructed as a product of the thesis Online Discovery and Model-to-Model Comparison of DCR Models from Event Streams by Lasse Helm Trige Starklit.

The research evaluates the miner based on 4 process descriptions (models) and their corresponding event logs which exist in this repository with the following numbers:

- Procurement process : 25
- Annotation process : 101
- Computer repair service : 3
- Dreyer's Fond application management : D 


Currently 3 test runs are implemented to evaluate the miner:

1. A basic test to discover model and compare with another model with followinng arguments:\
  1.1 Event log number (default in repository is 3, 25, 101, D)\
  1.2 Relations Threshold (if not wanted put 0)\
  1.3 List of constraints to mine (Conditions, Response, Exclude, Include). For multiple insert space between\
  1.4 Transitive reduction (see input for 1.3. If none put space)\
  1.5 Save as XML (put true if yes false for no). Will save to .../testsoftware/minedmodels/basic_test_model_*date*.xml\
  1.6 Compare to discover (put true for yes  false for reference model)




2. An online test, mining a model every x observation on different window sizes and compare to reference with the following arguments:\
  2.1 - 2.6 see 1.1 - 1.6\
  2.7 save event logs at the evaluation states. Will save to .../eventlogs/online/online_eventlog_graph*eventlogNumber*_windowsize*windowSize*obs*currentObservedEvents*.csv\
  2.8 List of windowsizes to compare. For multiple insert space between\
  2.9 Evaluation after x observations\
Output is .csv file in: .../testsoftware/evaluations/*logNumber*/modelmodel/results_*windowsize*.csv
   
3. Like 2, but sorts projects the stream from eventlog based on timestamps

4. A framework setting test discovering models on every test with the following arguments:\
  3.1 Event log number (default in repository is 3, 25, 101, D)\
  3.2 Relations Threshold (if not wanted put 0)\
  3.3 Compare to discover (put true for yes  false for reference model)
  3.3 Create plots (true for yes false for no). Will save in .../testsoftware/evaluations/*logNumber*\
Output is .csv file with performacne from all seetting combinations in .../testsoftware/evaluations/*logNumber/performance_test_*date*.csv


One can test conduct additional experiments using other files if at least the following is places:
- Reference process process model (.XML): .../testsoftware/groundtruthmodels/Process*EventLogNumber*.xml
- Discover process process model (.XML if wanted to test on DisCoveR): .../testsoftware/discovermodels/Process*EventLogNumber*.xml
- Event log (XES - Only legal traces): .../testsoftware/eventlogs/eventlog_graph*EventLogNumber*.xes
- Illegal Event log (XES - legal and illegal): .../testsoftware/eventlogs/eventlog_graph*EventLogNumber*_illegal.xes 

## Installation

Prerequisite: Maven running Java version 11.

```bash
mvn package
```

## Run

For 1. BasicTest. 

Example run with: log: 101, relation threshold: 0 , constraints: condition and response, transitive reduction: none, save xml: no, compare to: reference model, constraints in model: condition and response.
```bash
mvn -q clean compile exec:java -Dexec.mainClass="beamline.dcr.testsoftware.testrunners.BasicTester" -Dexec.arguments="101,0,Condition Response, ,false,false,Condition Response"

```

For 2. StreamTester

Example run with: log: 101, relation threshold: 0 , patterns to mine: condition and response, transitive reduction: none, save xml: no, compare to: reference model, save logs: no,trace window sizes: 5 and 10, max traces window: 5 and 10,  evaluate after: 5 event observations, constraints in model: condition and response.
```bash
 mvn -q clean compile exec:java -Dexec.mainClass="beamline.dcr.testsoftware.testrunners.StreamTester" -Dexec.arguments="101,0,Condition Response, ,false,false,false,5 10,5 10,5,Condition Response"
```

For 3. StreamDateTester
Uses date in event log to sort event stream
Example run with: log: 101, relation threshold: 0 , patterns to mine: condition and response, transitive reduction: none, save xml: no, compare to: reference model, save logs: no,trace window sizes: 10, 15, and 20, max traces window: 5, 10 and 15,  evaluate after: 5 event observations, constraints in model: condition and response.
```bash
 mvn -q clean compile exec:java -Dexec.mainClass="beamline.dcr.testsoftware.testrunners.StreamDateTester" -Dexec.arguments="D,0,Condition Response, ,false,false,false,10 15 20 30,5 10 15,5,Condition Response"
```


For 4. FrameworkSettingTester

Example run with: log: 101, relation threshold: 0 , compare to: reference model, create plots: no.
```bash
 mvn -q clean compile exec:java -Dexec.mainClass="beamline.dcr.testsoftware.testrunners.FrameworkSettingTester" -Dexec.arguments="101,0,false,false"
```

