#!/bin/bash

# generate the Markdown version
cp Hotmoka.source Hotmoka.md
cp pics/state1.fig state1_copy.fig
cp pics/state2.fig state2_copy.fig
cp pics/state3.fig state3_copy.fig

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
sed -i 's/@fig:runs/17/g' Hotmoka.md
sed -i 's/@fig:blockchain1/18/g' Hotmoka.md
sed -i 's/@fig:blockchain2/19/g' Hotmoka.md
sed -i 's/@fig:blockchain3/20/g' Hotmoka.md
sed -i 's/@fig:contract_hierarchy/21/g' Hotmoka.md
sed -i 's/@fig:lists_hierarchy/22/g' Hotmoka.md
sed -i 's/@fig:arrays_hierarchy/23/g' Hotmoka.md
sed -i 's/@fig:cross_wins/24/g' Hotmoka.md
sed -i 's/@fig:tictactoe_draw/25/g' Hotmoka.md
sed -i 's/@fig:tictactoe_grid/26/g' Hotmoka.md
sed -i 's/@fig:tictactoe_linear/27/g' Hotmoka.md
sed -i 's/@fig:byte_array_hierarchy/28/g' Hotmoka.md
sed -i 's/@fig:map_hierarchy/29/g' Hotmoka.md
sed -i 's/@fig:node_hierarchy/30/g' Hotmoka.md
sed -i 's/@fig:erc20_hierarchy/31/g' Hotmoka.md
sed -i 's/@fig:erc721_hierarchy/32/g' Hotmoka.md

