package io.takamaka.code.lang;

import static java.math.BigInteger.valueOf;
import java.math.BigInteger;

/**
 * The coins of the Takamaka language. They are, in increasing order of magnitude:
 * 
 * panarea aka pana (level 1)
 * alicudi aka ali (=1000 panareas) (level 2)
 * filicudi aka fili (=1000 alicudi) (level 3)
 * stromboli aka strom (=1000 filicudis) (level 4)
 * vulcano aka vul (=1000 strombolis) (level 5)
 * salina aka sali (=1000 vulcanos) (level 6)
 * lipari aka lipa (=1000 salinas) (level 7)
 * takamaka aka taka (=1000 liparis) (level 8)
 */
public class Coin {

	/**
	 * Yields the given amount of panareas, as a {@linkplain #java.math.BigInteger}.
	 * Panareas are level 1 coins.
	 * 
	 * @param howMany the amount
	 * @return the amount, as a {@linkplain #java.math.BigInteger}
	 */
	public static BigInteger panarea(BigInteger howMany) {
		return howMany;
	}

	/**
	 * Yields the given amount of panareas, as a {@linkplain #java.math.BigInteger}.
	 * Panareas are level 1 coins.
	 * 
	 * @param howMany the amount
	 * @return the amount, as a {@linkplain #java.math.BigInteger}
	 */
	public static BigInteger level1(BigInteger howMany) {
		return howMany;
	}

	/**
	 * Yields the given amount of panareas, as a {@linkplain #java.math.BigInteger}.
	 * Panareas are level 1 coins.
	 *
	 * @param howMany the amount
	 * @return the amount, as a {@linkplain #java.math.BigInteger}
	 */
	public static BigInteger pana(BigInteger howMany) {
		return panarea(howMany);
	}

	/**
	 * Yields the given amount of panareas, as a {@linkplain #java.math.BigInteger}.
	 * Panareas are level 1 coins.
	 * 
	 * @param howMany the amount
	 * @return the amount, as a {@linkplain #java.math.BigInteger}
	 */
	public static BigInteger panarea(long howMany) {
		return valueOf(howMany);
	}

	/**
	 * Yields the given amount of panareas, as a {@linkplain #java.math.BigInteger}.
	 * Panareas are level 1 coins.
	 * 
	 * @param howMany the amount
	 * @return the amount, as a {@linkplain #java.math.BigInteger}
	 */
	public static BigInteger level1(long howMany) {
		return valueOf(howMany);
	}

	/**
	 * Yields the given amount of panareas, as a {@linkplain #java.math.BigInteger}.
	 * Panareas are level 1 coins.
	 * 
	 * @param howMany the amount
	 * @return the amount, as a {@linkplain #java.math.BigInteger}
	 */
	public static BigInteger pana(long howMany) {
		return panarea(howMany);
	}

	/**
	 * Yields the given amount of alicudis, as a {@linkplain #java.math.BigInteger}.
	 * Alicudis are level 2 coins.
	 * 
	 * @param howMany the amount
	 * @return the amount, as a {@linkplain #java.math.BigInteger}
	 */
	public static BigInteger alicudi(BigInteger howMany) {
		return howMany.multiply(valueOf(1_000L));
	}

	/**
	 * Yields the given amount of alicudis, as a {@linkplain #java.math.BigInteger}.
	 * Alicudis are level 2 coins.
	 * 
	 * @param howMany the amount
	 * @return the amount, as a {@linkplain #java.math.BigInteger}
	 */
	public static BigInteger level2(BigInteger howMany) {
		return howMany.multiply(valueOf(1_000L));
	}

	/**
	 * Yields the given amount of alicudis, as a {@linkplain #java.math.BigInteger}.
	 * Alicudis are level 2 coins.
	 * 
	 * @param howMany the amount
	 * @return the amount, as a {@linkplain #java.math.BigInteger}
	 */
	public static BigInteger ali(BigInteger howMany) {
		return alicudi(howMany);
	}

	/**
	 * Yields the given amount of alicudis, as a {@linkplain #java.math.BigInteger}.
	 * Alicudis are level 2 coins.
	 * 
	 * @param howMany the amount
	 * @return the amount, as a {@linkplain #java.math.BigInteger}
	 */
	public static BigInteger alicudi(long howMany) {
		return valueOf(howMany).multiply(valueOf(1_000L));
	}

	/**
	 * Yields the given amount of alicudis, as a {@linkplain #java.math.BigInteger}.
	 * Alicudis are level 2 coins.
	 * 
	 * @param howMany the amount
	 * @return the amount, as a {@linkplain #java.math.BigInteger}
	 */
	public static BigInteger level2(long howMany) {
		return valueOf(howMany).multiply(valueOf(1_000L));
	}

