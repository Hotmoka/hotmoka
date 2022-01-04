#!/bin/bash

# generate the Markdown version
cp Hotmoka.source Hotmoka.md
cp pics/state1.fig state1_copy.fig
cp pics/state2.fig state2_copy.fig
cp pics/state3.fig state3_copy.fig

# place figure references. I miss Latex...
sed -i 's/@fig:receiver_payer/1/g' Hotmoka.md
sed -i 's/@fig:mokito_start/2/g' Hotmoka.md
sed -i 's/@fig:hotwallet_start/3/g' Hotmoka.md
sed -i 's/@fig:state1/4/g' Hotmoka.md
sed -i 's/@fig:mokito_menu/5/g' Hotmoka.md
sed -i 's/@fig:mokito_manifest/6/g' Hotmoka.md
sed -i 's/@fig:state2/7/g' Hotmoka.md
sed -i 's/@fig:mokito_new_account/8/g' Hotmoka.md
sed -i 's/@fig:mokito_elvis_new_account/9/g' Hotmoka.md
sed -i 's/@fig:mokito_show_elvis/10/g' Hotmoka.md
sed -i 's/@fig:mokito_added_elvis/11/g' Hotmoka.md
sed -i 's/@fig:mokito_accounts_menu/12/g' Hotmoka.md
sed -i 's/@fig:mokito_insert_passphrase/13/g' Hotmoka.md
sed -i 's/@fig:mokito_added_the_boss/14/g' Hotmoka.md
sed -i 's/@fig:projects/15/g' Hotmoka.md
sed -i 's/@fig:family_jar/17/g' Hotmoka.md
sed -i 's/@fig:family/16/g' Hotmoka.md
sed -i 's/@fig:state3/18/g' Hotmoka.md
sed -i 's/@fig:runs/19/g' Hotmoka.md
sed -i 's/@fig:blockchain1/20/g' Hotmoka.md
sed -i 's/@fig:blockchain2/21/g' Hotmoka.md
sed -i 's/@fig:blockchain3/22/g' Hotmoka.md
sed -i 's/@fig:contract_hierarchy/23/g' Hotmoka.md
sed -i 's/@fig:lists_hierarchy/24/g' Hotmoka.md
sed -i 's/@fig:arrays_hierarchy/25/g' Hotmoka.md
sed -i 's/@fig:cross_wins/26/g' Hotmoka.md
sed -i 's/@fig:tictactoe_draw/27/g' Hotmoka.md
sed -i 's/@fig:tictactoe_grid/28/g' Hotmoka.md
sed -i 's/@fig:tictactoe_linear/29/g' Hotmoka.md
sed -i 's/@fig:byte_array_hierarchy/30/g' Hotmoka.md
sed -i 's/@fig:map_hierarchy/31/g' Hotmoka.md
sed -i 's/@fig:node_hierarchy/32/g' Hotmoka.md
sed -i 's/@fig:erc20_hierarchy/33/g' Hotmoka.md
sed -i 's/@fig:erc721_hierarchy/34/g' Hotmoka.md

# These must be edited by hand since, for instance, they depend on accounts created in Mokito
sed -i 's/@account_mokito/701e20be588db820744df467826d67b9fe451406d7f75da6ef8aeb6805a7365f#0/g' Hotmoka.md
sed -i 's/@36words_of_account_mokito/word #1: rail\nword #2: double\nword #3: bag\nword #4: dove\nword #5: fluid\n...\nword #34: bounce\nword #35: deposit\nword #36: hotel/g' Hotmoka.md
sed -i 's/@tendermint_version/0.34.15/g' Hotmoka.md

