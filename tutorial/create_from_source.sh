#!/bin/bash

# This script transforms the ProgrammingHotmoka.source documentation into the following files:
# - ../README.md: the README file of the repository, in Markdown
# - ProgrammingHotmoka.pdf: a PDF version of the documentation
# - ProgrammingHotmoka.epub: an epub version of the documentation

# generate the Markdown version
cp ProgrammingHotmoka.source ProgrammingHotmoka.md
cp pics/state1.fig state1_copy.fig
cp pics/state2.fig state2_copy.fig
cp pics/state3.fig state3_copy.fig

# place figure references. I miss Latex...
sed -i 's/@fig:receiver_payer/1/g' ProgrammingHotmoka.md
sed -i 's/@fig:mokito_start/2/g' ProgrammingHotmoka.md
sed -i 's/@fig:hotwallet_start/3/g' ProgrammingHotmoka.md
sed -i 's/@fig:state1/4/g' ProgrammingHotmoka.md
sed -i 's/@fig:mokito_menu/5/g' ProgrammingHotmoka.md
sed -i 's/@fig:mokito_manifest/6/g' ProgrammingHotmoka.md
sed -i 's/@fig:state2/7/g' ProgrammingHotmoka.md
sed -i 's/@fig:mokito_new_account/8/g' ProgrammingHotmoka.md
sed -i 's/@fig:mokito_elvis_new_account/9/g' ProgrammingHotmoka.md
sed -i 's/@fig:mokito_show_elvis/10/g' ProgrammingHotmoka.md
sed -i 's/@fig:mokito_added_elvis/11/g' ProgrammingHotmoka.md
sed -i 's/@fig:mokito_accounts_menu/12/g' ProgrammingHotmoka.md
sed -i 's/@fig:mokito_insert_passphrase/13/g' ProgrammingHotmoka.md
sed -i 's/@fig:mokito_added_the_boss/14/g' ProgrammingHotmoka.md
sed -i 's/@fig:projects/15/g' ProgrammingHotmoka.md
sed -i 's/@fig:family_jar/17/g' ProgrammingHotmoka.md
sed -i 's/@fig:family/16/g' ProgrammingHotmoka.md
sed -i 's/@fig:state3/18/g' ProgrammingHotmoka.md
sed -i 's/@fig:runs/19/g' ProgrammingHotmoka.md
sed -i 's/@fig:blockchain1/20/g' ProgrammingHotmoka.md
sed -i 's/@fig:blockchain2/21/g' ProgrammingHotmoka.md
sed -i 's/@fig:blockchain3/22/g' ProgrammingHotmoka.md
sed -i 's/@fig:contract_hierarchy/23/g' ProgrammingHotmoka.md
sed -i 's/@fig:lists_hierarchy/24/g' ProgrammingHotmoka.md
sed -i 's/@fig:arrays_hierarchy/25/g' ProgrammingHotmoka.md
sed -i 's/@fig:cross_wins/26/g' ProgrammingHotmoka.md
sed -i 's/@fig:tictactoe_draw/27/g' ProgrammingHotmoka.md
sed -i 's/@fig:tictactoe_grid/28/g' ProgrammingHotmoka.md
sed -i 's/@fig:tictactoe_linear/29/g' ProgrammingHotmoka.md
sed -i 's/@fig:byte_array_hierarchy/30/g' ProgrammingHotmoka.md
sed -i 's/@fig:map_hierarchy/31/g' ProgrammingHotmoka.md
sed -i 's/@fig:node_hierarchy/32/g' ProgrammingHotmoka.md
sed -i 's/@fig:erc20_hierarchy/33/g' ProgrammingHotmoka.md
sed -i 's/@fig:erc721_hierarchy/34/g' ProgrammingHotmoka.md

