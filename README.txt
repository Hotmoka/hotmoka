In order to run the comparison of ERC20 contracts with snapshots,
you should compile and run that specific test, by using Maven:

mvn clean install -DskipTests
cd io-hotmoka-tests
mvn test -Dtest=io.hotmoka.tests.ExampleCoinSnapshotPerformance

The last command will print on the screen the results of the experiment
and collect all data inside the erc20_snapshots_comparison.tex Latex figure.
