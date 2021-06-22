export class HotmokaError {
    message: string
    exceptionClassName: string

    constructor(message: string, exceptionClassName: string) {
        this.message = message
        this.exceptionClassName = exceptionClassName
    }

}
