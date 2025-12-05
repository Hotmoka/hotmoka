#!/bin/bash

# This script...

mkdir -p target/pdf
rm -r target/pdf/*
cp src/main/latex/tutorial.tex target/pdf
cp src/main/latex/introduction.tex target/pdf
cp src/main/latex/getting_started.tex target/pdf
cp src/main/latex/parameters.tex target/pdf
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
cp src/main/latex/tutorial.tex target/html
cp src/main/latex/introduction.tex target/html
cp src/main/latex/getting_started.tex target/html
cp src/main/latex/parameters.tex target/html
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

