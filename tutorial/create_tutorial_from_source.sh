#!/bin/bash

# This script transforms the Tutorial.source documentation into the following files:
# - ../README.md: the README file of the repository, in Markdown
# - ProgrammingHotmoka.pdf: a PDF version of the documentation for Hotmoka
# - ProgrammingHotmoka.epub: an epub version of the documentation for Hotmoka
# - ProgrammingHotmoka.mobi: a mobi version of the documentation for Hotmoka
# - ProgrammingBlueknot.pdf: a PDF version of the documentation for Blueknot
# - ProgrammingBlueknot.epub: an epub version of the documentation for Blueknot
# - ProgrammingBlueknot.mobi: a mobi version of the documentation for Blueknot

cp Tutorial.source Tutorial.md
cp pics/state1.fig state1_copy.fig
cp pics/state2.fig state2_copy.fig
cp pics/state3.fig state3_copy.fig

# generate the Markdown version

# place figure references. I miss Latex...
sed -i 's/@fig:receiver_payer/1/g' Tutorial.md
sed -i 's/@fig:mokito_start/2/g' Tutorial.md
sed -i 's/@fig:hotwallet_start/3/g' Tutorial.md
sed -i 's/@fig:state1/4/g' Tutorial.md
sed -i 's/@fig:mokito_menu/5/g' Tutorial.md
sed -i 's/@fig:mokito_manifest/6/g' Tutorial.md
sed -i 's/@fig:state2/7/g' Tutorial.md
sed -i 's/@fig:mokito_new_account/8/g' Tutorial.md
sed -i 's/@fig:mokito_elvis_new_account/9/g' Tutorial.md
sed -i 's/@fig:mokito_show_elvis/10/g' Tutorial.md
sed -i 's/@fig:mokito_added_elvis/11/g' Tutorial.md
sed -i 's/@fig:mokito_accounts_menu/12/g' Tutorial.md
sed -i 's/@fig:mokito_insert_passphrase/13/g' Tutorial.md
sed -i 's/@fig:mokito_added_the_boss/14/g' Tutorial.md
sed -i 's/@fig:projects/15/g' Tutorial.md
sed -i 's/@fig:family_jar/17/g' Tutorial.md
sed -i 's/@fig:family/16/g' Tutorial.md
sed -i 's/@fig:state3/18/g' Tutorial.md
sed -i 's/@fig:runs/19/g' Tutorial.md
sed -i 's/@fig:blockchain1/20/g' Tutorial.md
sed -i 's/@fig:blockchain2/21/g' Tutorial.md
sed -i 's/@fig:blockchain3/22/g' Tutorial.md
sed -i 's/@fig:contract_hierarchy/23/g' Tutorial.md
sed -i 's/@fig:lists_hierarchy/24/g' Tutorial.md
sed -i 's/@fig:arrays_hierarchy/25/g' Tutorial.md
sed -i 's/@fig:cross_wins/26/g' Tutorial.md
sed -i 's/@fig:tictactoe_draw/27/g' Tutorial.md
sed -i 's/@fig:tictactoe_grid/28/g' Tutorial.md
sed -i 's/@fig:tictactoe_linear/29/g' Tutorial.md
sed -i 's/@fig:byte_array_hierarchy/30/g' Tutorial.md
sed -i 's/@fig:map_hierarchy/31/g' Tutorial.md
sed -i 's/@fig:erc20_hierarchy/32/g' Tutorial.md
sed -i 's/@fig:erc721_hierarchy/33/g' Tutorial.md
sed -i 's/@fig:node_hierarchy/34/g' Tutorial.md
sed -i 's/@fig:hotmoka_tendermint/35/g' Tutorial.md
sed -i 's/@fig:inbound_rules/36/g' Tutorial.md
sed -i 's/@fig:entities_hierarchy/37/g' Tutorial.md