	/**
	 * Yields the given amount of alicudis, as a {@linkplain #java.math.BigInteger}.
	 * Alicudis are level 2 coins.
	 * 
	 * @param howMany the amount
	 * @return the amount, as a {@linkplain #java.math.BigInteger}
	 */
	public static BigInteger ali(long howMany) {
		return alicudi(howMany);
	}

	/**
	 * Yields the given amount of filicudis, as a {@linkplain #java.math.BigInteger}.
	 * Filicudis are level 3 coins.
	 * 
	 * @param howMany the amount
	 * @return the amount, as a {@linkplain #java.math.BigInteger}
	 */
	public static BigInteger filicudi(BigInteger howMany) {
		return howMany.multiply(valueOf(1_000_000L));
	}

	/**
	 * Yields the given amount of filicudis, as a {@linkplain #java.math.BigInteger}.
	 * Filicudis are level 3 coins.
	 * 
	 * @param howMany the amount
	 * @return the amount, as a {@linkplain #java.math.BigInteger}
	 */
	public static BigInteger level3(BigInteger howMany) {
		return howMany.multiply(valueOf(1_000_000L));
	}

	/**
	 * Yields the given amount of filicudis, as a {@linkplain #java.math.BigInteger}.
	 * Filicudis are level 3 coins.
	 * 
	 * @param howMany the amount
	 * @return the amount, as a {@linkplain #java.math.BigInteger}
	 */
	public static BigInteger fili(BigInteger howMany) {
		return filicudi(howMany);
	}

	/**
	 * Yields the given amount of filicudis, as a {@linkplain #java.math.BigInteger}.
	 * Filicudis are level 3 coins.
	 * 
	 * @param howMany the amount
	 * @return the amount, as a {@linkplain #java.math.BigInteger}
	 */
	public static BigInteger filicudi(long howMany) {
		return valueOf(howMany).multiply(valueOf(1_000_000L));
	}

	/**
	 * Yields the given amount of filicudis, as a {@linkplain #java.math.BigInteger}.
	 * Filicudis are level 3 coins.
	 * 
	 * @param howMany the amount
	 * @return the amount, as a {@linkplain #java.math.BigInteger}
	 */
	public static BigInteger level3(long howMany) {
		return valueOf(howMany).multiply(valueOf(1_000_000L));
	}

	/**
	 * Yields the given amount of filicudis, as a {@linkplain #java.math.BigInteger}.
	 * Filicudis are level 3 coins.
	 * 
	 * @param howMany the amount
	 * @return the amount, as a {@linkplain #java.math.BigInteger}
	 */
	public static BigInteger fili(long howMany) {
		return filicudi(howMany);
	}

	/**
	 * Yields the given amount of strombolis, as a {@linkplain #java.math.BigInteger}.
	 * Strombolis are level 4 coins.
	 * 
	 * @param howMany the amount
	 * @return the amount, as a {@linkplain #java.math.BigInteger}
	 */
	public static BigInteger stromboli(BigInteger howMany) {
		return howMany.multiply(valueOf(1_000_000_000L));
	}

	/**
	 * Yields the given amount of strombolis, as a {@linkplain #java.math.BigInteger}.
	 * Strombolis are level 4 coins.
	 * 
	 * @param howMany the amount
	 * @return the amount, as a {@linkplain #java.math.BigInteger}
	 */
	public static BigInteger level4(BigInteger howMany) {
		return howMany.multiply(valueOf(1_000_000_000L));
	}

	/**
	 * Yields the given amount of strombolis, as a {@linkplain #java.math.BigInteger}.
	 * Strombolis are level 4 coins.
	 * 
	 * @param howMany the amount
	 * @return the amount, as a {@linkplain #java.math.BigInteger}
	 */
	public static BigInteger strom(BigInteger howMany) {
		return stromboli(howMany);
	}

	/**
	 * Yields the given amount of strombolis, as a {@linkplain #java.math.BigInteger}.
	 * Strombolis are level 4 coins.
	 * 
	 * @param howMany the amount
	 * @return the amount, as a {@linkplain #java.math.BigInteger}
	 */
	public static BigInteger stromboli(long howMany) {
		return valueOf(howMany).multiply(valueOf(1_000_000_000L));
	}

	/**
	 * Yields the given amount of strombolis, as a {@linkplain #java.math.BigInteger}.
	 * Strombolis are level 4 coins.
	 * 
	 * @param howMany the amount
	 * @return the amount, as a {@linkplain #java.math.BigInteger}
	 */
	public static BigInteger level4(long howMany) {
		return valueOf(howMany).multiply(valueOf(1_000_000_000L));
	}

	/**
	 * Yields the given amount of strombolis, as a {@linkplain #java.math.BigInteger}.
	 * Strombolis are level 4 coins.
	 * 
	 * @param howMany the amount
	 * @return the amount, as a {@linkplain #java.math.BigInteger}
	 */
	public static BigInteger strom(long howMany) {
		return stromboli(howMany);
	}

