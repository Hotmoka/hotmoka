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
sed -i 's/@version/1.0.11/g' Tutorial.md
sed -i 's/@takamakaCode/af30834f57fb095049ea41f002c4d557078e620f55ae4c1271ebed5925aced4c/g' Tutorial.md
sed -i 's/@manifest/a4a8ff9618bf5e11d31ad5a733a381e88b60add8e2cefe449463ee666dd86d00#0/g' Tutorial.md
sed -i 's/@gamete/4f17633e4ef8b1fa4f9859799ad8fa2103b332cbb87b2d42058ba6f3d6d3784a#0/g' Tutorial.md
sed -i 's/@gasStation/a4a8ff9618bf5e11d31ad5a733a381e88b60add8e2cefe449463ee666dd86d00#14/g' Tutorial.md
sed -i 's/@validators/a4a8ff9618bf5e11d31ad5a733a381e88b60add8e2cefe449463ee666dd86d00#1/g' Tutorial.md
sed -i 's/@maxFaucet/10000000000000/g' Tutorial.md
sed -i 's/@chainid/marabunta/g' Tutorial.md
sed -i 's/@chainid/marabunta/g' state1_copy.fig
sed -i 's/@chainid/marabunta/g' state2_copy.fig
sed -i 's/@chainid/marabunta/g' state3_copy.fig
sed -i 's/@account1/b554954fe64c3c9b302664ff631f27ef64578692d14c99330b93c3844bb07470#0/g' Tutorial.md
sed -i 's/@short_account1/b554954fe64...#0/g' state2_copy.fig
sed -i 's/@short_account1/b554954fe64...#0/g' state3_copy.fig
sed -i 's/@36words_of_account1/ 1: noise\n 2: before\n 3: hockey\n 4: system\n 5: portion\n 6: pledge\n 7: banana\n 8: admit\n 9: first\n10: cereal\n11: busy\n12: kiwi\n13: fetch\n14: enjoy\n15: display\n16: sister\n17: develop\n18: sunset\n19: another\n20: size\n21: wage\n22: moon\n23: panic\n24: uncle\n25: fiscal\n26: cruel\n27: reform\n28: odor\n29: nasty\n30: magic\n31: excuse\n32: identify\n33: entry\n34: lonely\n35: bright\n36: strong/g' Tutorial.md
sed -i "s/@publickeyaccount1/1yro95TIJMHhBqJIwVt6oUvuooob+aagaBF54fXrwCw=/g" Tutorial.md
sed -i "s/@short_publickeyaccount1/1yro95TIJM.../g" state2_copy.fig
sed -i "s/@short_publickeyaccount1/1yro95TIJM.../g" state3_copy.fig
sed -i 's/@account_anonymous/e58: 4H8FcCXaadNQBiJPJu9QxjVrmTpSJGRKaDKJSmDu7wSp/g' Tutorial.md
sed -i 's/@new_key/4H8FcCXaadNQBiJPJu9QxjVrmTpSJGRKaDKJSmDu7wSp/g' Tutorial.md
sed -i 's/@family_address/ca7b9ec64bd160a5ce9f0086f97cba58c6c44c9408628094686c4cc21d162d59/g' Tutorial.md
sed -i 's/@short_family_address/ca7b9ec64b.../g' state3_copy.fig
sed -i 's/@code_family_address/9758c153c70470001cf6710fb78f249d342a04f540fb9eef7bed166bc60c5fe0/g' Tutorial.md
sed -i 's/@family2_address/16b6e1ea3dc2e130fcdd6e8960451412b1b7cdd601a9ea2926316c3db0f9c441/g' Tutorial.md
sed -i 's/@family_exported_address/b1ec20bc3c928ca15514793a07550f4a098dbd14fbd9071752ce8baf3d741691/g' Tutorial.md
sed -i 's/@family3_address/c1928f9898af4f39bdff477a2615eea5d81102ff723ec348107982cd782451f7/g' Tutorial.md
sed -i 's/@person_object/fda19242316704992c7a69ff411d18976df5aaa82474c245224a9c730a0dfe8d#0/g' Tutorial.md
sed -i 's/@person2_object/4de33af3626963abdc7ebac24df222af3c0f7e094549bc9d30d725c20a0d0cc6#0/g' Tutorial.md
sed -i 's/@person3_object/4e9e3d84b66af44f1cc293d6b0974364c222f499c3e00cfec19ff12f86268422#0/g' Tutorial.md
sed -i 's/@gradual_ponzi_address/6d4943fbda07dcd65d419475716aa2836af3561894d3deda69758f2f806201f0/g' Tutorial.md
sed -i 's/@gradual_ponzi_object/bb84d4a8920ff0e04bb24425a855b0a9a43fc9d929f1d80e148a7ca6c952b68f#0/g' Tutorial.md
sed -i 's/@gradual_ponzi_list/bb84d4a8920ff0e04bb24425a855b0a9a43fc9d929f1d80e148a7ca6c952b68f#1/g' Tutorial.md
sed -i 's/@gradual_ponzi_first/bb84d4a8920ff0e04bb24425a855b0a9a43fc9d929f1d80e148a7ca6c952b68f#2/g' Tutorial.md
sed -i 's/@gradual_ponzi_last/a9c2263f81703bc809daac587d2f1134a16d564ea204d9f192d8e4496778fd69#0/g' Tutorial.md
sed -i 's/@account2/d8ee1b7e0f7f30accc67c7faa123d9ad81fdc26de9c4ddc19419ac57ea5830e4#0/g' Tutorial.md
sed -i 's/@account3/0e27d8cd23b0c7c0c0601a197947a6370fdfb192c97cf3108a0c309b12bcda87#0/g' Tutorial.md
sed -i 's/@account4/a6ab3cd1c2f397e1a90d02d167c3b60d8d384ca47ac1fa98d32bf8d4678bdbb9#0/g' Tutorial.md
sed -i 's/@account5/c309ee6b5dbc4da835e022829c02e45ba333a064a13c164aeb303dc9e4cd9a45#0/g' Tutorial.md
sed -i 's/@account6/15322cf5b17d90225a8551fcd8d62419a4a46dc8ef0b07903e2de1a308cf9dc1#0/g' Tutorial.md
sed -i 's/@account7/9b9865000f8baa1c9049774f495f5470873cb3c1be8e1210fca1b65c919f77e8#0/g' Tutorial.md
sed -i 's/@publickeyaccount4/wgG92apxmGgTZBoPP7bvP8MN9zLrQdP16L6FaE0YNsQ=/g' Tutorial.md
sed -i 's/@short_publickeyaccount5/MIIDQzCCAjUGByqGSM44BAEwggIoAo.../g' Tutorial.md
sed -i 's/@tictactoe_address/79303c594ea0bd85ba9d09be77f3b3d4470b2282804847a6584c05ba85312cd6/g' Tutorial.md
sed -i 's/@tictactoe_object/e9f93ade95843827a2eccd4cf417f87a940c2f2913a3c6d46694457995dd8b2a#0/g' Tutorial.md
sed -i 's/@erc20_address/053acafea6d01a146341b903e9b99293fb666fa7cb4ac92906727183aa8987ef/g' Tutorial.md
sed -i 's/@erc20_object/f45b62005675190e166b8e66606a31abad253dba4b5019f52d3b5780bf64659a#0/g' Tutorial.md
sed -i 's/@server/panarea.hotmoka.io/g' Tutorial.md
sed -i 's/@new_docker_key/UM9smd5QEvsshZh5LLifusHwhPzJx69PeRCLT8PTTPw/g' Tutorial.md
sed -i 's/@container_id1/d5abac04cf5029421a25162cb31658da1f6efa57b372c655c4405321976d1414/g' Tutorial.md
sed -i 's/@docker_takamaka_code/36eb52143cc794e5f193caaa75cbf903069ee7cee2ca31d65a2f20c021b46783/g' Tutorial.md
sed -i 's/@docker_manifest/8c45b32aead6383c76a694197d8351451f9beeb7fc88549a30e10fc16b540bbe#0/g' Tutorial.md
sed -i 's/@docker_gamete/33291835386e72562167156e2b9717793a5b91b89a497b7792b842a96cb53d67#0/g' Tutorial.md
sed -i 's/@docker_validators/8c45b32aead6383c76a694197d8351451f9beeb7fc88549a30e10fc16b540bbe#1/g' Tutorial.md
sed -i 's/@docker_validator0/7eee815a9d60bfd4a197ad0bea956db07bbe7ed23642015560a18ca51e22700c#0/g' Tutorial.md
sed -i 's/@docker_id_validator0/01BA307C588511343F451C240101A72AF63877E4/g' Tutorial.md
sed -i 's/@container_id2/c71977b818736b2bc3416a6e2a2dec26a2fb013f9738fbb6d15e4cc566df8949/g' Tutorial.md
sed -i 's/@container_id3/53c3c1418d192fef44e0727087579b3650e54241c83d40874baeda204bcb0c1a/g' Tutorial.md
sed -i 's/@docker_new_account/db736d5087375c7cf7de12bdb18db80ecdac42df436904fead0f19529cdbabce#0/g' Tutorial.md
sed -i 's/@docker_total_gas_new_account/44857/g' Tutorial.md
sed -i 's/@docker_reduced_balance/999999998720576/g' Tutorial.md
sed -i 's/@docker_balance_validator0/11226/g' Tutorial.md
sed -i 's/@docker_staked_validator0/33675/g' Tutorial.md
sed -i 's/@docker_diff1/1279424/g' Tutorial.md
sed -i 's/@docker_diff2/44857/g' Tutorial.md
sed -i 's/@docker_diff3/44/g' Tutorial.md
sed -i 's/@docker_sum1/44901/g' Tutorial.md

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