# These get automatically recomputed with the update script: do not edit!
sed -i 's/@hotmoka_version/1.5.0/g' Tutorial.md
sed -i 's/@takamakaCode/539c58b64358bcd2aaca8752ae2442aa119b0cadffc233ac7cfc07dd6d69e96f/g' Tutorial.md
sed -i 's/@manifest/9632a28d2690bff35fb0ecf7cdf13e130e496c0a3ae689b4b41251e8afd0396a#0/g' Tutorial.md
sed -i 's/@gamete/f7dc662c145ac39c36fb91d69379680fdee842a2bce66112156cff7c70a327a6#0/g' Tutorial.md
sed -i 's/@gasStation/9632a28d2690bff35fb0ecf7cdf13e130e496c0a3ae689b4b41251e8afd0396a#14/g' Tutorial.md
sed -i 's/@validators/9632a28d2690bff35fb0ecf7cdf13e130e496c0a3ae689b4b41251e8afd0396a#1/g' Tutorial.md
sed -i 's/@maxFaucet/10000000000000/g' Tutorial.md
sed -i 's/@chainid/marabunta/g' Tutorial.md
sed -i 's/@chainid/marabunta/g' state1_copy.fig
sed -i 's/@chainid/marabunta/g' state2_copy.fig
sed -i 's/@chainid/marabunta/g' state3_copy.fig
sed -i 's/@account1/ecc98397a53d4e9cbab9046caa6e5c96372b6965a6d3626753a2dff9a4f8648d#0/g' Tutorial.md
sed -i 's/@short_account1/ecc98397a53...#0/g' state2_copy.fig
sed -i 's/@short_account1/ecc98397a53...#0/g' state3_copy.fig
sed -i 's/@36words_of_account1/ 1: cluster\n 2: window\n 3: notice\n 4: orchard\n 5: roast\n 6: section\n 7: purchase\n 8: limit\n 9: accident\n10: blade\n11: bicycle\n12: march\n13: smart\n14: scrub\n15: future\n16: police\n17: excess\n18: total\n19: purity\n20: angle\n21: skirt\n22: damp\n23: inch\n24: shoulder\n25: clock\n26: enroll\n27: have\n28: plug\n29: chapter\n30: prefer\n31: merge\n32: you\n33: pilot\n34: segment\n35: cash\n36: vintage/g' Tutorial.md
sed -i "s/@publickeyaccount1/Us+I9cmpCom0V2mj6122bZ6ETwkgrEqLhW4\/hImgfn0=/g" Tutorial.md
sed -i "s/@short_publickeyaccount1/Us+I9cmpCo.../g" state2_copy.fig
sed -i "s/@short_publickeyaccount1/Us+I9cmpCo.../g" state3_copy.fig
sed -i 's/@account_anonymous/7d8ad0f560db6e519bdd7d2e17f0f2453dcb63cf09972aca8a187502bb9eab1a#0/g' Tutorial.md
sed -i 's/@new_key/DtoTbvTTFK6vwD4Gqx84Zy8ZKuty2AxUBiHr9ibey9Jz/g' Tutorial.md
sed -i 's/@family_address/d6e61f8eb43ac5ffcd2679cb8e1c364852caac1eb909f1023b8887867c8f61fd/g' Tutorial.md
sed -i 's/@short_family_address/d6e61f8eb4.../g' state3_copy.fig
sed -i 's/@code_family_address/4a49c4de02ef3a89e269cf023d654406b26799873e3a86a5edde23494062fb6f/g' Tutorial.md
sed -i 's/@family2_address/186c69408be303bab1b75b506de1f5080c25ceff72c66a05c99ed406a642e90f/g' Tutorial.md
sed -i 's/@family_exported_address/800683890e775acc01212f25102c56a1aab1244eaa81a5a9f8610bb120344cc9/g' Tutorial.md
sed -i 's/@family3_address/72475b6541c780b7b5ae33c5d762580cf4fe685489e7291336a0030e188209f8/g' Tutorial.md
sed -i 's/@person_object/ad0553caca35a81250e78f301523474d81b87af4d48d5b0a91c7c6beca52d29b#0/g' Tutorial.md
sed -i 's/@person2_object/2651af2642874957433676f4073b35d21bbd28e5ab52970bf9207ca6f45bac9b#0/g' Tutorial.md
sed -i 's/@person3_object/92e098c2cecdce49475fa8a5ee1437f3926829a12e6f8594d2b43a3ffff4ac87#0/g' Tutorial.md
sed -i 's/@gradual_ponzi_address/a45b8ea4b0df82e53271b80e5aff20291b339e6388e3a935ff5b8b355e08fa1f/g' Tutorial.md
sed -i 's/@gradual_ponzi_object/e2b10bce9162b976970b31b6c910845d61ddad5b2e0056850520f4686e0d4a4a#0/g' Tutorial.md
sed -i 's/@gradual_ponzi_list/e2b10bce9162b976970b31b6c910845d61ddad5b2e0056850520f4686e0d4a4a#1/g' Tutorial.md
sed -i 's/@gradual_ponzi_first/e2b10bce9162b976970b31b6c910845d61ddad5b2e0056850520f4686e0d4a4a#2/g' Tutorial.md
sed -i 's/@gradual_ponzi_last/21f66fdcaaa583e15c258ff10c0232cfd45522932a3164678601b3c08cea53b3#0/g' Tutorial.md
sed -i 's/@account2/66ac35b5435eb558f1c09cac7b8eed44a17c71b9cb8fecc7a1822f51b8397e08#0/g' Tutorial.md
sed -i 's/@account3/1624f46d236f29470512d64054a734b746a042e03a1110419bd317cf245becf7#0/g' Tutorial.md
sed -i 's/@account4/96797575214cfebb5e38b132e06b09582d1aadf28895fade1a57a9d123883209#0/g' Tutorial.md
sed -i 's/@account5/b28f591b8d31d291bfeb07d0556d75965d9bb53f3177621bd4845947ea185402#0/g' Tutorial.md
sed -i 's/@account6/6e9d738c572c01f13b53948d0a5bd7d2f8c323fa24f699a9e61fbeaa90d294ca#0/g' Tutorial.md
sed -i 's/@account7/fcdbd2437b0f404df1a384d478184117adef43479126f7804e95225d579a6262#0/g' Tutorial.md
sed -i 's/@publickeyaccount4/+ioEmT5i38Mo2auegR\/Sj0OZxmLbhvaXFtvudDKXMbI=/g' Tutorial.md
sed -i 's/@short_publickeyaccount5/MIIDQjCCAjUGByqGSM44BAEwggIoAo.../g' Tutorial.md
sed -i 's/@tictactoe_address/6eb9b42f336e6745ad90cddfbf9d6a18217a4f3d713fd5c09c51a6e66f80b322/g' Tutorial.md
sed -i 's/@tictactoe_object/dda0e74c32656173df094de78fba1192398a577eefe897b8f1eb56fab978eaef#0/g' Tutorial.md
sed -i 's/@erc20_address/cb01fa40fad6cdcb56a7a0ab99a706f15d9b9533f5cec6bb299ecc9835f66b31/g' Tutorial.md
sed -i 's/@erc20_object/7aadc95bca587da7705496bf1b833594125e983ccaabdb784de42b10c2aa9e8a#0/g' Tutorial.md
sed -i 's/@server/ws:\/\/panarea.hotmoka.io/g' Tutorial.md
sed -i 's/@new_docker_key/E4nsMYUerQq2pB4hKV3vGee3bcWuGPNYw1QtcNqkXXLU/g' Tutorial.md
sed -i 's/@container_id1/2bdcaefb79c9c2109b8fd0266665991f08fd6725bbc66f2754d5bbaabc693493/g' Tutorial.md
sed -i 's/@docker_takamaka_code/539c58b64358bcd2aaca8752ae2442aa119b0cadffc233ac7cfc07dd6d69e96f/g' Tutorial.md
sed -i 's/@docker_manifest/a9214bed3cd29aceb99ac13bb0ec62af5e37cdd62cd38eac48f5de45390ee811#0/g' Tutorial.md
sed -i 's/@docker_gamete/a66fcddf15aa4458068b8f3eba79272c4436e04240891763ab9a756ad6c0fdd0#0/g' Tutorial.md
sed -i 's/@docker_validators/a9214bed3cd29aceb99ac13bb0ec62af5e37cdd62cd38eac48f5de45390ee811#1/g' Tutorial.md
sed -i 's/@docker_validator0/d9aa5ee6c7ba17f746bf539f133e4248bff2af56b6b060e8458358f58e6a8c40#0/g' Tutorial.md
sed -i 's/@docker_id_validator0/2B0CB544900A57983032AE1D87CFA06DB5A0981C/g' Tutorial.md
sed -i 's/@container_id2/0c9878d4857b6709ba8c745c114f5e23fe2c6f2af142c80404d82e03ca3b67d8/g' Tutorial.md
sed -i 's/@container_id3/84a307f16dca5b2e1f199dad17569dd79db54da11f60ad5303314cb8c02128f9/g' Tutorial.md
sed -i 's/@docker_new_account/5ffed1e1bcad06a052a9468d07899ef9170dd990b8d6cf400dcb04a9f628cc5a#0/g' Tutorial.md
sed -i 's/@docker_total_gas_new_account/2575/g' Tutorial.md
sed -i 's/@docker_reduced_balance/999999998762858/g' Tutorial.md
sed -i 's/@docker_balance_validator0/645/g' Tutorial.md
sed -i 's/@docker_staked_validator0/1932/g' Tutorial.md
sed -i 's/@docker_diff1/1237142/g' Tutorial.md
sed -i 's/@docker_diff2/2575/g' Tutorial.md
sed -i 's/@docker_diff3/2/g' Tutorial.md
sed -i 's/@docker_sum1/2577/g' Tutorial.md

