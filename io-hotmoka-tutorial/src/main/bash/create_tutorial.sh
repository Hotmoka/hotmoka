#!/bin/bash

# This script...

mkdir -p target/pdf
rm -r target/pdf/*
cp src/main/latex/*.tex target/pdf
%LC_ALL=C sed -i 's/[^[:print:][:cntrl:]]//g' target/pdf/moka_nodes_manifest_show_output.tex
cp src/main/latex/biblio.bib target/pdf
cp -r src/main/resources/pics target/pdf
cd target/pdf
pdflatex tutorial.tex
lwarpmk print1
bibtex tutorial
lwarpmk printindex
lwarpmk print1
lwarpmk print1

cd ../..

mkdir -p target/html
rm -r target/html/*
cp src/main/latex/*.tex target/html
%sed -i 's/</\&langle;/g' target/html/moka_objects_help_show_output.tex
%sed -i 's/>/\&rangle;/g' target/html/moka_objects_help_show_output.tex
%LC_ALL=C sed -i 's/[^[:print:][:cntrl:]]//g' target/html/moka_nodes_manifest_show_output.tex
cp src/main/latex/biblio.bib target/html
cp -r src/main/resources/pics target/html
cd target/html
pdflatex tutorial.tex
lwarpmk html1
bibtex tutorial_html
lwarpmk htmlindex
lwarpmk html1
lwarpmk html1
lwarpmk limages

cd ../..
