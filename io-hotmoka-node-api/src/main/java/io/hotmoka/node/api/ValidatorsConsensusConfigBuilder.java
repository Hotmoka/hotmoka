package io.hotmoka.node.api;

import io.hotmoka.beans.api.nodes.ConsensusConfigBuilder;

/**
 * The builder of a configuration object of a Hotmoka node that uses validators.
 * 
 * @param <C> the concrete type of the configuration
 * @param <B> the concrete type of the builder
 */
public interface ValidatorsConsensusConfigBuilder<C extends ValidatorsConsensusConfig<C,B>, B extends ValidatorsConsensusConfigBuilder<C,B>> extends ConsensusConfigBuilder<C,B>{

	/**
	 * Sets the amount of validators' rewards that gets staked. The rest is sent to the validators immediately.
	 * 1000000 = 1%. It defaults to 75%.
	 * 
	 * @param percentStaked the buyer surcharge to set
	 * @return this builder
	 */
	B setPercentStaked(int percentStaked);

	/**
	 * Sets the extra tax paid when a validator acquires the shares of another validator
	 * (in percent of the offer cost). 1000000 = 1%. It defaults to 50%.
	 * 
	 * @param buyerSurcharge the buyer surcharge to set
	 * @return this builder
	 */
	B setBuyerSurcharge(int buyerSurcharge);

	/**
	 * Sets the percent of stake that gets slashed for each misbehaving validator. 1000000 means 1%.
	 * It defaults to 1%.
	 * 
	 * @param slashingForMisbehaving the slashing for misbehaving validators
	 * @return this builder
	 */
	B setSlashingForMisbehaving(int slashingForMisbehaving);

	/**
	 * Sets the percent of stake that gets slashed for each not behaving (not voting) validator.
	 * 1000000 means 1%. It defaults to 1%.
	 * 
	 * @param slashingForNotBehaving the slashing for not behaving validators
	 * @return this builder
	 */
	B setSlashingForNotBehaving(int slashingForNotBehaving);
}