# we regenerate the png figures, since they might contain some string changed
# by previous sed commands
fig2dev -L png -m 4 state1_copy.fig pics/state1.png
fig2dev -L png -m 4 state2_copy.fig pics/state2.png
fig2dev -L png -m 4 state3_copy.fig pics/state3.png
rm *_copy.fig

# Hotmoka-specific processing now...
cp Tutorial.md ProgrammingHotmoka.md
sed -i "s/\[Hotmokaonly]://g" ProgrammingHotmoka.md

# These must be edited by hand since, for instance, they depend on accounts created in Mokito or on the configuration (hotmoka/blueknot)
sed -i 's/@tendermint_version/0.34.15/g' ProgrammingHotmoka.md
sed -i 's/@takamaka_version/1.3.0/g' ProgrammingHotmoka.md
sed -i 's/@tool_repo/https:\/\/github.com\/Hotmoka\/hotmoka/g' ProgrammingHotmoka.md
sed -i 's/@tool/moka/g' ProgrammingHotmoka.md
sed -i 's/@Tool/Moka/g' ProgrammingHotmoka.md
sed -i 's/@app_repo/https:\/\/github.com\/Hotmoka\/HotmokaAndroid/g' ProgrammingHotmoka.md
sed -i 's/@app_id_play/io.hotmoka.android.mokito/g' ProgrammingHotmoka.md
sed -i 's/@app/mokito/g' ProgrammingHotmoka.md
sed -i 's/@App/Mokito/g' ProgrammingHotmoka.md
sed -i 's/@type/hotmoka/g' ProgrammingHotmoka.md
sed -i 's/@Type/Hotmoka/g' ProgrammingHotmoka.md
sed -i 's/@docker_hub_user/hotmoka/g' ProgrammingHotmoka.md
sed -i 's/@docker_user/hotmoka/g' ProgrammingHotmoka.md
sed -i 's/@hotwallet_repo/https:\/\/github.com\/Hotmoka\/hotwallet-browser/g' ProgrammingHotmoka.md
sed -i 's/@hotweb3_repo/https:\/\/github.com\/Hotmoka\/hotweb3/g' ProgrammingHotmoka.md
sed -i 's/@fausto_email/fausto.spoto@hotmoka.io/g' ProgrammingHotmoka.md
sed -i 's/@tutorial_repo/https:\/\/github.com\/Hotmoka\/hotmoka_tutorial.git/g' ProgrammingHotmoka.md
sed -i 's/@tutorial_name/hotmoka_tutorial/g' ProgrammingHotmoka.md
sed -i 's/@account_mokito/701e20be588db820744df467826d67b9fe451406d7f75da6ef8aeb6805a7365f#0/g' ProgrammingHotmoka.md
sed -i 's/@36words_of_account_mokito/word #1: rail\nword #2: double\nword #3: bag\nword #4: dove\nword #5: fluid\n...\nword #34: bounce\nword #35: deposit\nword #36: hotel/g' ProgrammingHotmoka.md

