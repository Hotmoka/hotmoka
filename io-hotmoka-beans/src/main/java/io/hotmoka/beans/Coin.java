/*
Copyright 2021 Fausto Spoto

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

package io.hotmoka.beans;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * The coins of the Takamaka language. They are, in increasing order of magnitude:
 * 
 * <ul>
 * <li>panarea aka pana (level 1)</li>
 * <li>alicudi aka ali (=1000 panareas) (level 2)</li>
 * <li>filicudi aka fili (=1000 alicudi) (level 3)</li>
 * <li>stromboli aka strom (=1000 filicudis) (level 4)</li>
 * <li>vulcano aka vul (=1000 strombolis) (level 5)</li>
 * <li>salina aka sali (=1000 vulcanos) (level 6)</li>
 * <li>lipari aka lipa (=1000 salinas) (level 7)</li>
 * <li>moka (=1000 liparis) (level 8)</li>
 * </ul>
 */
public enum Coin {

	PANAREA,
	ALICUDI,
	FILICUDI,
	STROMBOLI,
	VULCANO,
	SALINA,
	LIPARI,
	MOKA;

	/**
	 * Yields the level of this unit.
	 * 
	 * @return the level of this unit, between 1 and 8, inclusive
	 */
	public int level() {
		return ordinal() + 1;
	}

	/**
	 * Yields the panareas corresponding to the given amount of panareas.
	 * Panareas are level 1 coins.
	 * 
	 * @param howMany the amount
	 * @return the number of panareas corresponding to the coins
	 */
	public static BigInteger panarea(BigInteger howMany) {
		return howMany;
	}

	/**
	 * Yields the panareas corresponding to the given amount of panareas.
	 * Panareas are level 1 coins.
	 * 
	 * @param howMany the amount
	 * @return the number of panareas corresponding to the coins
	 */
	public static BigInteger level1(BigInteger howMany) {
		return howMany;
	}

	/**
	 * Yields the panareas corresponding to the given amount of panareas.
	 * Panareas are level 1 coins.
	 *
	 * @param howMany the amount
	 * @return the number of panareas corresponding to the coins
	 */
	public static BigInteger pana(BigInteger howMany) {
		return panarea(howMany);
	}

	/**
	 * Yields the panareas corresponding to the given amount of panareas.
	 * Panareas are level 1 coins.
	 * 
	 * @param howMany the amount
	 * @return the number of panareas corresponding to the coins
	 */
	public static BigInteger panarea(BigDecimal howMany) {
		return howMany.toBigInteger();
	}

	/**
	 * Yields the panareas corresponding to the given amount of panareas.
	 * Panareas are level 1 coins.
	 * 
	 * @param howMany the amount
	 * @return the number of panareas corresponding to the coins
	 */
	public static BigInteger level1(BigDecimal howMany) {
		return panarea(howMany);
	}

	/**
	 * Yields the panareas corresponding to the given amount of panareas.
	 * Panareas are level 1 coins.
	 *
	 * @param howMany the amount
	 * @return the number of panareas corresponding to the coins
	 */
	public static BigInteger pana(BigDecimal howMany) {
		return panarea(howMany);
	}

	/**
	 * Yields the panareas corresponding to the given amount of panareas.
	 * Panareas are level 1 coins.
	 * 
	 * @param howMany the amount
	 * @return the number of panareas corresponding to the coins
	 */
	public static BigInteger panarea(long howMany) {
		return BigInteger.valueOf(howMany);
	}

	/**
	 * Yields the panareas corresponding to the given amount of panareas.
	 * Panareas are level 1 coins.
	 * 
	 * @param howMany the amount
	 * @return the number of panareas corresponding to the coins
	 */
	public static BigInteger level1(long howMany) {
		return BigInteger.valueOf(howMany);
	}

	/**
	 * Yields the panareas corresponding to the given amount of panareas.
	 * Panareas are level 1 coins.
	 * 
	 * @param howMany the amount
	 * @return the number of panareas corresponding to the coins
	 */
	public static BigInteger pana(long howMany) {
		return panarea(howMany);
	}

	/**
	 * Yields the panareas corresponding to the given amount of alicudis.
	 * Alicudis are level 2 coins.
	 * 
	 * @param howMany the amount
	 * @return the number of panareas corresponding to the coins
	 */
	public static BigInteger alicudi(BigInteger howMany) {
		return howMany.multiply(BigInteger.valueOf(1_000L));
	}

