set terminal png
set datafile separator ','
set arrow from split, graph 0 to split, graph 1 nohead
set title titleString
set output outputfile
set yrange[0:1]
set key right bottom Left title 'Legend' box 3
plot filename using 1:5 title 'Dcr Miner' with lines, filename using 1:6 title 'DisCoveR' with lines
unset output

