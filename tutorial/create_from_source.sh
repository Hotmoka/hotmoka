#!/bin/bash

# generate the Markdown version
cp Hotmoka.source Hotmoka.md
cp pics/state1.fig pics/state1_copy.fig
cp pics/state2.fig pics/state2_copy.fig
cp pics/state3.fig pics/state3_copy.fig

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
sed -i 's/@tendermint_version/0.34.14/g' Hotmoka.md
sed -i 's/@takamakaCode/b991b27cb8276c6f9d0cad9f6ce251a661ff4bc7fba55b3362d9e6fac31dec1a/g' Hotmoka.md
sed -i 's/@manifest/a5d4a29b2cd0b183bcdc5d47ed3196c20a021757ec1dcc3aba25d46c0ab2b719#0/g' Hotmoka.md
sed -i 's/@gamete/9ed7f9894dad170f2eb0d44cf70b00718b72536df5578ece4881d7893df2974c#0/g' Hotmoka.md
sed -i 's/@gasStation/a5d4a29b2cd0b183bcdc5d47ed3196c20a021757ec1dcc3aba25d46c0ab2b719#10/g' Hotmoka.md
sed -i 's/@validators/a5d4a29b2cd0b183bcdc5d47ed3196c20a021757ec1dcc3aba25d46c0ab2b719#2/g' Hotmoka.md
sed -i 's/@maxFaucet/10000000000000/g' Hotmoka.md
sed -i 's/@chainid/marabunta/g' Hotmoka.md
sed -i 's/@chainid/marabunta/g' pics/state1_copy.fig
sed -i 's/@chainid/marabunta/g' pics/state2_copy.fig
sed -i 's/@chainid/marabunta/g' pics/state3_copy.fig
sed -i 's/@account1/551f32570409cd856f96537d546a65a9f7ffed0ec62ed1a90db346c0adf03cbe#0/g' Hotmoka.md
sed -i 's/@account1/551f3257040...#0/g' pics/state2_copy.fig
sed -i 's/@account1/551f3257040...#0/g' pics/state3_copy.fig
sed -i "s/@publickeyaccount1/dOo2zVppD\/NoIGE1AcwrFQeer8vPoibGo1M8s4zuzKg=/g" Hotmoka.md
sed -i "s/@publickeyaccount1/dOo2zVppD\/No.../g" pics/state2_copy.fig
sed -i "s/@publickeyaccount1/dOo2zVppD\/No.../g" pics/state3_copy.fig
sed -i 's/@family_address/d6441356d8038851ec8c4b615dc46b1c55c72d4fd8e4906f63e345c9f4dfe64f/g' Hotmoka.md
sed -i 's/@family_address/d6441356d803.../g' pics/state3_copy.fig
sed -i 's/@code_family_address/830ff7f3c268420ee9ab5e5225012e5c27d11d069d9df4d255702174d2b49f28/g' Hotmoka.md
sed -i 's/@family2_address/926bd122361285351f0f2bc80dadc44a334e6791779c6269f807712610ac44b6/g' Hotmoka.md
sed -i 's/@family_exported_address/954c4220fbbddc31fc79f29959f3ecd6c27c2a0e3121a2d0c301ff88a5aef099/g' Hotmoka.md
sed -i 's/@family3_address/d341a598b9badea6046e51036309c9cddf8fcbab80250943e02cda6d7ec63582/g' Hotmoka.md
sed -i 's/@person_object/5ea47fbefbae0df8cc1984fff0aaa159eb075f64a7fc27323f5f3e8fd0adc998#0/g' Hotmoka.md
sed -i 's/@person2_object/563173b4cf375d7d9724d1a0d532749c58ecb7a47bd5e1cfb9c32f4869f522a8#0/g' Hotmoka.md
sed -i 's/@person3_object/8e8306084c6093bf76ecd1073fbd41e182d337b20fc099af92760d0b8ca4659d#0/g' Hotmoka.md
sed -i 's/@gradual_ponzi_address/e10cbb19deca145bf1c12ee424fb61ee35fb7df4af15650ccda7be09aef510dd/g' Hotmoka.md
sed -i 's/@gradual_ponzi_object/5d602efe45bd98d61d4906b72f4a6e0ede94c57c74c0000219da5282ae35b8a9#0/g' Hotmoka.md
sed -i 's/@gradual_ponzi_list/5d602efe45bd98d61d4906b72f4a6e0ede94c57c74c0000219da5282ae35b8a9#1/g' Hotmoka.md
sed -i 's/@gradual_ponzi_first/5d602efe45bd98d61d4906b72f4a6e0ede94c57c74c0000219da5282ae35b8a9#2/g' Hotmoka.md
sed -i 's/@gradual_ponzi_last/079828212756b405cd3f30d4640d8bfdd8e855d7feb3575462ab448efcaab3d8#0/g' Hotmoka.md
sed -i 's/@account2/6602aedcfbee393a2828c7cc06e7319cf92502ce1c026b9e5527c27d799eeff9#0/g' Hotmoka.md
sed -i 's/@account3/84ebc6ccdb2f5e76bb8b7d93c9b60805f518a76ae59f78e1b26bfc3734e5475d#0/g' Hotmoka.md
sed -i 's/@account4/a091252ae70af86e7545e78825d9a5aa02f9a79f48731cc82a042294166c9c5e#0/g' Hotmoka.md
sed -i 's/@account5/4df114e85c55cd8de4cc8a6b5fef38f1551f55fa88b4ee0a71923768e7ce7973#0/g' Hotmoka.md
sed -i 's/@account6/9a1886a97c8a7c6388f95df4796d24c5df0c4d87f3204bcd5317f42948bbdda9#0/g' Hotmoka.md
sed -i 's/@account7/bbb4366229d822fca0b3181fd1d3aba0dca07b36f60f5f68a79f406043021ed1#0/g' Hotmoka.md
sed -i 's/@publickeyaccount4/7s5t2KZ3IKrnq8Xl93bLbIUwa+kUz2JYN0vjyithEok=/g' Hotmoka.md
sed -i 's/@publickeyaccount5/MIIDQjCCAjUGByqGSM44BAEwggIoAoIBAQCPeTXZua...Dadw==/g' Hotmoka.md
sed -i 's/@tictactoe_address/eb11ee45c0629dee6f408a8084e30c4cade49c3ce160c38fbba8717ec80fe88b/g' Hotmoka.md
sed -i 's/@tictactoe_object/271d411bf91a97002a8d248484309793a70d570e9d729649f058c245d59ddc89#0/g' Hotmoka.md
sed -i 's/@erc20_address/a218a576bfe5b6d22fbc4bcd3d7329dfdb5f5105c7f1f5bf1d6a9f31eaa34b7e/g' Hotmoka.md
sed -i 's/@erc20_object/ae11689748f899943ea627902278c171f14baefdaadf9f6aa8bd83af08e5b1b8#0/g' Hotmoka.md
sed -i 's/@account_mokito/067cea2b29d1a3bd0f7c82fb3b6a767e04a8dde8c70c4b9656c1f4f0c5e34cec#0/g' Hotmoka.md
sed -i 's/@server/panarea.hotmoka.io/g' Hotmoka.md

cp Hotmoka.md temp.md
sed -i "/^\[PDFonly]:/d" Hotmoka.md
sed -i "s/\[Markdownonly]://g" Hotmoka.md

# we regenerate the png figures, since they might contain some string changed
# by previous sed commands
fig2dev -L png -m 4 pics/state1_copy.fig pics/state1.png
fig2dev -L png -m 4 pics/state2_copy.fig pics/state2.png
fig2dev -L png -m 4 pics/state3_copy.fig pics/state3.png
rm pics/*_copy.fig

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