cp ProgrammingHotmoka.md temp.md
sed -i "/^\[PDFonly]:/d" ProgrammingHotmoka.md
sed -i "s/\[Markdownonly]://g" ProgrammingHotmoka.md

# generate the PDF version now
sed -i "/^\[Markdownonly]:/d" temp.md
sed -i "s/\[PDFonly]://g" temp.md
pandoc temp.md -o ProgrammingHotmoka.tex --include-in-header mystylefile_hotmoka.tex --include-after-body backcover.tex --toc --highlight-style=kate -V geometry:a4paper -V documentclass:book -V pagestyle:headings -V papersize:a4 -V colorlinks:true
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
pandoc -o ProgrammingHotmoka.epub metadata-hotmoka.yaml ProgrammingHotmoka.md
rm ProgrammingHotmoka.md

# generate the mobi version of the document
ebook-convert ProgrammingHotmoka.epub ProgrammingHotmoka.mobi

# Blueknot-specific processing now...
cp Tutorial.md ProgrammingBlueknot.md

# some paragraphs do not exist in the Blueknot version
sed -i "/^\[Hotmokaonly]:/d" ProgrammingBlueknot.md

# These must be edited by hand since, for instance, they depend on accounts created in Mokito or on the configuration (hotmoka/blueknot)
sed -i 's/@tendermint_version/0.34.15/g' ProgrammingBlueknot.md
sed -i 's/@takamaka_version/1.3.0/g' ProgrammingBlueknot.md
sed -i 's/@tool_repo/https:\/\/github.com\/Vero4Chain\/blueknot/g' ProgrammingBlueknot.md
sed -i 's/@tool/blue/g' ProgrammingBlueknot.md
sed -i 's/@Tool/Blue/g' ProgrammingBlueknot.md
sed -i 's/@app_repo/https:\/\/github.com\/Vero4Chain\/BlueknotAndroid/g' ProgrammingBlueknot.md
sed -i 's/@app_id_play/it.vero4chain.android.bluewally/g' ProgrammingBlueknot.md
sed -i 's/@app/bluewally/g' ProgrammingBlueknot.md
sed -i 's/@App/Bluewally/g' ProgrammingBlueknot.md
sed -i 's/@type/blueknot/g' ProgrammingBlueknot.md
sed -i 's/@Type/Blueknot/g' ProgrammingBlueknot.md
sed -i 's/@docker_hub_user/veroforchain/g' ProgrammingBlueknot.md
sed -i 's/@docker_user/blueknot/g' ProgrammingBlueknot.md
sed -i 's/@hotwallet_repo/https:\/\/github.com\/Vero4Chain\/hotwallet-browser/g' ProgrammingBlueknot.md
sed -i 's/@hotweb3_repo/https:\/\/github.com\/Vero4Chain\/hotweb3/g' ProgrammingBlueknot.md
sed -i 's/@fausto_email/fausto.spoto@vero4chain.it/g' ProgrammingBlueknot.md
sed -i 's/@tutorial_repo/https:\/\/github.com\/Vero4Chain\/blueknot_tutorial.git/g' ProgrammingBlueknot.md
sed -i 's/@tutorial_name/blueknot_tutorial/g' ProgrammingBlueknot.md
sed -i 's/@account_mokito/701e20be588db820744df467826d67b9fe451406d7f75da6ef8aeb6805a7365f#0/g' ProgrammingBlueknot.md
sed -i 's/@36words_of_account_mokito/word #1: rail\nword #2: double\nword #3: bag\nword #4: dove\nword #5: fluid\n...\nword #34: bounce\nword #35: deposit\nword #36: hotel/g' ProgrammingBlueknot.md

