var assert = require('assert');

describe('Main', function() {

    it('should have access to foo.bar', function () {

        var main = new bundle.Main();
        assert.equal('bar', main.foo.bar.name);
    });
});