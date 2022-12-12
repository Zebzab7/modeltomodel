minerDir=$PWD
javaTestDir="${minerDir}/src/main/java/beamline/dcr/testsoftware"


#Create online event logs
mvn clean
mvn package


trace_window_sizes=(10)
max_traces=(40 50)
obs_eval=10

for ws in "${trace_window_sizes[@]}"
do
	echo "$ws"
	for mt in "${max_traces[@]}"
	do
		echo "$mt"
 		mvn -q clean compile exec:java -Dexec.mainClass="beamline.dcr.testsoftware.testrunners.CreateOnlineLogs" -Dexec.arguments="0,$obs_eval,$ws,$mt"	
  	done
 done

#Create constraint strings from online event logs using DisCoveR


discoverAlgoDir="/Users/lassestarklit/Downloads/DisCoveR20210809" #needs to be changed!!!


#not synthetic
for entry in "$discoverAlgoDir/eventlogs/online"/*
#for synthetic /modified
#for entry in "$discoverAlgoDir/eventlogs/online/modified"/*
do
	filename="$(basename -- $entry)"
	
	if [[ $filename =~ "500"  ]] && [[ $filename =~ "pm" ]]; then
    java -jar "$discoverAlgoDir/DisCoveR.jar" -PDC "$entry" "$javaTestDir/discovermodels/constraintfiles/online/$filename.txt"
  fi
#	#not synthetic
	#
	#for synthetic /modified
	#java -jar "$discoverAlgoDir/DisCoveR.jar" -PDC "$entry" "$javaTestDir/discovermodels/constraintfiles/online/modified/$filename.txt"
	
done

#Create performance and graph
./gnuplotrun.bash 
