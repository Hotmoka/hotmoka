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

package io.takamaka.code.lang;

import static java.math.BigInteger.valueOf;
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
public class Coin {

	/**
	 * Yields the given amount of panareas, as a {@link java.math.BigInteger}.
	 * Panareas are level 1 coins.
	 * 
	 * @param howMany the amount
	 * @return the amount, as a {@link java.math.BigInteger}
	 */
	public static BigInteger panarea(BigInteger howMany) {
		return howMany;
	}

	/**
	 * Yields the given amount of panareas, as a {@link java.math.BigInteger}.
	 * Panareas are level 1 coins.
	 * 
	 * @param howMany the amount
	 * @return the amount, as a {@link java.math.BigInteger}
	 */
	public static BigInteger level1(BigInteger howMany) {
		return howMany;
	}

	/**
	 * Yields the given amount of panareas, as a {@link java.math.BigInteger}.
	 * Panareas are level 1 coins.
	 *
	 * @param howMany the amount
	 * @return the amount, as a {@link java.math.BigInteger}
	 */
	public static BigInteger pana(BigInteger howMany) {
		return panarea(howMany);
	}

	/**
	 * Yields the given amount of panareas, as a {@link java.math.BigInteger}.
	 * Panareas are level 1 coins.
	 * 
	 * @param howMany the amount
	 * @return the amount, as a {@link java.math.BigInteger}
	 */
	public static BigInteger panarea(long howMany) {
		return valueOf(howMany);
	}

	/**
	 * Yields the given amount of panareas, as a {@link java.math.BigInteger}.
	 * Panareas are level 1 coins.
	 * 
	 * @param howMany the amount
	 * @return the amount, as a {@link java.math.BigInteger}
	 */
	public static BigInteger level1(long howMany) {
		return valueOf(howMany);
	}

	/**
	 * Yields the given amount of panareas, as a {@link java.math.BigInteger}.
	 * Panareas are level 1 coins.
	 * 
	 * @param howMany the amount
	 * @return the amount, as a {@link java.math.BigInteger}
	 */
	public static BigInteger pana(long howMany) {
		return panarea(howMany);
	}

	/**
	 * Yields the given amount of alicudis, as a {@link java.math.BigInteger}.
	 * Alicudis are level 2 coins.
	 * 
	 * @param howMany the amount
	 * @return the amount, as a {@link java.math.BigInteger}
	 */
	public static BigInteger alicudi(BigInteger howMany) {
		return howMany.multiply(valueOf(1_000L));
	}

	/**
	 * Yields the given amount of alicudis, as a {@link java.math.BigInteger}.
	 * Alicudis are level 2 coins.
	 * 
	 * @param howMany the amount
	 * @return the amount, as a {@link java.math.BigInteger}
	 */
	public static BigInteger level2(BigInteger howMany) {
		return howMany.multiply(valueOf(1_000L));
	}

	/**
	 * Yields the given amount of alicudis, as a {@link java.math.BigInteger}.
	 * Alicudis are level 2 coins.
	 * 
	 * @param howMany the amount
	 * @return the amount, as a {@link java.math.BigInteger}
	 */
	public static BigInteger ali(BigInteger howMany) {
		return alicudi(howMany);
	}

	/**
	 * Yields the given amount of alicudis, as a {@link java.math.BigInteger}.
	 * Alicudis are level 2 coins.
	 * 
	 * @param howMany the amount
	 * @return the amount, as a {@link java.math.BigInteger}
	 */
	public static BigInteger alicudi(long howMany) {
		return valueOf(howMany).multiply(valueOf(1_000L));
	}

	/**
	 * Yields the given amount of alicudis, as a {@link java.math.BigInteger}.
	 * Alicudis are level 2 coins.
	 * 
	 * @param howMany the amount
	 * @return the amount, as a {@link java.math.BigInteger}
	 */
	public static BigInteger level2(long howMany) {
		return valueOf(howMany).multiply(valueOf(1_000L));
	}

	/**
	 * Yields the given amount of alicudis, as a {@link java.math.BigInteger}.
	 * Alicudis are level 2 coins.
	 * 
	 * @param howMany the amount
	 * @return the amount, as a {@link java.math.BigInteger}
	 */
	public static BigInteger ali(long howMany) {
		return alicudi(howMany);
	}

