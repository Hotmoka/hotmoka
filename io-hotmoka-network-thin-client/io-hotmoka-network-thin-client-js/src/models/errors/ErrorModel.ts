export class ErrorModel {
    message: string
    exceptionClassName: string

    constructor(message: string, exceptionClassName: string) {
        this.message = message
        this.exceptionClassName = exceptionClassName
    }

}
