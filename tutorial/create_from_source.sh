#!/bin/bash

# generate the Markdown version
cp Hotmoka.source Hotmoka.md

# place figure references. I miss Latex...
sed -i 's/@fig:projects/1/g' Hotmoka.md
sed -i 's/@fig:family_jar/3/g' Hotmoka.md
sed -i 's/@fig:family/2/g' Hotmoka.md
sed -i 's/@fig:blockchain1/4/g' Hotmoka.md
sed -i 's/@fig:blockchain2/5/g' Hotmoka.md
sed -i 's/@fig:blockchain3/6/g' Hotmoka.md
sed -i 's/@fig:contract_hierarchy/7/g' Hotmoka.md
sed -i 's/@fig:lists_hierarchy/8/g' Hotmoka.md
sed -i 's/@fig:arrays_hierarchy/9/g' Hotmoka.md
sed -i 's/@fig:cross_wins/10/g' Hotmoka.md
sed -i 's/@fig:tictactoe_draw/11/g' Hotmoka.md
sed -i 's/@fig:tictactoe_grid/12/g' Hotmoka.md
sed -i 's/@fig:tictactoe_linear/13/g' Hotmoka.md
sed -i 's/@fig:byte_array_hierarchy/14/g' Hotmoka.md
sed -i 's/@fig:map_hierarchy/15/g' Hotmoka.md
sed -i 's/@fig:node_hierarchy/16/g' Hotmoka.md
cp Hotmoka.md temp.md
sed -i "/^\[PDFonly]:/d" Hotmoka.md
sed -i "s/\[Markdownonly]://g" Hotmoka.md

# generate the PDF version now
sed -i "/^\[Markdownonly]:/d" temp.md
sed -i "s/\[PDFonly]://g" temp.md
pandoc temp.md -o Hotmoka.tex --include-in-header mystylefile.tex --include-after-body backcover.tex --toc --highlight-style=kate -V geometry:a4paper -V documentclass:book -V pagestyle:headings -V papersize:a4 -V colorlinks:true
rm temp.md
sed -i 's/\\begin{verbatim}/\\begin{myverbatim}\n\\begin{verbatim}/g' Hotmoka.tex
sed -i 's/\\end{verbatim}/\\end{verbatim}\n\\end{myverbatim}/g' Hotmoka.tex
sed -i 's/103 \& alicudi/$10^3$ \& alicudi/g' Hotmoka.tex
sed -i 's/106 \& filicudi/$10^6$ \& filicudi/g' Hotmoka.tex
sed -i 's/109 \& stromboli/$10^9$ \& stromboli/g' Hotmoka.tex
sed -i 's/1012 \& vulcano/$10^{12}$ \& vulcano/g' Hotmoka.tex
sed -i 's/1015 \& salina/$10^{15}$ \& salina/g' Hotmoka.tex
sed -i 's/1018 \& lipari/$10^{18}$ \& lipari/g' Hotmoka.tex
sed -i 's/1021 \& takamaka/$10^{21}$ \& takamaka/g' Hotmoka.tex
sed -i 's/\\chapterfont{\\clearpage}//g' Hotmoka.tex
sed -i 's/\\usepackage{sectsty}//g' Hotmoka.tex
sed -i 's/\\chapter{Table of Contents}/\\begin{comment}\\chapter{Table of Contents}/g' Hotmoka.tex
sed -i 's/\\hypertarget{introduction}/\\end{comment}\n\n\\hypertarget{introduction}/g' Hotmoka.tex

# delete the \begin{document}
sed -i 's/\\begin{document}//g' Hotmoka.tex
# plave \begin{document} before \BgThispage
sed -i 's/\\BgThispage/\\begin{document}\n\\BgThispage/g' Hotmoka.tex

pdflatex Hotmoka.tex

mv Hotmoka.md ../README.md