	/**
	 * Yields the given amount of filicudis, as a {@link java.math.BigInteger}.
	 * Filicudis are level 3 coins.
	 * 
	 * @param howMany the amount
	 * @return the amount, as a {@link java.math.BigInteger}
	 */
	public static BigInteger filicudi(BigInteger howMany) {
		return howMany.multiply(valueOf(1_000_000L));
	}

	/**
	 * Yields the given amount of filicudis, as a {@link java.math.BigInteger}.
	 * Filicudis are level 3 coins.
	 * 
	 * @param howMany the amount
	 * @return the amount, as a {@link java.math.BigInteger}
	 */
	public static BigInteger level3(BigInteger howMany) {
		return howMany.multiply(valueOf(1_000_000L));
	}

	/**
	 * Yields the given amount of filicudis, as a {@link java.math.BigInteger}.
	 * Filicudis are level 3 coins.
	 * 
	 * @param howMany the amount
	 * @return the amount, as a {@link java.math.BigInteger}
	 */
	public static BigInteger fili(BigInteger howMany) {
		return filicudi(howMany);
	}

	/**
	 * Yields the given amount of filicudis, as a {@link java.math.BigInteger}.
	 * Filicudis are level 3 coins.
	 * 
	 * @param howMany the amount
	 * @return the amount, as a {@link java.math.BigInteger}
	 */
	public static BigInteger filicudi(long howMany) {
		return valueOf(howMany).multiply(valueOf(1_000_000L));
	}

	/**
	 * Yields the given amount of filicudis, as a {@link java.math.BigInteger}.
	 * Filicudis are level 3 coins.
	 * 
	 * @param howMany the amount
	 * @return the amount, as a {@link java.math.BigInteger}
	 */
	public static BigInteger level3(long howMany) {
		return valueOf(howMany).multiply(valueOf(1_000_000L));
	}

	/**
	 * Yields the given amount of filicudis, as a {@link java.math.BigInteger}.
	 * Filicudis are level 3 coins.
	 * 
	 * @param howMany the amount
	 * @return the amount, as a {@link java.math.BigInteger}
	 */
	public static BigInteger fili(long howMany) {
		return filicudi(howMany);
	}

	/**
	 * Yields the given amount of strombolis, as a {@link java.math.BigInteger}.
	 * Strombolis are level 4 coins.
	 * 
	 * @param howMany the amount
	 * @return the amount, as a {@link java.math.BigInteger}
	 */
	public static BigInteger stromboli(BigInteger howMany) {
		return howMany.multiply(valueOf(1_000_000_000L));
	}

	/**
	 * Yields the given amount of strombolis, as a {@link java.math.BigInteger}.
	 * Strombolis are level 4 coins.
	 * 
	 * @param howMany the amount
	 * @return the amount, as a {@link java.math.BigInteger}
	 */
	public static BigInteger level4(BigInteger howMany) {
		return howMany.multiply(valueOf(1_000_000_000L));
	}

	/**
	 * Yields the given amount of strombolis, as a {@link java.math.BigInteger}.
	 * Strombolis are level 4 coins.
	 * 
	 * @param howMany the amount
	 * @return the amount, as a {@link java.math.BigInteger}
	 */
	public static BigInteger strom(BigInteger howMany) {
		return stromboli(howMany);
	}

	/**
	 * Yields the given amount of strombolis, as a {@link java.math.BigInteger}.
	 * Strombolis are level 4 coins.
	 * 
	 * @param howMany the amount
	 * @return the amount, as a {@link java.math.BigInteger}
	 */
	public static BigInteger stromboli(long howMany) {
		return valueOf(howMany).multiply(valueOf(1_000_000_000L));
	}

	/**
	 * Yields the given amount of strombolis, as a {@link java.math.BigInteger}.
	 * Strombolis are level 4 coins.
	 * 
	 * @param howMany the amount
	 * @return the amount, as a {@link java.math.BigInteger}
	 */
	public static BigInteger level4(long howMany) {
		return valueOf(howMany).multiply(valueOf(1_000_000_000L));
	}

