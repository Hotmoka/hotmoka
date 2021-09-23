#!/bin/bash

# generate the Markdown version
cp Hotmoka.source Hotmoka.md

# place figure references. I miss Latex...
sed -i 's/@fig:mokito_start/1/g' Hotmoka.md
sed -i 's/@fig:state1/2/g' Hotmoka.md
sed -i 's/@fig:mokito_menu/3/g' Hotmoka.md
sed -i 's/@fig:mokito_manifest/4/g' Hotmoka.md
sed -i 's/@fig:state2/5/g' Hotmoka.md
sed -i 's/@fig:mokito_new_account/6/g' Hotmoka.md
sed -i 's/@fig:mokito_elvis_new_account/7/g' Hotmoka.md
sed -i 's/@fig:mokito_show_elvis/8/g' Hotmoka.md
sed -i 's/@fig:mokito_added_elvis/9/g' Hotmoka.md
sed -i 's/@fig:mokito_accounts_menu/10/g' Hotmoka.md
sed -i 's/@fig:mokito_insert_passphrase/11/g' Hotmoka.md
sed -i 's/@fig:mokito_added_the_boss/12/g' Hotmoka.md
sed -i 's/@fig:projects/13/g' Hotmoka.md
sed -i 's/@fig:family_jar/15/g' Hotmoka.md
sed -i 's/@fig:family/14/g' Hotmoka.md
sed -i 's/@fig:state3/16/g' Hotmoka.md
sed -i 's/@fig:blockchain1/17/g' Hotmoka.md
sed -i 's/@fig:blockchain2/18/g' Hotmoka.md
sed -i 's/@fig:blockchain3/19/g' Hotmoka.md
sed -i 's/@fig:contract_hierarchy/20/g' Hotmoka.md
sed -i 's/@fig:lists_hierarchy/21/g' Hotmoka.md
sed -i 's/@fig:arrays_hierarchy/22/g' Hotmoka.md
sed -i 's/@fig:cross_wins/23/g' Hotmoka.md
sed -i 's/@fig:tictactoe_draw/24/g' Hotmoka.md
sed -i 's/@fig:tictactoe_grid/25/g' Hotmoka.md
sed -i 's/@fig:tictactoe_linear/26/g' Hotmoka.md
sed -i 's/@fig:byte_array_hierarchy/27/g' Hotmoka.md
sed -i 's/@fig:map_hierarchy/28/g' Hotmoka.md
sed -i 's/@fig:node_hierarchy/29/g' Hotmoka.md
sed -i 's/@account1/22e5e16eeed3b4a78176ddfe1f60d5a82b07b0fc0c95a2000b86a806853add39#0/g' Hotmoka.md
sed -i 's/@account2/167fa9c769b99cfcc43dd85f9cc2d06265e2a9bfb6fadc730fbd3dce477b7412#0/g' Hotmoka.md
sed -i 's/@account3/f58a6a89872d5af53a29e5e981e1374817c5f5e3d9900de17bb13369a86d0c43#0/g' Hotmoka.md
sed -i 's/@account_mokito/a2b1c53852f85ee47cc49e085112a7fd486cb89a0363e9717ccecb17a0ae31cf#0/g' Hotmoka.md
sed -i 's/@server/panarea.hotmoka.io/g' Hotmoka.md

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