sed -i 's/@hotmoka_version/1.0.6/g' Hotmoka.md
sed -i 's/@tendermint_version/0.34.15/g' Hotmoka.md
sed -i 's/@takamakaCode/b991b27cb8276c6f9d0cad9f6ce251a661ff4bc7fba55b3362d9e6fac31dec1a/g' Hotmoka.md
sed -i 's/@manifest/a5d4a29b2cd0b183bcdc5d47ed3196c20a021757ec1dcc3aba25d46c0ab2b719#0/g' Hotmoka.md
sed -i 's/@gamete/9ed7f9894dad170f2eb0d44cf70b00718b72536df5578ece4881d7893df2974c#0/g' Hotmoka.md
sed -i 's/@gasStation/a5d4a29b2cd0b183bcdc5d47ed3196c20a021757ec1dcc3aba25d46c0ab2b719#10/g' Hotmoka.md
sed -i 's/@validators/a5d4a29b2cd0b183bcdc5d47ed3196c20a021757ec1dcc3aba25d46c0ab2b719#2/g' Hotmoka.md
sed -i 's/@maxFaucet/10000000000000/g' Hotmoka.md
sed -i 's/@chainid/marabunta/g' Hotmoka.md
sed -i 's/@chainid/marabunta/g' state1_copy.fig
sed -i 's/@chainid/marabunta/g' state2_copy.fig
sed -i 's/@chainid/marabunta/g' state3_copy.fig
sed -i 's/@account1/551f32570409cd856f96537d546a65a9f7ffed0ec62ed1a90db346c0adf03cbe#0/g' Hotmoka.md
sed -i 's/@account1_short/551f3257040...#0/g' state2_copy.fig
sed -i 's/@account1_short/551f3257040...#0/g' state3_copy.fig
sed -i "s/@publickeyaccount1/dOo2zVppD\/NoIGE1AcwrFQeer8vPoibGo1M8s4zuzKg=/g" Hotmoka.md
sed -i "s/@publickeyaccount1_short/dOo2zVppD\/No.../g" state2_copy.fig
sed -i "s/@publickeyaccount1_short/dOo2zVppD\/No.../g" state3_copy.fig
sed -i 's/@family_address/2e53eb7ccd0c149ee1a1fdbd4633aac5308bbde33758122cff78a6242f8ed2d2/g' Hotmoka.md
sed -i 's/@family_address_short/2e53eb7ccd0c.../g' state3_copy.fig
sed -i 's/@code_family_address/f4ba6f0d52bb3c511c2662c049483a4ed011b738ca664f36981093fd8784c460/g' Hotmoka.md
sed -i 's/@family2_address/0f97d91463305bfb726818a28934d055bcb55a9da8ff722599123011d769dc75/g' Hotmoka.md
sed -i 's/@family_exported_address/d6759e78b014f17ea63f1d85f479c42c5b0e58a605f606fa62b658f9e010a8b5/g' Hotmoka.md
sed -i 's/@family3_address/2368f48451f56819da073784b6ce012bdd2d14fc04129852c18b29c65e12b446/g' Hotmoka.md
sed -i 's/@person_object/b355ff613c7f0ad73a008298b7031eca497e8259a22584a84caba9bdc7ed03f7#0/g' Hotmoka.md
sed -i 's/@person2_object/dcd4ba2a548a47e38db6c90b74d51d49ff5c58d02835e69752e75cfc14c6c9fd#0/g' Hotmoka.md
sed -i 's/@person3_object/3dc03d24130576c33d467b096bb688dd6c0f5cf556d817575228539aaf30381d#0/g' Hotmoka.md
sed -i 's/@gradual_ponzi_address/01226893bfd72ad0567a5dac45e7a0b0e6a362d2399eef1ea5afab797bade3ca/g' Hotmoka.md
sed -i 's/@gradual_ponzi_object/416e7662bfb5dcf4a9bfa9adc563381b1fd9db6df5f1dd8fd9e491edc6273f7c#0/g' Hotmoka.md
sed -i 's/@gradual_ponzi_list/416e7662bfb5dcf4a9bfa9adc563381b1fd9db6df5f1dd8fd9e491edc6273f7c#1/g' Hotmoka.md
sed -i 's/@gradual_ponzi_first/416e7662bfb5dcf4a9bfa9adc563381b1fd9db6df5f1dd8fd9e491edc6273f7c#2/g' Hotmoka.md
sed -i 's/@gradual_ponzi_last/bbf219f6eef566eaec214041441b170e018ba0936c081809d51413efa1eed683#0/g' Hotmoka.md
sed -i 's/@account2/61b5e1867abfc978bbc1859ad611001784fc027718fa0538d127f34380ca1058#0/g' Hotmoka.md
sed -i 's/@account3/caaa415201af5f9bb1e45ed2b469b1982ef4cd7c73520cd7c863d9f07766c9c6#0/g' Hotmoka.md
sed -i 's/@account4/01c4099ce14164e012d9f9e5768d07b7dd1c1b66f020823da01a44039b97269d#0/g' Hotmoka.md
sed -i 's/@account5/47890b419a65fe0d0437913102c9e90e54169e0a2a8ee3df01495521c35dea94#0/g' Hotmoka.md
sed -i 's/@account6/07c2b486ddcd5661d0c643b7179b7ff5f45904991ff6582a044020c3da9fe733#0/g' Hotmoka.md
sed -i 's/@account7/c8dac1f071b31e596b8b9c01e37a58f2d3c943149c32bc9da2c40d6b51af8ad2#0/g' Hotmoka.md
sed -i 's/@publickeyaccount4/EcXK83Z5E5Dlz29NU8vR9aqDyk\/twhNOWr4QKxHkZ0Y=/g' Hotmoka.md
sed -i 's/@publickeyaccount5/MIIDQjCCAjUGByqGSM44BAEwggIoAoIBAQCPeTXZua...neLg==/g' Hotmoka.md
sed -i 's/@tictactoe_address/5ff4be7f5a0a8b3c6ea93f890b3fff11b7bb5082cc8a91c6765990d95b32615f/g' Hotmoka.md
sed -i 's/@tictactoe_object/9d0a940c6a1889f75e90fcc630fe00f544cbccca10e9501d8c2ef4f42ab301ef#0/g' Hotmoka.md
sed -i 's/@erc20_address/34a4dc0f8ebb74a631f0f77f34860025c433b0c1c4b77b3f8a98a8fe500a38a3/g' Hotmoka.md
sed -i 's/@erc20_object/e7e9cabbc2c4b256a2e479884bc9bc03aa080ad84d9a905086bed51bd8a9cb54#0/g' Hotmoka.md
sed -i 's/@account_mokito/701e20be588db820744df467826d67b9fe451406d7f75da6ef8aeb6805a7365f#0/g' Hotmoka.md
sed -i 's/@server/panarea.hotmoka.io/g' Hotmoka.md

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
sed -i 's/\\usepackage{sectsty}//g' Hotmoka.tex
sed -i 's/\\chapter{Table of Contents}/\\begin{comment}\\chapter{Table of Contents}/g' Hotmoka.tex
sed -i 's/\\hypertarget{introduction}/\\end{comment}\n\n\\hypertarget{introduction}/g' Hotmoka.tex

# delete the \begin{document}
sed -i 's/\\begin{document}//g' Hotmoka.tex
# plave \begin{document} before \BgThispage
sed -i 's/\\BgThispage/\\begin{document}\n\\BgThispage/g' Hotmoka.tex

pdflatex Hotmoka.tex

mv Hotmoka.md ../README.md