	/**
	 * Yields the given amount of strombolis, as a {@link java.math.BigInteger}.
	 * Strombolis are level 4 coins.
	 * 
	 * @param howMany the amount
	 * @return the amount, as a {@link java.math.BigInteger}
	 */
	public static BigInteger strom(long howMany) {
		return stromboli(howMany);
	}

	/**
	 * Yields the given amount of vulcanos, as a {@link java.math.BigInteger}.
	 * Vulcanos are level 5 coins.
	 * 
	 * @param howMany the amount
	 * @return the amount, as a {@link java.math.BigInteger}
	 */
	public static BigInteger vulcano(BigInteger howMany) {
		return howMany.multiply(valueOf(1_000_000_000_000L));
	}

	/**
	 * Yields the given amount of vulcanos, as a {@link java.math.BigInteger}.
	 * Vulcanos are level 5 coins.
	 * 
	 * @param howMany the amount
	 * @return the amount, as a {@link java.math.BigInteger}
	 */
	public static BigInteger level5(BigInteger howMany) {
		return howMany.multiply(valueOf(1_000_000_000_000L));
	}

	/**
	 * Yields the given amount of vulcanos, as a {@link java.math.BigInteger}.
	 * Vulcanos are level 5 coins.
	 * 
	 * @param howMany the amount
	 * @return the amount, as a {@link java.math.BigInteger}
	 */
	public static BigInteger vul(BigInteger howMany) {
		return vulcano(howMany);
	}

	/**
	 * Yields the given amount of vulcanos, as a {@link java.math.BigInteger}.
	 * Vulcanos are level 5 coins.
	 * 
	 * @param howMany the amount
	 * @return the amount, as a {@link java.math.BigInteger}
	 */
	public static BigInteger vulcano(long howMany) {
		return valueOf(howMany).multiply(valueOf(1_000_000_000_000L));
	}
	
	/**
	 * Yields the given amount of vulcanos, as a {@link java.math.BigInteger}.
	 * Vulcanos are level 5 coins.
	 * 
	 * @param howMany the amount
	 * @return the amount, as a {@link java.math.BigInteger}
	 */
	public static BigInteger level5(long howMany) {
		return valueOf(howMany).multiply(valueOf(1_000_000_000_000L));
	}

	/**
	 * Yields the given amount of vulcanos, as a {@link java.math.BigInteger}.
	 * Vulcanos are level 5 coins.
	 * 
	 * @param howMany the amount
	 * @return the amount, as a {@link java.math.BigInteger}
	 */
	public static BigInteger vul(long howMany) {
		return vulcano(howMany);
	}

	/**
	 * Yields the given amount of salinas, as a {@link java.math.BigInteger}.
	 * Salinas are level 6 coins.
	 * 
	 * @param howMany the amount
	 * @return the amount, as a {@link java.math.BigInteger}
	 */
	public static BigInteger salina(BigInteger howMany) {
		return howMany.multiply(valueOf(1_000_000_000_000_000L));
	}

	/**
	 * Yields the given amount of salinas, as a {@link java.math.BigInteger}.
	 * Salinas are level 6 coins.
	 * 
	 * @param howMany the amount
	 * @return the amount, as a {@link java.math.BigInteger}
	 */
	public static BigInteger level6(BigInteger howMany) {
		return howMany.multiply(valueOf(1_000_000_000_000_000L));
	}

	/**
	 * Yields the given amount of salinas, as a {@link java.math.BigInteger}.
	 * Salinas are level 6 coins.
	 * 
	 * @param howMany the amount
	 * @return the amount, as a {@link java.math.BigInteger}
	 */
	public static BigInteger sali(BigInteger howMany) {
		return salina(howMany);
	}

	/**
	 * Yields the given amount of salinas, as a {@link java.math.BigInteger}.
	 * Salinas are level 6 coins.
	 * 
	 * @param howMany the amount
	 * @return the amount, as a {@link java.math.BigInteger}
	 */
	public static BigInteger salina(long howMany) {
		return valueOf(howMany).multiply(valueOf(1_000_000_000_000_000L));
	}

	/**
	 * Yields the given amount of salinas, as a {@link java.math.BigInteger}.
	 * Salinas are level 6 coins.
	 * 
	 * @param howMany the amount
	 * @return the amount, as a {@link java.math.BigInteger}
	 */
	public static BigInteger level6(long howMany) {
		return valueOf(howMany).multiply(valueOf(1_000_000_000_000_000L));
	}

