/*
Copyright 2023 Fausto Spoto

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package io.hotmoka.node.internal.nodes;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import com.moandjiezana.toml.Toml;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.crypto.Base64ConversionException;
import io.hotmoka.crypto.api.SignatureAlgorithm;
import io.hotmoka.node.AbstractConsensusConfig;
import io.hotmoka.node.AbstractConsensusConfigBuilder;
import io.hotmoka.node.api.nodes.ValidatorsConsensusConfig;
import io.hotmoka.node.api.nodes.ValidatorsConsensusConfigBuilder;
import io.hotmoka.node.internal.nodes.ValidatorsConsensusConfigImpl.ValidatorsConsensusConfigBuilderImpl;

/**
 * Implementation of the consensus parameters of a Hotmoka node that uses validators.
 * This information is typically contained in the manifest of the node.
 * 
 * @param <C> the type of this consensus configuration
 * @param <B> the type of the builder of this consensus configuration
 */
@Immutable
public abstract class ValidatorsConsensusConfigImpl<C extends ValidatorsConsensusConfigImpl<C,B>, B extends ValidatorsConsensusConfigBuilderImpl<C,B>>
		extends AbstractConsensusConfig<C,B>
		implements ValidatorsConsensusConfig<C,B> {

	/**
	 * The amount of validators' rewards that gets staked. The rest is sent to the validators immediately.
	 * 1000000 = 1%. It defaults to 75%.
	 */
	public final int percentStaked;

	/**
	 * Extra tax paid when a validator acquires the shares of another validator
	 * (in percent of the offer cost). 1000000 = 1%. It defaults to 50%.
	 */
	public final int buyerSurcharge;

	/**
	 * The percent of stake that gets slashed for each misbehaving validator. 1000000 means 1%.
	 * It defaults to 1%.
	 */
	public final int slashingForMisbehaving;

	/**
	 * The percent of stake that gets slashed for validators that do not behave
	 * (or do not vote). 1000000 means 1%. It defaults to 0.5%.
	 */
	public final int slashingForNotBehaving;

	/**
	 * Full constructor for the builder pattern.
	 * 
	 * @param builder the builder where information is extracted from
	 */
	public ValidatorsConsensusConfigImpl(ValidatorsConsensusConfigBuilderImpl<C,B> builder) {
		super(builder);

		this.percentStaked = builder.percentStaked;
		this.buyerSurcharge = builder.buyerSurcharge;
		this.slashingForMisbehaving = builder.slashingForMisbehaving;
		this.slashingForNotBehaving = builder.slashingForNotBehaving;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof ValidatorsConsensusConfigImpl<?,?> vcci && super.equals(other) &&
			percentStaked == vcci.percentStaked &&
			buyerSurcharge == vcci.buyerSurcharge &&
			slashingForMisbehaving == vcci.slashingForMisbehaving &&
			slashingForNotBehaving == vcci.slashingForNotBehaving;
	}

	@Override
	public String toToml() {
		var sb = new StringBuilder(super.toToml());

		sb.append("\n");
		sb.append("# the amount of validators' rewards that gets staked. The rest is sent to the validators\n");
		sb.append("# immediately. 1000000 means 1%\n");
		sb.append("percent_staked = " + percentStaked + "\n");
		sb.append("\n");
		sb.append("# extra tax paid when a validator acquires the shares of another validator\n");
		sb.append("# (in percent of the offer cost). 1000000 means 1%\n");
		sb.append("buyer_surcharge = " + buyerSurcharge + "\n");
		sb.append("\n");
		sb.append("# the percent of stake that gets slashed for each misbehaving validator. 1000000 means 1%\n");
		sb.append("slashing_for_misbehaving = " + slashingForMisbehaving + "\n");
		sb.append("\n");
		sb.append("# the percent of stake that gets slashed for each validator that does not behave\n");
		sb.append("# (or does not vote). 1000000 means 1%\n");
		sb.append("slashing_for_not_behaving = " + slashingForNotBehaving + "\n");

		return sb.toString();
	}

	@Override
	public int getPercentStaked() {
		return percentStaked;
	}

	@Override
	public int getBuyerSurcharge() {
		return buyerSurcharge;
	}

	@Override
	public int getSlashingForMisbehaving() {
		return slashingForMisbehaving;
	}

	@Override
	public int getSlashingForNotBehaving() {
		return slashingForNotBehaving;
	}

	/**
	 * The builder of a configuration object.
	 * 
	 * @param <C> the type of the consensus configuration
	 * @param <B> the type of this builder of the consensus configuration
	 */
	public abstract static class ValidatorsConsensusConfigBuilderImpl<C extends ValidatorsConsensusConfigImpl<C,B>, B extends ValidatorsConsensusConfigBuilderImpl<C,B>>
			extends AbstractConsensusConfigBuilder<C,B>
			implements ValidatorsConsensusConfigBuilder<C,B> {

		private int percentStaked = 75_000_000;
		private int buyerSurcharge = 50_000_000;
		private int slashingForMisbehaving = 1_000_000;
		private int slashingForNotBehaving = 500_000;

		/**
		 * Creates a builder with default values for the properties.
		 * 
		 * @throws NoSuchAlgorithmException if some cryptographic algorithm is not available
		 */
		protected ValidatorsConsensusConfigBuilderImpl() throws NoSuchAlgorithmException {}

		/**
		 * Creates a builder containing default data. but for the given signature.
		 * 
		 * @param signatureForRequests the signature algorithm to use for signing the requests
		 * @return the builder
		 */
		protected ValidatorsConsensusConfigBuilderImpl(SignatureAlgorithm signatureForRequests) {
			super(signatureForRequests);
		}

		/**
		 * Creates a builder with properties initialized to those of the given configuration object.
		 * 
		 * @param config the configuration object
		 */
		protected ValidatorsConsensusConfigBuilderImpl(C config) {
			super(config);

			setBuyerSurcharge(config.getBuyerSurcharge());
			setPercentStaked(config.getPercentStaked());
			setSlashingForMisbehaving(config.getSlashingForMisbehaving());
			setSlashingForNotBehaving(config.getSlashingForNotBehaving());
		}

		/**
		 * Reads the properties of the given TOML file and sets them for
		 * the corresponding fields of this builder.
		 * 
		 * @param toml the file
		 * @throws NoSuchAlgorithmException if some cryptographic algorithm in the TOML file is not available
		 * @throws Base64ConversionException if some public key in the TOML file is not correctly Base64-encoded
		 * @throws InvalidKeySpecException if the specification of some public key in the TOML file is illegal
		 * @throws InvalidKeyException if some public key in the TOML file is invalid
		 */
		protected ValidatorsConsensusConfigBuilderImpl(Toml toml) throws NoSuchAlgorithmException, InvalidKeySpecException, Base64ConversionException, InvalidKeyException {
			super(toml);

			var percentStaked = toml.getLong("percent_staked");
			if (percentStaked != null)
				setPercentStaked((long) percentStaked);

			var buyerSurcharge = toml.getLong("buyer_surcharge");
			if (buyerSurcharge != null)
				setBuyerSurcharge((long) buyerSurcharge);

			var slashingForMisbehaving = toml.getLong("slashing_for_misbehaving");
			if (slashingForMisbehaving != null)
				setSlashingForMisbehaving((long) slashingForMisbehaving);

			var slashingForNotBehaving = toml.getLong("slashing_for_not_behaving");
			if (slashingForNotBehaving != null)
				setSlashingForNotBehaving((long) slashingForNotBehaving);
		}

		@Override
		public B setPercentStaked(int percentStaked) {
			return setPercentStaked((long) percentStaked);
		}

		private B setPercentStaked(long percentStaked) {
			if (percentStaked < 0 || percentStaked > 100_000_000L)
				throw new IllegalArgumentException("percentStaked must be between 0 and 100_000_000");

			this.percentStaked = (int) percentStaked;
			return getThis();
		}

		@Override
		public B setBuyerSurcharge(int buyerSurcharge) {
			return setBuyerSurcharge((long) buyerSurcharge);
		}

		private B setBuyerSurcharge(long buyerSurcharge) {
			if (buyerSurcharge < 0 || buyerSurcharge > 100_000_000L)
				throw new IllegalArgumentException("buyerSurcharge must be between 0 and 100_000_000");

			this.buyerSurcharge = (int) buyerSurcharge;
			return getThis();
		}

		@Override
		public B setSlashingForMisbehaving(int slashingForMisbehaving) {
			return setSlashingForMisbehaving((long) slashingForMisbehaving);
		}

		private B setSlashingForMisbehaving(long slashingForMisbehaving) {
			if (slashingForMisbehaving < 0 || slashingForMisbehaving > 100_000_000L)
				throw new IllegalArgumentException("slashingForMisbehaving must be between 0 and 100_000_000");

			this.slashingForMisbehaving = (int) slashingForMisbehaving;
			return getThis();
		}

		@Override
		public B setSlashingForNotBehaving(int slashingForNotBehaving) {
			return setSlashingForNotBehaving((long) slashingForNotBehaving);
		}

		private B setSlashingForNotBehaving(long slashingForNotBehaving) {
			if (slashingForNotBehaving < 0 || slashingForNotBehaving > 100_000_000L)
				throw new IllegalArgumentException("slashingForNotBehaving must be between 0 and 100_000_000");

			this.slashingForNotBehaving = (int) slashingForNotBehaving;
			return getThis();
		}
	}
}