# These must be edited by hand since, for instance, they depend on accounts created in Mokito
sed -i 's/@account_mokito/701e20be588db820744df467826d67b9fe451406d7f75da6ef8aeb6805a7365f#0/g' ProgrammingHotmoka.md
sed -i 's/@36words_of_account_mokito/word #1: rail\nword #2: double\nword #3: bag\nword #4: dove\nword #5: fluid\n...\nword #34: bounce\nword #35: deposit\nword #36: hotel/g' ProgrammingHotmoka.md
sed -i 's/@tendermint_version/0.34.15/g' ProgrammingHotmoka.md

# These can be automatically recomputed with the update script
sed -i 's/@hotmoka_version/1.0.7/g' ProgrammingHotmoka.md
sed -i 's/@takamakaCode/5fd6ae9fe7dbd499621f56814c1f6f1e30718ca9aea69b427dee8c16b9f6c665/g' ProgrammingHotmoka.md
sed -i 's/@manifest/188c6c032ca1f4f559e1cd2d3e044ba81e08b6a01934fc12ef0657cb8636c7a8#0/g' ProgrammingHotmoka.md
sed -i 's/@gamete/5aeca15b70978d3aa4973f2611a775cf9db13c2391f03e7ab2593fe010e31cd5#0/g' ProgrammingHotmoka.md
sed -i 's/@gasStation/188c6c032ca1f4f559e1cd2d3e044ba81e08b6a01934fc12ef0657cb8636c7a8#11/g' ProgrammingHotmoka.md
sed -i 's/@validators/188c6c032ca1f4f559e1cd2d3e044ba81e08b6a01934fc12ef0657cb8636c7a8#2/g' ProgrammingHotmoka.md
sed -i 's/@maxFaucet/10000000000000/g' ProgrammingHotmoka.md
sed -i 's/@chainid/marabunta/g' ProgrammingHotmoka.md
sed -i 's/@chainid/marabunta/g' state1_copy.fig
sed -i 's/@chainid/marabunta/g' state2_copy.fig
sed -i 's/@chainid/marabunta/g' state3_copy.fig
sed -i 's/@account1/75af93866a41581c0aa2dd0ab33ac8790637c6dfc759a7bbd8cf97a43ca32be0#0/g' ProgrammingHotmoka.md
sed -i 's/@short_account1/75af93866a4...#0/g' state2_copy.fig
sed -i 's/@short_account1/75af93866a4...#0/g' state3_copy.fig
sed -i 's/@36words_of_account1/ 1: cage\n 2: faint\n 3: act\n 4: snake\n 5: stairs\n 6: derive\n 7: giraffe\n 8: glance\n 9: before\n10: merry\n11: sea\n12: decline\n13: foot\n14: six\n15: boost\n16: else\n17: fix\n18: theory\n19: post\n20: ring\n21: private\n22: output\n23: capable\n24: camp\n25: daughter\n26: dad\n27: vault\n28: rebuild\n29: knife\n30: unaware\n31: dinner\n32: virus\n33: device\n34: bone\n35: way\n36: regret/g' ProgrammingHotmoka.md
sed -i "s/@publickeyaccount1/BheU05MT\/MGmeytPvrdW+Kggj965oh4SQ6seyOoTw1c=/g" ProgrammingHotmoka.md
sed -i "s/@short_publickeyaccount1/BheU05MT\/M.../g" state2_copy.fig
sed -i "s/@short_publickeyaccount1/BheU05MT\/M.../g" state3_copy.fig
sed -i 's/@account_anonymous/75af93866a41581c0aa2dd0ab33ac8790637c6dfc759a7bbd8cf97a43ca32be0#0/g' ProgrammingHotmoka.md
sed -i 's/@new_key/HhKzZWgc6Fad6J1dxx1seEuJZB9m4JhwEbti1VBW52Nr/g' ProgrammingHotmoka.md
sed -i 's/@family_address/967a1c00eaf6e24cfe52efcdee1a0f037209fbc2bac63e1e9801d0e7860c5a8f/g' ProgrammingHotmoka.md
sed -i 's/@short_family_address/967a1c00ea.../g' state3_copy.fig
sed -i 's/@code_family_address/84fde74632ced7e3654d40d796873c5542a4a17c4a1ee987dadebe5ea8f3f351/g' ProgrammingHotmoka.md
sed -i 's/@family2_address/d3e02c711680cc4d2f11c1593572512d35f2aaab3b62782933880b1c030239e3/g' ProgrammingHotmoka.md
sed -i 's/@family_exported_address/b7fffc9dffff205774ae6753fa612a89429b886ec62fad4817a3370545b1a158/g' ProgrammingHotmoka.md
sed -i 's/@family3_address/24681fa7eb8aa247e184ec6e9490625becb80b9c8604e12670481ea169da0ce2/g' ProgrammingHotmoka.md
sed -i 's/@person_object/37735b40020370f0b1d0a7d1b83f60e591579868e383cf06156f28853a159155#0/g' ProgrammingHotmoka.md
sed -i 's/@person2_object/2bb2100a2368c0f80446e0a179e31949e05c1f1f7ef57058a2ca9d7d7622e81a#0/g' ProgrammingHotmoka.md
sed -i 's/@person3_object/2bb311f3bb43e8b2550429347a09a9f730b6eb2688bb8ebb7c15c3c96c48e42e#0/g' ProgrammingHotmoka.md
sed -i 's/@gradual_ponzi_address/64f405c480a8058546d101629819e63463ce7da0e25c6edbb94a4413d99e5c27/g' ProgrammingHotmoka.md
sed -i 's/@gradual_ponzi_object/84d1edde36aaab618d46742d89647148e413add6d9a77eeb0ddd9130046552ad#0/g' ProgrammingHotmoka.md
sed -i 's/@gradual_ponzi_list/84d1edde36aaab618d46742d89647148e413add6d9a77eeb0ddd9130046552ad#1/g' ProgrammingHotmoka.md
sed -i 's/@gradual_ponzi_first/84d1edde36aaab618d46742d89647148e413add6d9a77eeb0ddd9130046552ad#2/g' ProgrammingHotmoka.md
sed -i 's/@gradual_ponzi_last/0a314615f98a1e800c0e63628911d6bccd6f5c9c4b99b229319cf4ca4860565a#0/g' ProgrammingHotmoka.md
sed -i 's/@account2/26b240580489d5a00e241db547fe2ae756a0209ae87fc6a17e4a06f36f1e7ff0#0/g' ProgrammingHotmoka.md
sed -i 's/@account3/48ef7306af5e86adbe01dd7807e1c7bf30fbd3781a63e770245ac72743c5fd1a#0/g' ProgrammingHotmoka.md
sed -i 's/@account4/43d9e576b03706079ac617ff62430ab3691c75f247d264e33c2a9a0986507c64#0/g' ProgrammingHotmoka.md
sed -i 's/@account5/1215c77ebec338cc31767b129095708837645fb25e491b98c60a2f6415995d4a#0/g' ProgrammingHotmoka.md
sed -i 's/@account6/a7e878c6965109d1bf8ec4c9635b3513bfc2055a298e840be5badf8bd166d335#0/g' ProgrammingHotmoka.md
sed -i 's/@account7/9437ba8c5c29edb48e9fd0bba1a54e0286a84609d2c13391a8fdf5d9b25bff68#0/g' ProgrammingHotmoka.md
sed -i 's/@publickeyaccount4/shA7+XygJ5Wc+WPccFPis6TLbWxqWVnR3eNTJradf5c=/g' ProgrammingHotmoka.md
sed -i 's/@short_publickeyaccount5/MIIDQjCCAjUGByqGSM44BAEwggIoAo.../g' ProgrammingHotmoka.md
sed -i 's/@tictactoe_address/e74e78ee4ee2cfa8a14c5a77f2112484702e225143468bc0c7ed3bf2ed4f8a11/g' ProgrammingHotmoka.md
sed -i 's/@tictactoe_object/702da7b9404d8391cca09a0dcc46250af711eeee8e22de43387284404565e957#0/g' ProgrammingHotmoka.md
sed -i 's/@erc20_address/ff3c79e01bdfd37afdd7b9bec052caf5f012e7a58f6a83931cfdfb88d42cb6af/g' ProgrammingHotmoka.md
sed -i 's/@erc20_object/bb3c76396f27d8402e9252a9194c69ed635f7a2de2385a7f89c0344e6083c8ba#0/g' ProgrammingHotmoka.md
sed -i 's/@server/panarea.hotmoka.io/g' ProgrammingHotmoka.md