	/**
	 * Yields the given amount of salinas, as a {@link java.math.BigInteger}.
	 * Salinas are level 6 coins.
	 * 
	 * @param howMany the amount
	 * @return the amount, as a {@link java.math.BigInteger}
	 */
	public static BigInteger sali(long howMany) {
		return salina(howMany);
	}

	/**
	 * Yields the given amount of liparis, as a {@link java.math.BigInteger}.
	 * Liparis are level 7 coins.
	 * 
	 * @param howMany the amount
	 * @return the amount, as a {@link java.math.BigInteger}
	 */
	public static BigInteger lipari(BigInteger howMany) {
		return howMany.multiply(valueOf(1_000_000_000_000_000_000L));
	}

	/**
	 * Yields the given amount of liparis, as a {@link java.math.BigInteger}.
	 * Liparis are level 7 coins.
	 * 
	 * @param howMany the amount
	 * @return the amount, as a {@link java.math.BigInteger}
	 */
	public static BigInteger level7(BigInteger howMany) {
		return howMany.multiply(valueOf(1_000_000_000_000_000_000L));
	}

	/**
	 * Yields the given amount of liparis, as a {@link java.math.BigInteger}.
	 * Liparis are level 7 coins.
	 * 
	 * @param howMany the amount
	 * @return the amount, as a {@link java.math.BigInteger}
	 */
	public static BigInteger lipa(BigInteger howMany) {
		return lipari(howMany);
	}

	/**
	 * Yields the given amount of liparis, as a {@link java.math.BigInteger}.
	 * Liparis are level 7 coins.
	 * 
	 * @param howMany the amount
	 * @return the amount, as a {@link java.math.BigInteger}
	 */
	public static BigInteger lipari(long howMany) {
		return valueOf(howMany).multiply(valueOf(1_000_000_000_000_000_000L));
	}

	/**
	 * Yields the given amount of liparis, as a {@link java.math.BigInteger}.
	 * Liparis are level 7 coins.
	 * 
	 * @param howMany the amount
	 * @return the amount, as a {@link java.math.BigInteger}
	 */
	public static BigInteger level7(long howMany) {
		return valueOf(howMany).multiply(valueOf(1_000_000_000_000_000_000L));
	}

	/**
	 * Yields the given amount of liparis, as a {@link java.math.BigInteger}.
	 * Liparis are level 7 coins.
	 * 
	 * @param howMany the amount
	 * @return the amount, as a {@link java.math.BigInteger}
	 */
	public static BigInteger lipa(long howMany) {
		return lipari(howMany);
	}

	/**
	 * Yields the given amount of mokas, as a {@link java.math.BigInteger}.
	 * Mokas are level 8 coins.
	 * 
	 * @param howMany the amount
	 * @return the amount, as a {@link java.math.BigInteger}
	 */
	public static BigInteger moka(BigInteger howMany) {
		return howMany.multiply(valueOf(1_000_000_000_000_000_000L).multiply(valueOf(1_000L)));
	}

	/**
	 * Yields the given amount of mokas, as a {@link java.math.BigInteger}.
	 * Mokas are level 8 coins.
	 * 
	 * @param howMany the amount
	 * @return the amount, as a {@link java.math.BigInteger}
	 */
	public static BigInteger level8(BigInteger howMany) {
		return moka(howMany);
	}

	/**
	 * Yields the given amount of mokas, as a {@link java.math.BigInteger}.
	 * Mokas are level 8 coins.
	 * 
	 * @param howMany the amount
	 * @return the amount, as a {@link java.math.BigInteger}
	 */
	public static BigInteger moka(long howMany) {
		return valueOf(howMany).multiply(valueOf(1_000_000_000_000_000_000L).multiply(valueOf(1_000L)));
	}

	/**
	 * Yields the given amount of mokas, as a {@link java.math.BigInteger}.
	 * Mokas are level 8 coins.
	 * 
	 * @param howMany the amount
	 * @return the amount, as a {@link java.math.BigInteger}
	 */
	public static BigInteger level8(long howMany) {
		return valueOf(howMany).multiply(valueOf(1_000_000_000_000_000_000L).multiply(valueOf(1_000L)));
	}
}