	/**
	 * Yields the panareas corresponding to the given amount of alicudis.
	 * Alicudis are level 2 coins.
	 * 
	 * @param howMany the amount
	 * @return the number of panareas corresponding to the coins
	 */
	public static BigInteger level2(BigInteger howMany) {
		return howMany.multiply(BigInteger.valueOf(1_000L));
	}

	/**
	 * Yields the panareas corresponding to the given amount of alicudis.
	 * Alicudis are level 2 coins.
	 * 
	 * @param howMany the amount
	 * @return the number of panareas corresponding to the coins
	 */
	public static BigInteger ali(BigInteger howMany) {
		return alicudi(howMany);
	}

	/**
	 * Yields the panareas corresponding to the given amount of alicudis.
	 * Alicudis are level 2 coins.
	 * 
	 * @param howMany the amount
	 * @return the number of panareas corresponding to the coins
	 */
	public static BigInteger alicudi(BigDecimal howMany) {
		return howMany.multiply(BigDecimal.valueOf(1_000L)).toBigInteger();
	}

	/**
	 * Yields the panareas corresponding to the given amount of alicudis.
	 * Alicudis are level 2 coins.
	 * 
	 * @param howMany the amount
	 * @return the number of panareas corresponding to the coins
	 */
	public static BigInteger level2(BigDecimal howMany) {
		return alicudi(howMany);
	}

	/**
	 * Yields the panareas corresponding to the given amount of alicudis.
	 * Alicudis are level 2 coins.
	 * 
	 * @param howMany the amount
	 * @return the number of panareas corresponding to the coins
	 */
	public static BigInteger ali(BigDecimal howMany) {
		return alicudi(howMany);
	}

	/**
	 * Yields the panareas corresponding to the given amount of alicudis.
	 * Alicudis are level 2 coins.
	 * 
	 * @param howMany the amount
	 * @return the number of panareas corresponding to the coins
	 */
	public static BigInteger alicudi(long howMany) {
		return BigInteger.valueOf(howMany).multiply(BigInteger.valueOf(1_000L));
	}

	/**
	 * Yields the panareas corresponding to the given amount of alicudis.
	 * Alicudis are level 2 coins.
	 * 
	 * @param howMany the amount
	 * @return the number of panareas corresponding to the coins
	 */
	public static BigInteger level2(long howMany) {
		return BigInteger.valueOf(howMany).multiply(BigInteger.valueOf(1_000L));
	}

	/**
	 * Yields the panareas corresponding to the given amount of alicudis.
	 * Alicudis are level 2 coins.
	 * 
	 * @param howMany the amount
	 * @return the number of panareas corresponding to the coins
	 */
	public static BigInteger ali(long howMany) {
		return alicudi(howMany);
	}

	/**
	 * Yields the panareas corresponding to the given amount of filicudis.
	 * Filicudis are level 3 coins.
	 * 
	 * @param howMany the amount
	 * @return the number of panareas corresponding to the coins
	 */
	public static BigInteger filicudi(BigInteger howMany) {
		return howMany.multiply(BigInteger.valueOf(1_000_000L));
	}

	/**
	 * Yields the panareas corresponding to the given amount of filicudis.
	 * Filicudis are level 3 coins.
	 * 
	 * @param howMany the amount
	 * @return the number of panareas corresponding to the coins
	 */
	public static BigInteger level3(BigInteger howMany) {
		return howMany.multiply(BigInteger.valueOf(1_000_000L));
	}

	/**
	 * Yields the panareas corresponding to the given amount of filicudis.
	 * Filicudis are level 3 coins.
	 * 
	 * @param howMany the amount
	 * @return the number of panareas corresponding to the coins
	 */
	public static BigInteger fili(BigInteger howMany) {
		return filicudi(howMany);
	}

	/**
	 * Yields the panareas corresponding to the given amount of filicudis.
	 * Filicudis are level 3 coins.
	 * 
	 * @param howMany the amount
	 * @return the number of panareas corresponding to the coins
	 */
	public static BigInteger filicudi(BigDecimal howMany) {
		return howMany.multiply(BigDecimal.valueOf(1_000_000L)).toBigInteger();
	}

	/**
	 * Yields the panareas corresponding to the given amount of filicudis.
	 * Filicudis are level 3 coins.
	 * 
	 * @param howMany the amount
	 * @return the number of panareas corresponding to the coins
	 */
	public static BigInteger level3(BigDecimal howMany) {
		return filicudi(howMany);
	}

