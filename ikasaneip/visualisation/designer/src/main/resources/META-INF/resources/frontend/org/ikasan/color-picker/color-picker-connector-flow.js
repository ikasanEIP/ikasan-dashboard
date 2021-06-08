window.Vaadin.Flow.designerConnector = {
    initLazy : function(designer) {

        // Check whether the connector was already initialized for the Iron list
        if (designer.$connector) {
            return;
        }
        console.log('init designer');

        designer.$connector = {};
        this.clippboardFigure=null;

        designer.$connector.designer = new View(this, "canvas-wrapper");

        let _this = designer.$connector.designer;

        Mousetrap.bind(['ctrl+a', 'command+a'], $.proxy(function (event) {
            debugger;
            _this.getFigures().each((i, figure)=>{
                figure.select(false);
            });

            return false;
        },this));

        let x=100;
        let y=100;


        $(document).ready(function () {
            $("#canvas-wrapper").mouseover(function (e) {
                x=e.offsetX;
                y=e.offsetY;
            });
        });

        let rightClickX=0;
        let rightClickY=0;
        $(document).on("contextmenu", function(e){
            rightClickX=e.pageX;
            rightClickY=e.pageY
            return false;
        });

        class FigureLite {
            constructor(name, x, y) {
                this.image = name;
                this.x = x;
                this.y = y;
            }

        }

        class Container {
            constructor(figures, x, y) {
                this.figures = figures;
                this.x = x;
                this.y = y;
            }

        }

        designer.$connector.getSelected = function () {
            console.log(_this.getFigures());
            let figures = new Array();

            _this.getFigures().each((i, figure)=>{
                figures.push(new FigureLite(figure.getId(), figure.x, figure.y));
            });

            let container = new Container(figures, rightClickX, rightClickY);

            console.log(JSON.stringify(container));
            return JSON.stringify(container);
        }

        designer.$connector.addIcon = function (image, h, w) {
            debugger;
            let messageChannel = new draw2d.shape.basic.Image({id: "test", path: image, width:w, height:h, x:x, y:y, keepAspectRatio: true});
            messageChannel.createPort("input");
            messageChannel.createPort("output");

            designer.$connector.designer.add(messageChannel);
        }

        designer.$connector.bringToFront = function () {
            _this.getFigures().each((i, figure)=>{
                if(figure.isSelected()) {
                    figure.toFront();
                }
            });
        }

        designer.$connector.sendToBack = function () {
            _this.getFigures().each((i, figure)=>{
                if(figure.isSelected()) {
                    figure.toBack();
                }
            });
        }


        designer.$connector.group = function () {
            _this.getCommandStack().execute(new draw2d.command.CommandGroup(_this, _this.getSelection()))
        }

        designer.$connector.ungroup = function () {
            debugger;
            _this.getCommandStack().execute(new draw2d.command.CommandUngroup(_this, _this.getSelection()))
        }

        designer.$connector.addBoundary = function (h, w) {
            debugger;
            let boundary =  new draw2d.shape.basic.Rectangle({
                x: x,
                y: y,
                bgColor: "#ffffff",
                alpha  : 0.7,
                width: w,
                height: h,
                radius: 10,
                dash: "--"
            });

            designer.$connector.designer.add(boundary);
        }

    }
}
