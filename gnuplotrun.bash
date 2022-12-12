echo "Gnuplotrun script running..."

thesisDir=$PWD

mvn clean
mvn package
javaTestDir="${thesisDir}/src/main/java/beamline/dcr/testsoftware"

#trace_window_sizes=(5 10 15)
trace_window_sizes=(10)
max_traces=(10 20 30 40 50)
#max_traces=(10 15 20 30)
obs_eval=10


for ws in "${trace_window_sizes[@]}"
do
	for mt in "${max_traces[@]}"
	do
 		mvn -q clean compile exec:java -Dexec.mainClass="beamline.dcr.testsoftware.testrunners.DriftCaptureRun" -Dexec.arguments="0,$obs_eval,$ws,$mt,true"
  	done
done


#not synthetic
for entry in "$javaTestDir/evaluations/driftjaccardtest"/*
#Synthetic / modified
#for entry in "$javaTestDir/evaluations/driftjaccardtest/modified"/*
do
	if [ "${entry: -4}" == ".csv" ]
		then

		filename_ext=${entry##*/}
		filename=${filename_ext%.csv}
		graph_title="${filename//_/ }"  
		
		row_model1="$(grep ",model1," "$entry" | wc -l)"
		
		split_row=$((row_model1 + 1))

		split_event=$(awk -F',' -v rowNumber=$split_row 'NR==rowNumber {print $1}' "$entry")
		#Synthetic / modified
		#gnuplot  -e "filename='$entry'; split=$split_event; titleString='$graph_title'; outputfile='$javaTestDir/evaluations/driftjaccardtest/graphs/modified/$filename.png'" gnuplot_jaccard_fitness.gnuplot
		#not synthetic 
		gnuplot  -e "filename='$entry'; split=$split_event; titleString='$graph_title'; outputfile='$javaTestDir/evaluations/driftjaccardtest/graphs/$filename.png'" gnuplot_jaccard_fitness.gnuplot
	fi 
done