	/**
	 * Yields the given amount of vulcanos, as a {@linkplain #java.math.BigInteger}.
	 * Vulcanos are level 5 coins.
	 * 
	 * @param howMany the amount
	 * @return the amount, as a {@linkplain #java.math.BigInteger}
	 */
	public static BigInteger vulcano(BigInteger howMany) {
		return howMany.multiply(valueOf(1_000_000_000_000L));
	}

	/**
	 * Yields the given amount of vulcanos, as a {@linkplain #java.math.BigInteger}.
	 * Vulcanos are level 5 coins.
	 * 
	 * @param howMany the amount
	 * @return the amount, as a {@linkplain #java.math.BigInteger}
	 */
	public static BigInteger level5(BigInteger howMany) {
		return howMany.multiply(valueOf(1_000_000_000_000L));
	}

	/**
	 * Yields the given amount of vulcanos, as a {@linkplain #java.math.BigInteger}.
	 * Vulcanos are level 5 coins.
	 * 
	 * @param howMany the amount
	 * @return the amount, as a {@linkplain #java.math.BigInteger}
	 */
	public static BigInteger vul(BigInteger howMany) {
		return vulcano(howMany);
	}

	/**
	 * Yields the given amount of vulcanos, as a {@linkplain #java.math.BigInteger}.
	 * Vulcanos are level 5 coins.
	 * 
	 * @param howMany the amount
	 * @return the amount, as a {@linkplain #java.math.BigInteger}
	 */
	public static BigInteger vulcano(long howMany) {
		return valueOf(howMany).multiply(valueOf(1_000_000_000_000L));
	}
	
	/**
	 * Yields the given amount of vulcanos, as a {@linkplain #java.math.BigInteger}.
	 * Vulcanos are level 5 coins.
	 * 
	 * @param howMany the amount
	 * @return the amount, as a {@linkplain #java.math.BigInteger}
	 */
	public static BigInteger level5(long howMany) {
		return valueOf(howMany).multiply(valueOf(1_000_000_000_000L));
	}

	/**
	 * Yields the given amount of vulcanos, as a {@linkplain #java.math.BigInteger}.
	 * Vulcanos are level 5 coins.
	 * 
	 * @param howMany the amount
	 * @return the amount, as a {@linkplain #java.math.BigInteger}
	 */
	public static BigInteger vul(long howMany) {
		return vulcano(howMany);
	}

	/**
	 * Yields the given amount of salinas, as a {@linkplain #java.math.BigInteger}.
	 * Salinas are level 6 coins.
	 * 
	 * @param howMany the amount
	 * @return the amount, as a {@linkplain #java.math.BigInteger}
	 */
	public static BigInteger salina(BigInteger howMany) {
		return howMany.multiply(valueOf(1_000_000_000_000_000L));
	}

	/**
	 * Yields the given amount of salinas, as a {@linkplain #java.math.BigInteger}.
	 * Salinas are level 6 coins.
	 * 
	 * @param howMany the amount
	 * @return the amount, as a {@linkplain #java.math.BigInteger}
	 */
	public static BigInteger level6(BigInteger howMany) {
		return howMany.multiply(valueOf(1_000_000_000_000_000L));
	}

	/**
	 * Yields the given amount of salinas, as a {@linkplain #java.math.BigInteger}.
	 * Salinas are level 6 coins.
	 * 
	 * @param howMany the amount
	 * @return the amount, as a {@linkplain #java.math.BigInteger}
	 */
	public static BigInteger sali(BigInteger howMany) {
		return salina(howMany);
	}

	/**
	 * Yields the given amount of salinas, as a {@linkplain #java.math.BigInteger}.
	 * Salinas are level 6 coins.
	 * 
	 * @param howMany the amount
	 * @return the amount, as a {@linkplain #java.math.BigInteger}
	 */
	public static BigInteger salina(long howMany) {
		return valueOf(howMany).multiply(valueOf(1_000_000_000_000_000L));
	}

	/**
	 * Yields the given amount of salinas, as a {@linkplain #java.math.BigInteger}.
	 * Salinas are level 6 coins.
	 * 
	 * @param howMany the amount
	 * @return the amount, as a {@linkplain #java.math.BigInteger}
	 */
	public static BigInteger level6(long howMany) {
		return valueOf(howMany).multiply(valueOf(1_000_000_000_000_000L));
	}

	/**
	 * Yields the given amount of salinas, as a {@linkplain #java.math.BigInteger}.
	 * Salinas are level 6 coins.
	 * 
	 * @param howMany the amount
	 * @return the amount, as a {@linkplain #java.math.BigInteger}
	 */
	public static BigInteger sali(long howMany) {
		return salina(howMany);
	}