cp ProgrammingBlueknot.md temp.md
sed -i "/^\[PDFonly]:/d" ProgrammingBlueknot.md
sed -i "s/\[Markdownonly]://g" ProgrammingBlueknot.md

# generate the PDF version now
sed -i "/^\[Markdownonly]:/d" temp.md
sed -i "s/\[PDFonly]://g" temp.md
pandoc temp.md -o ProgrammingBlueknot.tex --include-in-header mystylefile_blueknot.tex --include-after-body backcover.tex --toc --highlight-style=kate -V geometry:a4paper -V documentclass:book -V pagestyle:headings -V papersize:a4 -V colorlinks:true
rm temp.md
sed -i 's/\\begin{verbatim}/\\begin{myverbatim}\n\\begin{verbatim}/g' ProgrammingBlueknot.tex
sed -i 's/\\end{verbatim}/\\end{verbatim}\n\\end{myverbatim}/g' ProgrammingBlueknot.tex
sed -i 's/103 \& alicudi/$10^3$ \& alicudi/g' ProgrammingBlueknot.tex
sed -i 's/106 \& filicudi/$10^6$ \& filicudi/g' ProgrammingBlueknot.tex
sed -i 's/109 \& stromboli/$10^9$ \& stromboli/g' ProgrammingBlueknot.tex
sed -i 's/1012 \& vulcano/$10^{12}$ \& vulcano/g' ProgrammingBlueknot.tex
sed -i 's/1015 \& salina/$10^{15}$ \& salina/g' ProgrammingBlueknot.tex
sed -i 's/1018 \& lipari/$10^{18}$ \& lipari/g' ProgrammingBlueknot.tex
sed -i 's/1021 \& moka/$10^{21}$ \& moka/g' ProgrammingBlueknot.tex
sed -i 's/\\chapterfont{\\clearpage}//g' ProgrammingBlueknot.tex
sed -i 's/\\chapter{Table of Contents}/\\begin{comment}\\chapter{Table of Contents}/g' ProgrammingBlueknot.tex
sed -i 's/\\hypertarget{introduction}/\\end{comment}\n\n\\hypertarget{introduction}/g' ProgrammingBlueknot.tex

# delete the \begin{document}
sed -i 's/\\begin{document}//g' ProgrammingBlueknot.tex
# place \begin{document} before \BgThispage
sed -i 's/\\BgThispage/\\begin{document}\n\\BgThispage/g' ProgrammingBlueknot.tex

pdflatex ProgrammingBlueknot.tex

# mv ProgrammingBlueknot.md ../README.md

# generate the epub version of the document
# we remove the first lines of the Markdown, that contain Java build information
tail -n +6 ../README.md > ProgrammingBlueknot.md
pandoc -o ProgrammingBlueknot.epub metadata-blueknot.yaml ProgrammingBlueknot.md
rm ProgrammingBlueknot.md

# generate the mobi version of the document
ebook-convert ProgrammingBlueknot.epub ProgrammingBlueknot.mobi

rm Tutorial.md
