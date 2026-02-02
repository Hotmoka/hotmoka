#!/bin/bash

# This script creates a zip containing the html files for publishing
# the documentation online; it is meant to be run after create_tutorial.sh

cd target/html
rm -f *.tex
rm -f tutorial.pdf
rm -f tutorial.log
rm -f tutorial.out
rm -f tutorial.toc
rm -f tutorial.idx
rm -f tutorial.aux
rm -f tutorial.lwarpmkconf
rm -f texput.log
rm -f tutorial_html.aux
rm -f tutorial_html.bbl
rm -f tutorial_html.blg
rm -f tutorial_html.idx
rm -f tutorial_html.ilg
rm -f tutorial_html.ind
rm -f tutorial_html.log
rm -f tutorial_html.pdf
rm -f tutorial_html.sidetoc
rm -f tutorial_html.toc
rm -f tutorial-images.txt
zip -r tutorial.zip *

cd ../..