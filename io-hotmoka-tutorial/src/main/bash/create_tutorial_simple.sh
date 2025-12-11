#!/bin/bash

# This script...

mkdir -p target/pdf
rm -r target/pdf/*
cp src/main/latex/*.txt target/pdf
LC_ALL=C sed -i 's/[^[:print:][:cntrl:]]//g' target/pdf/moka_nodes_manifest_show.txt
cp src/main/latex/tutorial.tex target/pdf
cp src/main/latex/introduction.tex target/pdf
cp src/main/latex/getting_started.tex target/pdf
cp src/main/latex/parameters.tex target/pdf
cp src/main/latex/biblio.bib target/pdf
cp -r src/main/resources/pics target/pdf
cd target/pdf
pdflatex tutorial.tex

cd ../..


