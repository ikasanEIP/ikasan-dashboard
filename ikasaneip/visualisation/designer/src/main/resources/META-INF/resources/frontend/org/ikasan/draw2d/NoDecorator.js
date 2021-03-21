NoDecorator = draw2d.decoration.connection.Decorator.extend({

    NAME : "NoDecorator",

    /**
     * @constructor
     *
     * @param {Number} [width] the width of the bar
     * @param {Number} [height] the height of the bar
     */
    init: function(width, height)
    {
        this._super( width, height);
    },

    /**
     * @method
     * Draw a bar decoration.
     *
     *
     * @param {Raphael} paper the raphael paper object for the paint operation
     **/
    paint: function(paper)
    {
        let st = paper.set();

        return st;
    }

});