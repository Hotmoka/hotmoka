export class Utils {


    /**
     * Checks if two string arrays are equal and in the same order.
     * @param arr1 the first array
     * @param arr2 the second array
     * @return true if the are equal, false otherwise
     */
    public static arrayEquals(arr1: Array<string>, arr2: Array<string>): boolean {
        if (arr1.length !== arr2.length) {
            return false
        }
        for (let i = 0; i < arr1.length; i++) {
            if (arr1[i] !== arr2[i]) {
                return false
            }
        }
        return true
    }
}