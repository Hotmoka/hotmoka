#!/bin/bash

# generate the Markdown version
cp Takamaka.source Takamaka.md

# place figure references. I miss Latex...
sed -i 's/@fig:projects/1/g' Takamaka.md
sed -i 's/@fig:family_jar/3/g' Takamaka.md
sed -i 's/@fig:family/2/g' Takamaka.md
sed -i 's/@fig:blockchain1/4/g' Takamaka.md
sed -i 's/@fig:blockchain2/5/g' Takamaka.md
sed -i 's/@fig:blockchain3/6/g' Takamaka.md
sed -i 's/@fig:contract_hierarchy/7/g' Takamaka.md
sed -i 's/@fig:cross_wins/8/g' Takamaka.md
sed -i 's/@fig:tictactoe_draw/9/g' Takamaka.md
sed -i 's/@fig:tictactoe_grid/10/g' Takamaka.md
sed -i 's/@fig:tictactoe_linear/11/g' Takamaka.md
sed -i 's/@fig:array_hierarchy/12/g' Takamaka.md
cp Takamaka.md temp.md
sed -i "/^\[PDFonly]:/d" Takamaka.md
sed -i "s/\[Markdownonly]://g" Takamaka.md

# generate the PDF version now
sed -i "/^\[Markdownonly]:/d" temp.md
sed -i "s/\[PDFonly]://g" temp.md
pandoc temp.md -o Takamaka.tex --include-in-header mystylefile.tex --toc --highlight-style=kate -V geometry:a4paper -V documentclass:book -V pagestyle:headings -V papersize:a4 -V colorlinks:true
rm temp.md
sed -i 's/\\begin{verbatim}/\\begin{myverbatim}\n\\begin{verbatim}/g' Takamaka.tex
sed -i 's/\\end{verbatim}/\\end{verbatim}\n\\end{myverbatim}/g' Takamaka.tex
sed -i 's/103 \& alicudi/$10^3$ \& alicudi/g' Takamaka.tex
sed -i 's/106 \& filicudi/$10^6$ \& filicudi/g' Takamaka.tex
sed -i 's/109 \& stromboli/$10^9$ \& stromboli/g' Takamaka.tex
sed -i 's/1012 \& vulcano/$10^{12}$ \& vulcano/g' Takamaka.tex
sed -i 's/1015 \& salina/$10^{15}$ \& salina/g' Takamaka.tex
sed -i 's/1018 \& lipari/$10^{18}$ \& lipari/g' Takamaka.tex
sed -i 's/1021 \& takamaka/$10^{21}$ \& takamaka/g' Takamaka.tex
sed -i 's/\\chapterfont{\\clearpage}//g' Takamaka.tex
sed -i 's/\\usepackage{sectsty}//g' Takamaka.tex
sed -i 's/\\chapter{Table of Contents}/\\begin{comment}\\chapter{Table of Contents}/g' Takamaka.tex
sed -i 's/\\hypertarget{introduction}/\\end{comment}\n\n\\hypertarget{introduction}/g' Takamaka.tex
pdflatex Takamaka.tex

