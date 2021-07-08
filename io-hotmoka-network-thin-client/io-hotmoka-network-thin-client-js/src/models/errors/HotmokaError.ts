export class HotmokaError extends Error {
    message: string
    exceptionClassName: string

    constructor(message: string, exceptionClassName: string) {
        super(message)
        this.message = message
        this.exceptionClassName = exceptionClassName
    }

}
