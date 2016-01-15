///<reference path="bar.ts"/>

module bundle.common {

    export class Foo {
        bar:Bar;
        constructor() {
            this.bar = new Bar();
        }
    }
}