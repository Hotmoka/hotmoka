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
sed -i 's/@hotmoka_version/1.4.2/g' Tutorial.md
sed -i 's/@takamakaCode/1304b9ea0c85d7f5cec21a08b8fa36372b2fa646d8d245a531015a214e543d4e/g' Tutorial.md
sed -i 's/@manifest/65303c84e48691f2f36e313c22e60cf9938f372c2555d817423ac0d474dd0a0f#0/g' Tutorial.md
sed -i 's/@gamete/d9d1c62179695a7d70d3a04b83ff225f6fecec956347c321d24ff7f94313c4aa#0/g' Tutorial.md
sed -i 's/@gasStation/65303c84e48691f2f36e313c22e60cf9938f372c2555d817423ac0d474dd0a0f#14/g' Tutorial.md
sed -i 's/@validators/65303c84e48691f2f36e313c22e60cf9938f372c2555d817423ac0d474dd0a0f#1/g' Tutorial.md
sed -i 's/@maxFaucet/10000000000000/g' Tutorial.md
sed -i 's/@chainid/marabunta/g' Tutorial.md
sed -i 's/@chainid/marabunta/g' state1_copy.fig
sed -i 's/@chainid/marabunta/g' state2_copy.fig
sed -i 's/@chainid/marabunta/g' state3_copy.fig
sed -i 's/@account1/06221248e1e101c3c49994e08a423e8db1cf1e09ef9980b0a3f5b9dc11a081c4#0/g' Tutorial.md
sed -i 's/@short_account1/06221248e1e...#0/g' state2_copy.fig
sed -i 's/@short_account1/06221248e1e...#0/g' state3_copy.fig
sed -i 's/@36words_of_account1/ 1: entry\n 2: swap\n 3: desk\n 4: cake\n 5: deny\n 6: gift\n 7: ten\n 8: pigeon\n 9: flee\n10: genius\n11: swallow\n12: gravity\n13: giraffe\n14: ankle\n15: castle\n16: destroy\n17: achieve\n18: bullet\n19: chapter\n20: clap\n21: anger\n22: canal\n23: when\n24: renew\n25: someone\n26: utility\n27: know\n28: credit\n29: arch\n30: faint\n31: street\n32: describe\n33: balance\n34: advice\n35: illness\n36: puppy/g' Tutorial.md
sed -i "s/@publickeyaccount1/ABus\/uzf1xZMr0n2brSS2SMKvqRmZgMqR5TnS\/fTI2c=/g" Tutorial.md
sed -i "s/@short_publickeyaccount1/ABus\/uzf1x.../g" state2_copy.fig
sed -i "s/@short_publickeyaccount1/ABus\/uzf1x.../g" state3_copy.fig
sed -i 's/@account_anonymous/8173394e316ec56fa8a910ad14974fd1a82d35a3443f149222d046b9e87a213a#0/g' Tutorial.md
sed -i 's/@new_key/93zM3PPrCBbLuFc1zmjxeNtqsnCya6cFfH8g333KpzDx/g' Tutorial.md
sed -i 's/@family_address/d52a85ba061d4ce22ac3d2dd020c1bdfd6092fd1c3422d76655384b32832e812/g' Tutorial.md
sed -i 's/@short_family_address/d52a85ba06.../g' state3_copy.fig
sed -i 's/@code_family_address/964163ab4b97d1ef552c1fabebd48ab083afa7a6e1c79434b77c723539463093/g' Tutorial.md
sed -i 's/@family2_address/6859c0ac037bba2611c51889a46e040aad8501292166356430108b51036adf41/g' Tutorial.md
sed -i 's/@family_exported_address/310dc0c0d8075bc2deb2fc8aa05e599c55cc0c0a914087afa63cf839a97f8e3b/g' Tutorial.md
sed -i 's/@family3_address/703eb73f37ac0d365f19bdd48b279e03ba6b3da7ff028940c5aeee9cf1d30d4b/g' Tutorial.md
sed -i 's/@person_object/7ad11aa4a2b9d6af774bb77aeb65950566d4af8c5e4f654d2a616b951522c8dd#0/g' Tutorial.md
sed -i 's/@person2_object/b892a260407401395fb9cb611438970d61d1e0cb6ad11273e40b1871323e3f40#0/g' Tutorial.md
sed -i 's/@person3_object/a65bd680eea73ad62893e670e5d72c2409eb7965221b0d27cb687bedb389809d#0/g' Tutorial.md
sed -i 's/@gradual_ponzi_address/5f88e1917b67b7dff3f2e05c5cf92b60f37a3574757b794e00d0dea1518bf615/g' Tutorial.md
sed -i 's/@gradual_ponzi_object/0ded63da4890f2f89a37017c760158959b71a443e59fdc93f5aa6ff50c2b99fc#0/g' Tutorial.md
sed -i 's/@gradual_ponzi_list/0ded63da4890f2f89a37017c760158959b71a443e59fdc93f5aa6ff50c2b99fc#1/g' Tutorial.md
sed -i 's/@gradual_ponzi_first/0ded63da4890f2f89a37017c760158959b71a443e59fdc93f5aa6ff50c2b99fc#2/g' Tutorial.md
sed -i 's/@gradual_ponzi_last/8387bf9fe3ddbb960fb4298f2ddfaaadaaf3e1dd59cf0c5f1ea5dbe1d64d68c9#0/g' Tutorial.md
sed -i 's/@account2/b8a949ecf1a1d488870dde15c687bea6addb7d12cddbd3e63389e6edef3699d0#0/g' Tutorial.md
sed -i 's/@account3/7c46c6c5fc2d4e8c5c4897b00a249b97ff52339d995223af0963ece8c12dccc0#0/g' Tutorial.md
sed -i 's/@account4/c22e582c3db4b4d72bdf651e73ace0f1a6cdceba7e88e9227400e3bce942b4c8#0/g' Tutorial.md
sed -i 's/@account5/8d2c41847cf4451692ab4a8e8cb71274400b65145838c9e020d087beaeba0b66#0/g' Tutorial.md
sed -i 's/@account6/9618e5fae49c512d1d4f650efa99217e261b341fb956c966ef168fc2856bd130#0/g' Tutorial.md
sed -i 's/@account7/12d1174ddb0cf608ed13216f87a8fbf39cb691a68549fdc18c2f1af2a9eb1ab5#0/g' Tutorial.md
sed -i 's/@publickeyaccount4/fW7b9LG5o2eyjhM26ZvNY0pCs7G0IM7gwofg7mQUJyY=/g' Tutorial.md
sed -i 's/@short_publickeyaccount5/MIIDQjCCAjUGByqGSM44BAEwggIoAo.../g' Tutorial.md
sed -i 's/@tictactoe_address/bf2d9c0182352c95f9b61c1abe69a1443214af61a1885b4fc6a718d7c9427bc0/g' Tutorial.md
sed -i 's/@tictactoe_object/c77cb480fbc171691ea02bdd734fc59a14a536d27add9d631f63ac616ee2598a#0/g' Tutorial.md
sed -i 's/@erc20_address/e7cca566b70fdd65752944c903ebddeeeca9ce7dc07e8ceba150e5677d8dfb23/g' Tutorial.md
sed -i 's/@erc20_object/fc0b39f52e1c461cb4c209fcdeda60162c889585796b8734e0b61b4f3d2e3366#0/g' Tutorial.md
sed -i 's/@server/panarea.hotmoka.io/g' Tutorial.md
sed -i 's/@new_docker_key/5ShDFfJ3Hxb1YiDiSVh9Tu5XZxn34NnMQqka6EEjXd8r/g' Tutorial.md
sed -i 's/@container_id1/9697d0d8fe2dc02856959156cab6aae7d7a982fc4ae8ecf7d7b9cff5b2c245a8/g' Tutorial.md
sed -i 's/@docker_takamaka_code/1304b9ea0c85d7f5cec21a08b8fa36372b2fa646d8d245a531015a214e543d4e/g' Tutorial.md
sed -i 's/@docker_manifest/a92d4f27e3dca8c9e6adaf0cecb309410d302581324e9db36896cf33b59da3b2#0/g' Tutorial.md
sed -i 's/@docker_gamete/60b4faf74662af3644d14594d3c197fd50a14c84ccef3fea5f1c89243af04df8#0/g' Tutorial.md
sed -i 's/@docker_validators/a92d4f27e3dca8c9e6adaf0cecb309410d302581324e9db36896cf33b59da3b2#1/g' Tutorial.md
sed -i 's/@docker_validator0/f4006e028c4de9e82c449fd87d8a342dde3742e6de67d9977215b27bda7d4174#0/g' Tutorial.md
sed -i 's/@docker_id_validator0/FC86B7A29E277E3710C8991461D40F989BC28471/g' Tutorial.md
sed -i 's/@container_id2/d6cb775370665e578f307383d83cd9ef96613fed15fb7c590d3fc6c77625da8f/g' Tutorial.md
sed -i 's/@container_id3/8b229ec4db28c5df2e7e1f6799b22f8e92ea8b807af5f2353361a2fb8203dd31/g' Tutorial.md
sed -i 's/@docker_new_account/6cb8fba55c4d6729795c7f4982ea2f401c73e1614cf269f878d10536d5f6969b#0/g' Tutorial.md
sed -i 's/@docker_total_gas_new_account/2577/g' Tutorial.md
sed -i 's/@docker_reduced_balance/999999998762856/g' Tutorial.md
sed -i 's/@docker_balance_validator0/0/g' Tutorial.md
sed -i 's/@docker_staked_validator0/0/g' Tutorial.md
sed -i 's/@docker_diff1/1237144/g' Tutorial.md
sed -i 's/@docker_diff2/2577/g' Tutorial.md
sed -i 's/@docker_diff3/-2577/g' Tutorial.md
sed -i 's/@docker_sum1/0/g' Tutorial.md

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