	/**
	 * Yields the panareas corresponding to the given amount of filicudis.
	 * Filicudis are level 3 coins.
	 * 
	 * @param howMany the amount
	 * @return the number of panareas corresponding to the coins
	 */
	public static BigInteger fili(BigDecimal howMany) {
		return filicudi(howMany);
	}

	/**
	 * Yields the panareas corresponding to the given amount of filicudis.
	 * Filicudis are level 3 coins.
	 * 
	 * @param howMany the amount
	 * @return the number of panareas corresponding to the coins
	 */
	public static BigInteger filicudi(long howMany) {
		return BigInteger.valueOf(howMany).multiply(BigInteger.valueOf(1_000_000L));
	}

	/**
	 * Yields the panareas corresponding to the given amount of filicudis.
	 * Filicudis are level 3 coins.
	 * 
	 * @param howMany the amount
	 * @return the number of panareas corresponding to the coins
	 */
	public static BigInteger level3(long howMany) {
		return BigInteger.valueOf(howMany).multiply(BigInteger.valueOf(1_000_000L));
	}

	/**
	 * Yields the panareas corresponding to the given amount of filicudis.
	 * Filicudis are level 3 coins.
	 * 
	 * @param howMany the amount
	 * @return the number of panareas corresponding to the coins
	 */
	public static BigInteger fili(long howMany) {
		return filicudi(howMany);
	}

	/**
	 * Yields the panareas corresponding to the given amount of strombolis.
	 * Strombolis are level 4 coins.
	 * 
	 * @param howMany the amount
	 * @return the number of panareas corresponding to the coins
	 */
	public static BigInteger stromboli(BigInteger howMany) {
		return howMany.multiply(BigInteger.valueOf(1_000_000_000L));
	}

	/**
	 * Yields the panareas corresponding to the given amount of strombolis.
	 * Strombolis are level 4 coins.
	 * 
	 * @param howMany the amount
	 * @return the number of panareas corresponding to the coins
	 */
	public static BigInteger level4(BigInteger howMany) {
		return howMany.multiply(BigInteger.valueOf(1_000_000_000L));
	}

	/**
	 * Yields the panareas corresponding to the given amount of strombolis.
	 * Strombolis are level 4 coins.
	 * 
	 * @param howMany the amount
	 * @return the number of panareas corresponding to the coins
	 */
	public static BigInteger strom(BigInteger howMany) {
		return stromboli(howMany);
	}

	/**
	 * Yields the panareas corresponding to the given amount of strombolis.
	 * Strombolis are level 4 coins.
	 * 
	 * @param howMany the amount
	 * @return the number of panareas corresponding to the coins
	 */
	public static BigInteger stromboli(BigDecimal howMany) {
		return howMany.multiply(BigDecimal.valueOf(1_000_000_000L)).toBigInteger();
	}

	/**
	 * Yields the panareas corresponding to the given amount of strombolis.
	 * Strombolis are level 4 coins.
	 * 
	 * @param howMany the amount
	 * @return the number of panareas corresponding to the coins
	 */
	public static BigInteger level4(BigDecimal howMany) {
		return stromboli(howMany);
	}

	/**
	 * Yields the panareas corresponding to the given amount of strombolis.
	 * Strombolis are level 4 coins.
	 * 
	 * @param howMany the amount
	 * @return the number of panareas corresponding to the coins
	 */
	public static BigInteger strom(BigDecimal howMany) {
		return stromboli(howMany);
	}

	/**
	 * Yields the panareas corresponding to the given amount of strombolis.
	 * Strombolis are level 4 coins.
	 * 
	 * @param howMany the amount
	 * @return the number of panareas corresponding to the coins
	 */
	public static BigInteger stromboli(long howMany) {
		return BigInteger.valueOf(howMany).multiply(BigInteger.valueOf(1_000_000_000L));
	}

	/**
	 * Yields the panareas corresponding to the given amount of strombolis.
	 * Strombolis are level 4 coins.
	 * 
	 * @param howMany the amount
	 * @return the number of panareas corresponding to the coins
	 */
	public static BigInteger level4(long howMany) {
		return BigInteger.valueOf(howMany).multiply(BigInteger.valueOf(1_000_000_000L));
	}

	/**
	 * Yields the panareas corresponding to the given amount of strombolis.
	 * Strombolis are level 4 coins.
	 * 
	 * @param howMany the amount
	 * @return the number of panareas corresponding to the coins
	 */
	public static BigInteger strom(long howMany) {
		return stromboli(howMany);
	}

