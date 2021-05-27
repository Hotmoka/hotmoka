export class ClassType {
    public static readonly BIG_INTEGER = new ClassType("java.math.BigInteger")

    public readonly name: string

    private constructor(name: string) {
       this.name = name
    }
}