	/**
	 * Yields the given amount of liparis, as a {@linkplain #java.math.BigInteger}.
	 * Liparis are level 7 coins.
	 * 
	 * @param howMany the amount
	 * @return the amount, as a {@linkplain #java.math.BigInteger}
	 */
	public static BigInteger lipari(BigInteger howMany) {
		return howMany.multiply(valueOf(1_000_000_000_000_000_000L));
	}

	/**
	 * Yields the given amount of liparis, as a {@linkplain #java.math.BigInteger}.
	 * Liparis are level 7 coins.
	 * 
	 * @param howMany the amount
	 * @return the amount, as a {@linkplain #java.math.BigInteger}
	 */
	public static BigInteger level7(BigInteger howMany) {
		return howMany.multiply(valueOf(1_000_000_000_000_000_000L));
	}

	/**
	 * Yields the given amount of liparis, as a {@linkplain #java.math.BigInteger}.
	 * Liparis are level 7 coins.
	 * 
	 * @param howMany the amount
	 * @return the amount, as a {@linkplain #java.math.BigInteger}
	 */
	public static BigInteger lipa(BigInteger howMany) {
		return lipari(howMany);
	}

	/**
	 * Yields the given amount of liparis, as a {@linkplain #java.math.BigInteger}.
	 * Liparis are level 7 coins.
	 * 
	 * @param howMany the amount
	 * @return the amount, as a {@linkplain #java.math.BigInteger}
	 */
	public static BigInteger lipari(long howMany) {
		return valueOf(howMany).multiply(valueOf(1_000_000_000_000_000_000L));
	}

	/**
	 * Yields the given amount of liparis, as a {@linkplain #java.math.BigInteger}.
	 * Liparis are level 7 coins.
	 * 
	 * @param howMany the amount
	 * @return the amount, as a {@linkplain #java.math.BigInteger}
	 */
	public static BigInteger level7(long howMany) {
		return valueOf(howMany).multiply(valueOf(1_000_000_000_000_000_000L));
	}

	/**
	 * Yields the given amount of liparis, as a {@linkplain #java.math.BigInteger}.
	 * Liparis are level 7 coins.
	 * 
	 * @param howMany the amount
	 * @return the amount, as a {@linkplain #java.math.BigInteger}
	 */
	public static BigInteger lipa(long howMany) {
		return lipari(howMany);
	}

	/**
	 * Yields the given amount of takamakas, as a {@linkplain #java.math.BigInteger}.
	 * Takamakas are level 8 coins.
	 * 
	 * @param howMany the amount
	 * @return the amount, as a {@linkplain #java.math.BigInteger}
	 */
	public static BigInteger takamaka(BigInteger howMany) {
		return howMany.multiply(valueOf(1_000_000_000_000_000_000L).multiply(valueOf(1_000L)));
	}

	/**
	 * Yields the given amount of takamakas, as a {@linkplain #java.math.BigInteger}.
	 * Takamakas are level 8 coins.
	 * 
	 * @param howMany the amount
	 * @return the amount, as a {@linkplain #java.math.BigInteger}
	 */
	public static BigInteger taka(BigInteger howMany) {
		return takamaka(howMany);
	}

	/**
	 * Yields the given amount of takamakas, as a {@linkplain #java.math.BigInteger}.
	 * Takamakas are level 8 coins.
	 * 
	 * @param howMany the amount
	 * @return the amount, as a {@linkplain #java.math.BigInteger}
	 */
	public static BigInteger level8(BigInteger howMany) {
		return takamaka(howMany);
	}

	/**
	 * Yields the given amount of takamakas, as a {@linkplain #java.math.BigInteger}.
	 * Takamakas are level 8 coins.
	 * 
	 * @param howMany the amount
	 * @return the amount, as a {@linkplain #java.math.BigInteger}
	 */
	public static BigInteger takamaka(long howMany) {
		return valueOf(howMany).multiply(valueOf(1_000_000_000_000_000_000L).multiply(valueOf(1_000L)));
	}

	/**
	 * Yields the given amount of takamakas, as a {@linkplain #java.math.BigInteger}.
	 * Takamakas are level 8 coins.
	 * 
	 * @param howMany the amount
	 * @return the amount, as a {@linkplain #java.math.BigInteger}
	 */
	public static BigInteger level8(long howMany) {
		return valueOf(howMany).multiply(valueOf(1_000_000_000_000_000_000L).multiply(valueOf(1_000L)));
	}

	/**
	 * Yields the given amount of takamakas, as a {@linkplain #java.math.BigInteger}.
	 * Takamakas are level 8 coins.
	 * 
	 * @param howMany the amount
	 * @return the amount, as a {@linkplain #java.math.BigInteger}
	 */
	public static BigInteger taka(long howMany) {
		return takamaka(howMany);
	}
}