	/**
	 * Yields the panareas corresponding to the given amount of vulcanos.
	 * Vulcanos are level 5 coins.
	 * 
	 * @param howMany the amount
	 * @return the number of panareas corresponding to the coins
	 */
	public static BigInteger vulcano(BigInteger howMany) {
		return howMany.multiply(BigInteger.valueOf(1_000_000_000_000L));
	}

	/**
	 * Yields the panareas corresponding to the given amount of vulcanos.
	 * Vulcanos are level 5 coins.
	 * 
	 * @param howMany the amount
	 * @return the number of panareas corresponding to the coins
	 */
	public static BigInteger level5(BigInteger howMany) {
		return howMany.multiply(BigInteger.valueOf(1_000_000_000_000L));
	}

	/**
	 * Yields the panareas corresponding to the given amount of vulcanos.
	 * Vulcanos are level 5 coins.
	 * 
	 * @param howMany the amount
	 * @return the number of panareas corresponding to the coins
	 */
	public static BigInteger vul(BigInteger howMany) {
		return vulcano(howMany);
	}

	/**
	 * Yields the panareas corresponding to the given amount of vulcanos.
	 * Vulcanos are level 5 coins.
	 * 
	 * @param howMany the amount
	 * @return the number of panareas corresponding to the coins
	 */
	public static BigInteger vulcano(BigDecimal howMany) {
		return howMany.multiply(BigDecimal.valueOf(1_000_000_000_000L)).toBigInteger();
	}

	/**
	 * Yields the panareas corresponding to the given amount of vulcanos.
	 * Vulcanos are level 5 coins.
	 * 
	 * @param howMany the amount
	 * @return the number of panareas corresponding to the coins
	 */
	public static BigInteger level5(BigDecimal howMany) {
		return vulcano(howMany);
	}

	/**
	 * Yields the panareas corresponding to the given amount of vulcanos.
	 * Vulcanos are level 5 coins.
	 * 
	 * @param howMany the amount
	 * @return the number of panareas corresponding to the coins
	 */
	public static BigInteger vul(BigDecimal howMany) {
		return vulcano(howMany);
	}

	/**
	 * Yields the panareas corresponding to the given amount of vulcanos.
	 * Vulcanos are level 5 coins.
	 * 
	 * @param howMany the amount
	 * @return the number of panareas corresponding to the coins
	 */
	public static BigInteger vulcano(long howMany) {
		return BigInteger.valueOf(howMany).multiply(BigInteger.valueOf(1_000_000_000_000L));
	}
	
	/**
	 * Yields the panareas corresponding to the given amount of vulcanos.
	 * Vulcanos are level 5 coins.
	 * 
	 * @param howMany the amount
	 * @return the number of panareas corresponding to the coins
	 */
	public static BigInteger level5(long howMany) {
		return BigInteger.valueOf(howMany).multiply(BigInteger.valueOf(1_000_000_000_000L));
	}

	/**
	 * Yields the panareas corresponding to the given amount of vulcanos.
	 * Vulcanos are level 5 coins.
	 * 
	 * @param howMany the amount
	 * @return the number of panareas corresponding to the coins
	 */
	public static BigInteger vul(long howMany) {
		return vulcano(howMany);
	}

	/**
	 * Yields the panareas corresponding to the given amount of salinas.
	 * Salinas are level 6 coins.
	 * 
	 * @param howMany the amount
	 * @return the number of panareas corresponding to the coins
	 */
	public static BigInteger salina(BigInteger howMany) {
		return howMany.multiply(BigInteger.valueOf(1_000_000_000_000_000L));
	}

	/**
	 * Yields the panareas corresponding to the given amount of salinas.
	 * Salinas are level 6 coins.
	 * 
	 * @param howMany the amount
	 * @return the number of panareas corresponding to the coins
	 */
	public static BigInteger level6(BigInteger howMany) {
		return howMany.multiply(BigInteger.valueOf(1_000_000_000_000_000L));
	}

	/**
	 * Yields the panareas corresponding to the given amount of salinas.
	 * Salinas are level 6 coins.
	 * 
	 * @param howMany the amount
	 * @return the number of panareas corresponding to the coins
	 */
	public static BigInteger sali(BigInteger howMany) {
		return salina(howMany);
	}

