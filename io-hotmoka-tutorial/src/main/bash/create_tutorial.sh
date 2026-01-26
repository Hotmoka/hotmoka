#!/bin/bash

# This script compiles the tutorial, generating:
# A book at target/pdf/tutorial.pdf
# A web site at target/html/

mkdir -p target/pdf
rm -r target/pdf/*
cp src/main/latex/*.tex target/pdf
cp -r src/main/resources/pics target/pdf/tutorial-images
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
cp -r src/main/resources/pics target/html/tutorial-images
cd target/html
pdflatex tutorial.tex
lwarpmk html1
bibtex tutorial_html
lwarpmk htmlindex
lwarpmk html1
lwarpmk html1
lwarpmk limages

cd ../..