# These can be automatically recomputed with the update script
sed -i 's/@hotmoka_version/1.0.7/g' Hotmoka.md
sed -i 's/@takamakaCode/2b898f851d057a2b0e77d2f51391b7c81b7f96a376ee77181aa3929c017a5993/g' Hotmoka.md
sed -i 's/@manifest/f11b3b1b4b478c7240777ec0944335a4fcf4bb1763a9fcd52c7676be373859f2#0/g' Hotmoka.md
sed -i 's/@gamete/5916b588bf3cc90dae3d2c69988bade56b390745562472345aff848800c17f79#0/g' Hotmoka.md
sed -i 's/@gasStation/f11b3b1b4b478c7240777ec0944335a4fcf4bb1763a9fcd52c7676be373859f2#11/g' Hotmoka.md
sed -i 's/@validators/f11b3b1b4b478c7240777ec0944335a4fcf4bb1763a9fcd52c7676be373859f2#2/g' Hotmoka.md
sed -i 's/@maxFaucet/10000000000000/g' Hotmoka.md
sed -i 's/@chainid/marabunta/g' Hotmoka.md
sed -i 's/@chainid/marabunta/g' state1_copy.fig
sed -i 's/@chainid/marabunta/g' state2_copy.fig
sed -i 's/@chainid/marabunta/g' state3_copy.fig
sed -i 's/@account1/f698bae6f5a6d21a20309d2581d46a3a7e8d3f43802bc676cc52651ff6f36411#0/g' Hotmoka.md
sed -i 's/@short_account1/f698bae6f5a...#0/g' state2_copy.fig
sed -i 's/@short_account1/f698bae6f5a...#0/g' state3_copy.fig
sed -i 's/@36words_of_account1/ 1: electric\n 2: victory\n 3: report\n 4: crack\n 5: name\n 6: capital\n 7: hospital\n 8: adult\n 9: dad\n10: green\n11: rally\n12: invest\n13: hat\n14: frost\n15: orange\n16: regular\n17: spirit\n18: speed\n19: around\n20: demise\n21: fix\n22: tuition\n23: pottery\n24: exist\n25: minor\n26: wheel\n27: theme\n28: fiscal\n29: guess\n30: great\n31: end\n32: physical\n33: want\n34: open\n35: affair\n36: magic/g' Hotmoka.md
sed -i "s/@publickeyaccount1/lDu0keDdQCZZNY3H+c+utniSmQe0coSJdRO6iDRnUdA=/g" Hotmoka.md
sed -i "s/@short_publickeyaccount1/lDu0keDdQC.../g" state2_copy.fig
sed -i "s/@short_publickeyaccount1/lDu0keDdQC.../g" state3_copy.fig
sed -i 's/@family_address/967a1c00eaf6e24cfe52efcdee1a0f037209fbc2bac63e1e9801d0e7860c5a8f/g' Hotmoka.md
sed -i 's/@short_family_address/967a1c00ea.../g' state3_copy.fig
sed -i 's/@code_family_address/84fde74632ced7e3654d40d796873c5542a4a17c4a1ee987dadebe5ea8f3f351/g' Hotmoka.md
sed -i 's/@family2_address/d3e02c711680cc4d2f11c1593572512d35f2aaab3b62782933880b1c030239e3/g' Hotmoka.md
sed -i 's/@family_exported_address/b7fffc9dffff205774ae6753fa612a89429b886ec62fad4817a3370545b1a158/g' Hotmoka.md
sed -i 's/@family3_address/24681fa7eb8aa247e184ec6e9490625becb80b9c8604e12670481ea169da0ce2/g' Hotmoka.md
sed -i 's/@person_object/37735b40020370f0b1d0a7d1b83f60e591579868e383cf06156f28853a159155#0/g' Hotmoka.md
sed -i 's/@person2_object/2bb2100a2368c0f80446e0a179e31949e05c1f1f7ef57058a2ca9d7d7622e81a#0/g' Hotmoka.md
sed -i 's/@person3_object/2bb311f3bb43e8b2550429347a09a9f730b6eb2688bb8ebb7c15c3c96c48e42e#0/g' Hotmoka.md
sed -i 's/@gradual_ponzi_address/64f405c480a8058546d101629819e63463ce7da0e25c6edbb94a4413d99e5c27/g' Hotmoka.md
sed -i 's/@gradual_ponzi_object/84d1edde36aaab618d46742d89647148e413add6d9a77eeb0ddd9130046552ad#0/g' Hotmoka.md
sed -i 's/@gradual_ponzi_list/84d1edde36aaab618d46742d89647148e413add6d9a77eeb0ddd9130046552ad#1/g' Hotmoka.md
sed -i 's/@gradual_ponzi_first/84d1edde36aaab618d46742d89647148e413add6d9a77eeb0ddd9130046552ad#2/g' Hotmoka.md
sed -i 's/@gradual_ponzi_last/0a314615f98a1e800c0e63628911d6bccd6f5c9c4b99b229319cf4ca4860565a#0/g' Hotmoka.md
sed -i 's/@account2/26b240580489d5a00e241db547fe2ae756a0209ae87fc6a17e4a06f36f1e7ff0#0/g' Hotmoka.md
sed -i 's/@account3/48ef7306af5e86adbe01dd7807e1c7bf30fbd3781a63e770245ac72743c5fd1a#0/g' Hotmoka.md
sed -i 's/@account4/43d9e576b03706079ac617ff62430ab3691c75f247d264e33c2a9a0986507c64#0/g' Hotmoka.md
sed -i 's/@account5/1215c77ebec338cc31767b129095708837645fb25e491b98c60a2f6415995d4a#0/g' Hotmoka.md
sed -i 's/@account6/a7e878c6965109d1bf8ec4c9635b3513bfc2055a298e840be5badf8bd166d335#0/g' Hotmoka.md
sed -i 's/@account7/9437ba8c5c29edb48e9fd0bba1a54e0286a84609d2c13391a8fdf5d9b25bff68#0/g' Hotmoka.md
sed -i 's/@publickeyaccount4/shA7+XygJ5Wc+WPccFPis6TLbWxqWVnR3eNTJradf5c=/g' Hotmoka.md
sed -i 's/@short_publickeyaccount5/MIIDQjCCAjUGByqGSM44BAEwggIoAo.../g' Hotmoka.md
sed -i 's/@tictactoe_address/e74e78ee4ee2cfa8a14c5a77f2112484702e225143468bc0c7ed3bf2ed4f8a11/g' Hotmoka.md
sed -i 's/@tictactoe_object/702da7b9404d8391cca09a0dcc46250af711eeee8e22de43387284404565e957#0/g' Hotmoka.md
sed -i 's/@erc20_address/ff3c79e01bdfd37afdd7b9bec052caf5f012e7a58f6a83931cfdfb88d42cb6af/g' Hotmoka.md
sed -i 's/@erc20_object/bb3c76396f27d8402e9252a9194c69ed635f7a2de2385a7f89c0344e6083c8ba#0/g' Hotmoka.md
sed -i 's/@server/panarea.hotmoka.io:8080/g' Hotmoka.md