	/**
	 * Yields the panareas corresponding to the given amount of salinas.
	 * Salinas are level 6 coins.
	 * 
	 * @param howMany the amount
	 * @return the number of panareas corresponding to the coins
	 */
	public static BigInteger salina(BigDecimal howMany) {
		return howMany.multiply(BigDecimal.valueOf(1_000_000_000_000_000L)).toBigInteger();
	}

	/**
	 * Yields the panareas corresponding to the given amount of salinas.
	 * Salinas are level 6 coins.
	 * 
	 * @param howMany the amount
	 * @return the number of panareas corresponding to the coins
	 */
	public static BigInteger level6(BigDecimal howMany) {
		return salina(howMany);
	}

	/**
	 * Yields the panareas corresponding to the given amount of salinas.
	 * Salinas are level 6 coins.
	 * 
	 * @param howMany the amount
	 * @return the number of panareas corresponding to the coins
	 */
	public static BigInteger sali(BigDecimal howMany) {
		return salina(howMany);
	}

	/**
	 * Yields the panareas corresponding to the given amount of salinas.
	 * Salinas are level 6 coins.
	 * 
	 * @param howMany the amount
	 * @return the number of panareas corresponding to the coins
	 */
	public static BigInteger salina(long howMany) {
		return BigInteger.valueOf(howMany).multiply(BigInteger.valueOf(1_000_000_000_000_000L));
	}

	/**
	 * Yields the panareas corresponding to the given amount of salinas.
	 * Salinas are level 6 coins.
	 * 
	 * @param howMany the amount
	 * @return the number of panareas corresponding to the coins
	 */
	public static BigInteger level6(long howMany) {
		return BigInteger.valueOf(howMany).multiply(BigInteger.valueOf(1_000_000_000_000_000L));
	}

	/**
	 * Yields the panareas corresponding to the given amount of salinas.
	 * Salinas are level 6 coins.
	 * 
	 * @param howMany the amount
	 * @return the number of panareas corresponding to the coins
	 */
	public static BigInteger sali(long howMany) {
		return salina(howMany);
	}

	/**
	 * Yields the panareas corresponding to the given amount of liparis.
	 * Liparis are level 7 coins.
	 * 
	 * @param howMany the amount
	 * @return the number of panareas corresponding to the coins
	 */
	public static BigInteger lipari(BigInteger howMany) {
		return howMany.multiply(BigInteger.valueOf(1_000_000_000_000_000_000L));
	}

	/**
	 * Yields the panareas corresponding to the given amount of liparis.
	 * Liparis are level 7 coins.
	 * 
	 * @param howMany the amount
	 * @return the number of panareas corresponding to the coins
	 */
	public static BigInteger level7(BigInteger howMany) {
		return howMany.multiply(BigInteger.valueOf(1_000_000_000_000_000_000L));
	}

	/**
	 * Yields the panareas corresponding to the given amount of liparis.
	 * Liparis are level 7 coins.
	 * 
	 * @param howMany the amount
	 * @return the number of panareas corresponding to the coins
	 */
	public static BigInteger lipa(BigInteger howMany) {
		return lipari(howMany);
	}

	/**
	 * Yields the panareas corresponding to the given amount of liparis.
	 * Liparis are level 7 coins.
	 * 
	 * @param howMany the amount
	 * @return the number of panareas corresponding to the coins
	 */
	public static BigInteger lipari(BigDecimal howMany) {
		return howMany.multiply(BigDecimal.valueOf(1_000_000_000_000_000_000L)).toBigInteger();
	}

	/**
	 * Yields the panareas corresponding to the given amount of liparis.
	 * Liparis are level 7 coins.
	 * 
	 * @param howMany the amount
	 * @return the number of panareas corresponding to the coins
	 */
	public static BigInteger level7(BigDecimal howMany) {
		return lipari(howMany);
	}

	/**
	 * Yields the panareas corresponding to the given amount of liparis.
	 * Liparis are level 7 coins.
	 * 
	 * @param howMany the amount
	 * @return the number of panareas corresponding to the coins
	 */
	public static BigInteger lipa(BigDecimal howMany) {
		return lipari(howMany);
	}

	/**
	 * Yields the panareas corresponding to the given amount of liparis.
	 * Liparis are level 7 coins.
	 * 
	 * @param howMany the amount
	 * @return the number of panareas corresponding to the coins
	 */
	public static BigInteger lipari(long howMany) {
		return BigInteger.valueOf(howMany).multiply(BigInteger.valueOf(1_000_000_000_000_000_000L));
	}

