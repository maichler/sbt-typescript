///<reference path="common/foo.ts"/>

module bundle {
    import Foo = bundle.common.Foo;
    export class Main {
        foo:Foo;
        constructor() {
            this.foo = new Foo();
        }
    }
}

declare var exports:any;
exports.bundle = bundle;