cp Hotmoka.md temp.md
sed -i "/^\[PDFonly]:/d" Hotmoka.md
sed -i "s/\[Markdownonly]://g" Hotmoka.md

# we regenerate the png figures, since they might contain some string changed
# by previous sed commands
fig2dev -L png -m 4 state1_copy.fig pics/state1.png
fig2dev -L png -m 4 state2_copy.fig pics/state2.png
fig2dev -L png -m 4 state3_copy.fig pics/state3.png
rm *_copy.fig

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
sed -i 's/1021 \& moka/$10^{21}$ \& moka/g' Hotmoka.tex
sed -i 's/\\chapterfont{\\clearpage}//g' Hotmoka.tex
sed -i 's/\\chapter{Table of Contents}/\\begin{comment}\\chapter{Table of Contents}/g' Hotmoka.tex
sed -i 's/\\hypertarget{introduction}/\\end{comment}\n\n\\hypertarget{introduction}/g' Hotmoka.tex

# delete the \begin{document}
sed -i 's/\\begin{document}//g' Hotmoka.tex
# plave \begin{document} before \BgThispage
sed -i 's/\\BgThispage/\\begin{document}\n\\BgThispage/g' Hotmoka.tex

pdflatex Hotmoka.tex

mv Hotmoka.md ../README.md