	/**
	 * Yields the panareas corresponding to the given amount of liparis.
	 * Liparis are level 7 coins.
	 * 
	 * @param howMany the amount
	 * @return the number of panareas corresponding to the coins
	 */
	public static BigInteger level7(long howMany) {
		return BigInteger.valueOf(howMany).multiply(BigInteger.valueOf(1_000_000_000_000_000_000L));
	}

	/**
	 * Yields the panareas corresponding to the given amount of liparis.
	 * Liparis are level 7 coins.
	 * 
	 * @param howMany the amount
	 * @return the number of panareas corresponding to the coins
	 */
	public static BigInteger lipa(long howMany) {
		return lipari(howMany);
	}

	/**
	 * Yields the panareas corresponding to the given amount of mokas.
	 * Mokas are level 8 coins.
	 * 
	 * @param howMany the amount
	 * @return the number of panareas corresponding to the coins
	 */
	public static BigInteger moka(BigInteger howMany) {
		return howMany.multiply(BigInteger.valueOf(1_000_000_000_000_000_000L).multiply(BigInteger.valueOf(1_000L)));
	}

	/**
	 * Yields the panareas corresponding to the given amount of mokas.
	 * Mokas are level 8 coins.
	 * 
	 * @param howMany the amount
	 * @return the number of panareas corresponding to the coins
	 */
	public static BigInteger level8(BigInteger howMany) {
		return howMany.multiply(BigInteger.valueOf(1_000_000_000_000_000_000L).multiply(BigInteger.valueOf(1_000L)));
	}

	/**
	 * Yields the panareas corresponding to the given amount of mokas.
	 * Mokas are level 8 coins.
	 * 
	 * @param howMany the amount
	 * @return the number of panareas corresponding to the coins
	 */
	public static BigInteger moka(BigDecimal howMany) {
		return howMany.multiply(BigDecimal.valueOf(1_000_000_000_000_000_000L).multiply(BigDecimal.valueOf(1_000L))).toBigInteger();
	}

	/**
	 * Yields the panareas corresponding to the given amount of mokas.
	 * Mokas are level 8 coins.
	 * 
	 * @param howMany the amount
	 * @return the number of panareas corresponding to the coins
	 */
	public static BigInteger level8(BigDecimal howMany) {
		return moka(howMany);
	}

	/**
	 * Yields the panareas corresponding to the given amount of mokas.
	 * Mokas are level 8 coins.
	 * 
	 * @param howMany the amount
	 * @return the number of panareas corresponding to the coins
	 */
	public static BigInteger moka(long howMany) {
		return BigInteger.valueOf(howMany).multiply(BigInteger.valueOf(1_000_000_000_000_000_000L).multiply(BigInteger.valueOf(1_000L)));
	}

	/**
	 * Yields the panareas corresponding to the given amount of mokas.
	 * Mokas are level 8 coins.
	 * 
	 * @param howMany the amount
	 * @return the number of panareas corresponding to the coins
	 */
	public static BigInteger level8(long howMany) {
		return BigInteger.valueOf(howMany).multiply(BigInteger.valueOf(1_000_000_000_000_000_000L).multiply(BigInteger.valueOf(1_000L)));
	}

	/**
	 * Yields the panareas corresponding to the given amount of coins.
	 * 
	 * @param level the level of the coins, between (1 for panareas and 8 for mokas)
	 * @param howMany the amount of coins
	 * @return the number of panareas corresponding to the coins
	 */
	public static BigInteger level(int level, BigDecimal howMany) {
		switch (level) {
		case 1: return howMany.toBigInteger();
		case 2: return howMany.multiply(BigDecimal.valueOf(1_000L)).toBigInteger();
		case 3: return howMany.multiply(BigDecimal.valueOf(1_000_000L)).toBigInteger();
		case 4: return howMany.multiply(BigDecimal.valueOf(1_000_000_000L)).toBigInteger();
		case 5: return howMany.multiply(BigDecimal.valueOf(1_000_000_000_000L)).toBigInteger();
		case 6: return howMany.multiply(BigDecimal.valueOf(1_000_000_000_000_000L)).toBigInteger();
		case 7: return howMany.multiply(BigDecimal.valueOf(1_000_000_000_000_000_000L)).toBigInteger();
		case 8: return howMany.multiply(BigDecimal.valueOf(1_000_000_000_000_000_000L)).multiply(BigDecimal.valueOf(1_000L)).toBigInteger();
		default: throw new IllegalArgumentException("level must be between 1 and 8 inclusive");
		}
	}
}