cp ProgrammingHotmoka.md temp.md
sed -i "/^\[PDFonly]:/d" ProgrammingHotmoka.md
sed -i "s/\[Markdownonly]://g" ProgrammingHotmoka.md

# we regenerate the png figures, since they might contain some string changed
# by previous sed commands
fig2dev -L png -m 4 state1_copy.fig pics/state1.png
fig2dev -L png -m 4 state2_copy.fig pics/state2.png
fig2dev -L png -m 4 state3_copy.fig pics/state3.png
rm *_copy.fig

# generate the PDF version now
sed -i "/^\[Markdownonly]:/d" temp.md
sed -i "s/\[PDFonly]://g" temp.md
pandoc temp.md -o ProgrammingHotmoka.tex --include-in-header mystylefile.tex --include-after-body backcover.tex --toc --highlight-style=kate -V geometry:a4paper -V documentclass:book -V pagestyle:headings -V papersize:a4 -V colorlinks:true
rm temp.md
sed -i 's/\\begin{verbatim}/\\begin{myverbatim}\n\\begin{verbatim}/g' ProgrammingHotmoka.tex
sed -i 's/\\end{verbatim}/\\end{verbatim}\n\\end{myverbatim}/g' ProgrammingHotmoka.tex
sed -i 's/103 \& alicudi/$10^3$ \& alicudi/g' ProgrammingHotmoka.tex
sed -i 's/106 \& filicudi/$10^6$ \& filicudi/g' ProgrammingHotmoka.tex
sed -i 's/109 \& stromboli/$10^9$ \& stromboli/g' ProgrammingHotmoka.tex
sed -i 's/1012 \& vulcano/$10^{12}$ \& vulcano/g' ProgrammingHotmoka.tex
sed -i 's/1015 \& salina/$10^{15}$ \& salina/g' ProgrammingHotmoka.tex
sed -i 's/1018 \& lipari/$10^{18}$ \& lipari/g' ProgrammingHotmoka.tex
sed -i 's/1021 \& moka/$10^{21}$ \& moka/g' ProgrammingHotmoka.tex
sed -i 's/\\chapterfont{\\clearpage}//g' ProgrammingHotmoka.tex
sed -i 's/\\chapter{Table of Contents}/\\begin{comment}\\chapter{Table of Contents}/g' ProgrammingHotmoka.tex
sed -i 's/\\hypertarget{introduction}/\\end{comment}\n\n\\hypertarget{introduction}/g' ProgrammingHotmoka.tex

# delete the \begin{document}
sed -i 's/\\begin{document}//g' ProgrammingHotmoka.tex
# place \begin{document} before \BgThispage
sed -i 's/\\BgThispage/\\begin{document}\n\\BgThispage/g' ProgrammingHotmoka.tex

pdflatex ProgrammingHotmoka.tex

mv ProgrammingHotmoka.md ../README.md

# generate the epub version of the document
# we remove the first lines of the Markdown, that contain Java build information
tail -n +6 ../README.md > ProgrammingHotmoka.md
pandoc -o ProgrammingHotmoka.epub metadata.yaml ProgrammingHotmoka.md
rm ProgrammingHotmoka.md
