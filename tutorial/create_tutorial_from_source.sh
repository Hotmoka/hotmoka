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
sed -i 's/@hotmoka_version/1.4.3/g' Tutorial.md
sed -i 's/@takamakaCode/ced33b3dde5c99993528f4fe83e015ae0847c1fa2afaae47a6932fcd01fca4b1/g' Tutorial.md
sed -i 's/@manifest/b773fe0550b89f288414a16a3cb0841f2067533eaba09de6c74a82a2dbfdd2a8#0/g' Tutorial.md
sed -i 's/@gamete/c4277cbda401f8b491930a6c97e9df051a3f0c6ace14c6f21d528fec7b9f2751#0/g' Tutorial.md
sed -i 's/@gasStation/b773fe0550b89f288414a16a3cb0841f2067533eaba09de6c74a82a2dbfdd2a8#14/g' Tutorial.md
sed -i 's/@validators/b773fe0550b89f288414a16a3cb0841f2067533eaba09de6c74a82a2dbfdd2a8#1/g' Tutorial.md
sed -i 's/@maxFaucet/10000000000000/g' Tutorial.md
sed -i 's/@chainid/marabunta/g' Tutorial.md
sed -i 's/@chainid/marabunta/g' state1_copy.fig
sed -i 's/@chainid/marabunta/g' state2_copy.fig
sed -i 's/@chainid/marabunta/g' state3_copy.fig
sed -i 's/@account1/da5ceafc37c8e5fbf01c299b2ccd1deebcad79d1f37e8cd37bd7af0b3df6faf2#0/g' Tutorial.md
sed -i 's/@short_account1/da5ceafc37c...#0/g' state2_copy.fig
sed -i 's/@short_account1/da5ceafc37c...#0/g' state3_copy.fig
sed -i 's/@36words_of_account1/ 1: move\n 2: jazz\n 3: indicate\n 4: seek\n 5: child\n 6: banner\n 7: sadness\n 8: sand\n 9: country\n10: sad\n11: away\n12: exile\n13: place\n14: install\n15: valid\n16: weekend\n17: tourist\n18: test\n19: alpha\n20: farm\n21: ready\n22: grief\n23: jeans\n24: stuff\n25: pulp\n26: soldier\n27: lamp\n28: wonder\n29: snake\n30: sadness\n31: gadget\n32: thumb\n33: digital\n34: result\n35: royal\n36: rural/g' Tutorial.md
sed -i "s/@publickeyaccount1/j38M8fQ8sUXzTBFhFsFyV3B11s9Xjwigy6TE8h\/IEDc=/g" Tutorial.md
sed -i "s/@short_publickeyaccount1/j38M8fQ8sU.../g" state2_copy.fig
sed -i "s/@short_publickeyaccount1/j38M8fQ8sU.../g" state3_copy.fig
sed -i 's/@account_anonymous/c980b5701ba05895c1ed4ca31ac93e6e3163e90cf95d9d91716736d7785b83a3#0/g' Tutorial.md
sed -i 's/@new_key/DKRoopGmmzn6VSGR8AvtC15gHCjtwqYeWpyob7w2YZar/g' Tutorial.md
sed -i 's/@family_address/ecd2fede8472200f8fc0820a248445985d4ee45cc7462b6864e9fc57710e1f48/g' Tutorial.md
sed -i 's/@short_family_address/ecd2fede84.../g' state3_copy.fig
sed -i 's/@code_family_address/68aa761740db347ad9fb0bb2ae3cfa3e1989051a591cda4611740cda45287e1a/g' Tutorial.md
sed -i 's/@family2_address/13a25c8225d6650406c4470925bdc3b3027ab515ca5b011a0f27bf503ea594b4/g' Tutorial.md
sed -i 's/@family_exported_address/1cd736703ff0781a4a6aa045e7b55520ba4fc8a8209391401bcd45cccbd42478/g' Tutorial.md
sed -i 's/@family3_address/4fd4ee5810133597615fd53a4d50aa8ce609ac027c1c2ce528d79a7abeb04804/g' Tutorial.md
sed -i 's/@person_object/54239f919366a00ec15ede6061de8252caf096bc1724527c8173efbd38d2de6a#0/g' Tutorial.md
sed -i 's/@person2_object/fd021e0ec11732c55f2fbd9e480339ed10dafca6aeebea47a67d0abad2368e10#0/g' Tutorial.md
sed -i 's/@person3_object/f21090fc4d2072a75c82c67662d369a0af48d5899a5fd10832386fba2bafbb8b#0/g' Tutorial.md
sed -i 's/@gradual_ponzi_address/775ec579852725da25418ff5677513b24a5c1780f3d4cf95424b39be68c6faba/g' Tutorial.md
sed -i 's/@gradual_ponzi_object/2c99690e50aa75b38df27991cb65fc8431c1424cbe1e0bbf1f804af98dcbea5e#0/g' Tutorial.md
sed -i 's/@gradual_ponzi_list/2c99690e50aa75b38df27991cb65fc8431c1424cbe1e0bbf1f804af98dcbea5e#1/g' Tutorial.md
sed -i 's/@gradual_ponzi_first/2c99690e50aa75b38df27991cb65fc8431c1424cbe1e0bbf1f804af98dcbea5e#2/g' Tutorial.md
sed -i 's/@gradual_ponzi_last/398ebf2f871cb19cdb80d7fcd1ac01ed1f33d12b832b0277ac694bd6b3191dc4#0/g' Tutorial.md
sed -i 's/@account2/caa3a8d71ff2577c38947d7bb9565957dfc778b8904cade4deca718ced83ae49#0/g' Tutorial.md
sed -i 's/@account3/d85759af2a019fdd4b8f3c3a4bec40a8069df0c481679d4022386f8fffff09c8#0/g' Tutorial.md
sed -i 's/@account4/f827fd99b925b358bdcfcc9dd28cbe8838b7f32f81852f95c1f738e6b0efc593#0/g' Tutorial.md
sed -i 's/@account5/4b2dc2baa68940bcaf31649fa34c75dabb55bbab75f78c1d947cad2428073c4c#0/g' Tutorial.md
sed -i 's/@account6/d1b23a0df402bff103e4bd36e2ccabbfd89ec67803408ee59c5e5e7c42427580#0/g' Tutorial.md
sed -i 's/@account7/1b8412be2cdbdc4310ccd5b167d43706f8cd42638dd519465369caf37966d73f#0/g' Tutorial.md
sed -i 's/@publickeyaccount4/xsvCqG2w5WV1WTyZ3o5Dl7+r5kUF7ZO5ahk7xSGdbVI=/g' Tutorial.md
sed -i 's/@short_publickeyaccount5/MIIDQjCCAjUGByqGSM44BAEwggIoAo.../g' Tutorial.md
sed -i 's/@tictactoe_address/472976d486586feef25f7cdc78fac7d3cd910b5bb6d28dd058676ddb191e103c/g' Tutorial.md
sed -i 's/@tictactoe_object/fc71778362ef3db27137afeb8dfdb53a03f35f323dc171a6b38996bf754b2764#0/g' Tutorial.md
sed -i 's/@erc20_address/a699cd3917a8a6eacd7edab48fa5b993d643d58b628b10246c950e1aa0fd1626/g' Tutorial.md
sed -i 's/@erc20_object/2bdaa2554a8720418c0a17c00c43a6c5cfd8be011770ed13916da18c5428033f#0/g' Tutorial.md
sed -i 's/@server/panarea.hotmoka.io/g' Tutorial.md
sed -i 's/@new_docker_key/EHUnrQooCTDDWytAoMcDspSuUVprKdHyahNUUNWhydkj/g' Tutorial.md
sed -i 's/@container_id1/63887f9a9ecac5d48c0d3a302fee0eddfd06ac3a41c6dde6ac825e0f30fe7248/g' Tutorial.md
sed -i 's/@docker_takamaka_code/ced33b3dde5c99993528f4fe83e015ae0847c1fa2afaae47a6932fcd01fca4b1/g' Tutorial.md
sed -i 's/@docker_manifest/a8729f32a95220615275437fe9552d4e87b89a4ffa8803f6dcf99f244aa21c52#0/g' Tutorial.md
sed -i 's/@docker_gamete/d5f5d35e561edf06a864d48f5468898ae215b8e3dd0466f6fe4936f938a58acb#0/g' Tutorial.md
sed -i 's/@docker_validators/a8729f32a95220615275437fe9552d4e87b89a4ffa8803f6dcf99f244aa21c52#1/g' Tutorial.md
sed -i 's/@docker_validator0/9925a3df62d5c0e07dc351bdc6f074b1cf66e05ad4cf3e99c2f228ca5b22fb74#0/g' Tutorial.md
sed -i 's/@docker_id_validator0/DD81EECE7FF72FE3EB609733A07C5EEA552F9C1E/g' Tutorial.md
sed -i 's/@container_id2/09496e7f3b0bdca760c95eb2f5a4b4159db4189a7896b7ef4f2db1b8a4bd394f/g' Tutorial.md
sed -i 's/@container_id3/d8986ba3e3743655c87e2d3be5fdf5902ff61f79c199c1f6f08da8d4fffbea36/g' Tutorial.md
sed -i 's/@docker_new_account/581175caf42dbb094eaa5a0295aa3bd5cbfdf2c459546b6fe88c3d65fdd394b7#0/g' Tutorial.md
sed -i 's/@docker_total_gas_new_account/2577/g' Tutorial.md
sed -i 's/@docker_reduced_balance/999999998762856/g' Tutorial.md
sed -i 's/@docker_balance_validator0/645/g' Tutorial.md
sed -i 's/@docker_staked_validator0/1934/g' Tutorial.md
sed -i 's/@docker_diff1/1237144/g' Tutorial.md
sed -i 's/@docker_diff2/2577/g' Tutorial.md
sed -i 's/@docker_diff3/2/g' Tutorial.md
sed -i 's/@docker_sum1/2579/g' Tutorial.md

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
sed -i 's/@takamaka_version/1.1.0/g' ProgrammingHotmoka.md
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
sed -i 's/@takamaka_version/1.1.0/g' ProgrammingBlueknot.md
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
