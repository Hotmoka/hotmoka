package io.hotmoka.node.api;

/**
 * The builder of a configuration object of a HOtmoka node that uses validators.
 * 
 * @param <T> the concrete type of the builder
 */
public interface ValidatorsConsensusConfigBuilder<T extends ValidatorsConsensusConfigBuilder<T>> extends ConsensusConfigBuilder<T>{

	/**
	 * Sets the amount of validators' rewards that gets staked. The rest is sent to the validators immediately.
	 * 1000000 = 1%. It defaults to 75%.
	 * 
	 * @param percentStaked the buyer surcharge to set
	 * @return this builder
	 */
	T setPercentStaked(int percentStaked);

	/**
	 * Sets the extra tax paid when a validator acquires the shares of another validator
	 * (in percent of the offer cost). 1000000 = 1%. It defaults to 50%.
	 * 
	 * @param buyerSurcharge the buyer surcharge to set
	 * @return this builder
	 */
	T setBuyerSurcharge(int buyerSurcharge);

	/**
	 * Sets the percent of stake that gets slashed for each misbehaving validator. 1000000 means 1%.
	 * It defaults to 1%.
	 * 
	 * @param slashingForMisbehaving the slashing for misbehaving validators
	 * @return this builder
	 */
	T setSlashingForMisbehaving(int slashingForMisbehaving);

	/**
	 * Sets the percent of stake that gets slashed for each not behaving (not voting) validator.
	 * 1000000 means 1%. It defaults to 1%.
	 * 
	 * @param slashingForNotBehaving the slashing for not behaving validators
	 * @return this builder
	 */
	T setSlashingForNotBehaving(int slashingForNotBehaving);

	@Override
	ValidatorsConsensusConfig build();
}