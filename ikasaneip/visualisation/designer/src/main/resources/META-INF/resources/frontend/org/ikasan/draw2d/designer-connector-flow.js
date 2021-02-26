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

        let canvasRightClickX=0;
        let canvasRightClickY=0;
        let rightClickX=0;
        let rightClickY=0;

        $(document).ready(function () {
            $("#canvas-wrapper").mouseover(function (e) {
                if(e.offsetX > 100) {
                    x = e.offsetX;
                }
                if(e.offsetY > 100) {
                    y=e.offsetY;
                }
            });
            $("#canvas-wrapper").on("contextmenu", function(e){
                canvasRightClickX=e.offsetX;
                canvasRightClickY=e.offsetY;
                rightClickX=e.pageX;
                rightClickY=e.pageY;
                return false;
            });
        });


        // $(document).on("contextmenu", function(e){
        //     rightClickX=e.pageX;
        //     rightClickY=e.pageY
        //     return false;
        // });

        class FigureLite {
            constructor(name, x, y, width, height, type) {
                this.identifier = name;
                this.x = x;
                this.y = y;
                this.width = width;
                this.height= height;
                this.type = type;
            }

        }

        class Container {
            constructor(figures, x, y, windowx, windowy) {
                this.figures = figures;
                this.x = x;
                this.y = y;
                this.windowx = windowx;
                this.windowy = windowy;
            }

        }

        designer.$connector.getSelected = function () {
            console.log(_this.getFigures());
            console.log(_this.getLines());
            let figures = new Array();

            // _this.getFigures().each((i, figure)=>{
            //     figures.push(new FigureLite(figure.getId(), figure.x, figure.y, figure.getWidth(), figure.getHeight(), JSON.stringify(figure.getPersistentAttributes())));
            // });
            //
            // _this.getLines().each((i, figure)=>{
            //     figures.push(new FigureLite(figure.getId(), figure.x, figure.y, figure.getWidth(), figure.getHeight(), JSON.stringify(figure.getPersistentAttributes())));
            // });

            _this.getSelection().each((i, figure)=>{
                figures.push(new FigureLite(figure.getId(), figure.x, figure.y, figure.getWidth(), figure.getHeight(), figure.NAME));
            });

            let container = new Container(figures, canvasRightClickX, canvasRightClickY, rightClickX, rightClickY);

            console.log(JSON.stringify(container));
            return JSON.stringify(container);
        }

        designer.$connector.addIcon = function (identifier, image, h, w) {
            debugger;
            let messageChannel = new draw2d.shape.basic.Image({id: identifier, path: image, width:w, height:h, x:x, y:y, keepAspectRatio: true});
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

        designer.$connector.rotate = function (degrees) {
            _this.getFigures().each((i, figure)=>{
                if(figure.isSelected()) {
                    // _this.getCommandStack().execute(new draw2d.command.CommandRotate(_this.getSelection(), degrees % 360));
                    // figure.setRotationAngle(degrees % 360);
                    // figure.rotationAngle = (figure.getRotationAngle() + 90) % 360;
                    // figure.repaint();

                    // let command = figure.createCommand(new draw2d.command.CommandType(draw2d.command.CommandType.ROTATE));
                    console.log('figure current rotation angle ' + figure.getRotationAngle());
                    console.log('degrees ' + degrees);
                    console.log(figure);
                    figure.setRotationAngle((figure.getRotationAngle() + degrees) % 360);
                    figure.repaint();
                    // let command = new draw2d.command.CommandRotate(figure, (figure.getRotationAngle() + degrees) % 360);
                    // if (command !== null) {
                    //     _this.getCommandStack().execute(command);
                    //     figure.repaint();
                    // }
                }
            });
        }

        designer.$connector.addBoundary = function (h, w) {
            let boundary =  new draw2d.shape.basic.Rectangle({
                x: x,
                y: y,
                bgColor: "#ffffff",
                alpha  : 0.7,
                width: w,
                height: h,
                radius: 10,
                dash: "--",
                rotationAngle: 15,
            });

            boundary.uninstallEditPolicy(new draw2d.policy.figure.RectangleSelectionFeedbackPolicy());
            boundary.installEditPolicy(new RotateRectangleSelectionFeedbackPolicy());
            designer.$connector.designer.add(boundary);
        }

        designer.$connector.designer.on("dblclick", function(emitter, event){
            let figure = event.figure;
            let figureLite = new FigureLite(figure.getId(), figure.x, figure.y, figure.getWidth(), figure.getHeight(), figure.NAME);
            let element = document.getElementById("canvas-wrapper");
            element.$server.doubleClickEvent(JSON.stringify(figureLite));
        });


        designer.$connector.addTriangle = function (h, w) {
            let triangle = new TriangleFigure({x: x, y:y, width:w, height:h, bgColor:"rgba(255,255,255,0)"});

            designer.$connector.designer.add(triangle);
        }


        designer.$connector.addOval = function (h, w) {
            let oval =  new draw2d.shape.basic.Oval({width:w,height:h, x:x, y:y, bgColor:"rgba(255,255,255,0)"});

            designer.$connector.designer.add(oval);
        }

        designer.$connector.addCircle = function () {
            let circle =new draw2d.shape.basic.Circle({diameter:80, x:x, y:y, bgColor:"rgba(255,255,255,0)"});

            designer.$connector.designer.add(circle);
        }

        designer.$connector.addLabel = function (labelString, x, y) {
            let label = new draw2d.shape.basic.Label({
                text: labelString,
                color:"rgba(255,255,255,0)",
                fontColor:"#0d0d0d",
                bgColor:"rgba(255,255,255,0)",
                outlineColor:"rgba(255,255,255,0)",
                fontFamily: "Trebuchet MS",
                fontSize: "12pt",
                x:x, y:y
            });


            label.installEditor(new draw2d.ui.LabelInplaceEditor());

            designer.$connector.designer.add(label);
        }

        designer.$connector.addLabelToFigure = function (figureIdentifier, labelString) {
            let _figure = null;
            _this.getFigures().each((i, figure)=>{
                if(figure.id === figureIdentifier) {
                    _figure = figure;
                }
            });

            if(_figure != null) {

                let label = new draw2d.shape.basic.Label({
                    text: labelString,
                    color: "rgba(255,255,255,0)",
                    fontColor: "#0d0d0d",
                    bgColor: "rgba(255,255,255,0)",
                    outlineColor: "rgba(255,255,255,0)",
                    fontFamily: "Trebuchet MS",
                    fontSize: "12pt",
                    x: _figure.x - (_figure.width / 2), y: _figure.y + _figure.getHeight()
                });

                designer.$connector.designer.add(label);

                label.setX(_figure.x - (label.getWidth() / 2) + (_figure.getWidth() / 2));

                let figuresToGroup = new draw2d.util.ArrayList();
                figuresToGroup.add(label);
                figuresToGroup.add(_figure);

                _this.getCommandStack().execute(new draw2d.command.CommandGroup(_this, figuresToGroup))
            }
        }


        designer.$connector.setBackgroundColor = function (color) {
            debugger;
            _this.getSelection().each((i, figure)=>{
                debugger;
                if(figure.NAME === 'draw2d.Connection') {
                    figure.setColor(color);

                    let targetDecorator = figure.getTargetDecorator();
                    if(targetDecorator != null) {
                        targetDecorator.setBackgroundColor(color);
                        targetDecorator.setColor(color);
                    }

                    let sourceDecorator = figure.getSourceDecorator();
                    if(sourceDecorator != null) {
                        sourceDecorator.setBackgroundColor(color);
                        sourceDecorator.setColor(color);
                    }
                }
                else {
                    figure.setBackgroundColor(color);
                }
            });
        }

        designer.$connector.setLineType = function (pattern) {
            debugger;
            _this.getFigures().each((i, figure)=>{
                debugger;
                if(figure.isSelected()) {
                    debugger;
                    figure.setDashArray(pattern);
                }
            });
        }

        designer.$connector.setRadius = function (radius) {
            debugger;
            _this.getFigures().each((i, figure)=>{
                debugger;
                if(figure.isSelected()) {
                    debugger;
                    figure.setRadius(radius);
                }
            });
        }

        designer.$connector.setStroke = function (width) {
            debugger;
            _this.getFigures().each((i, figure)=>{
                debugger;
                if(figure.isSelected()) {
                    debugger;
                    figure.setStroke(width);
                }
            });
        }

        designer.$connector.exportJson = function () {
            debugger;
            let writer = new draw2d.io.json.Writer();
            let result = null;
            writer.marshal(designer.$connector.designer, function(json){
                result = JSON.stringify(json,null,2);
            });

            return result;
        }

        designer.$connector.importJson = function (jsonDocument) {
            debugger
            let reader = new draw2d.io.json.Reader();
            reader.unmarshal(designer.$connector.designer, jsonDocument);
        }




        let pngResult = null;

        designer.$connector.exportPng = function () {
            debugger;
            let xCoords = [];
            let yCoords = [];
            designer.$connector.designer.getFigures().each(function(i,f){
                let b = f.getBoundingBox();
                xCoords.push(b.x, b.x+b.w);
                yCoords.push(b.y, b.y+b.h);
            });
            let minX   = Math.min.apply(Math, xCoords);
            let minY   = Math.min.apply(Math, yCoords);
            let width  = Math.max.apply(Math, xCoords)-minX;
            let height = Math.max.apply(Math, yCoords)-minY;

            let writer = new draw2d.io.png.Writer();
            writer.marshal(designer.$connector.designer,function(png){
                $("#preview").attr("src",png);
            }, new draw2d.geo.Rectangle(minX - 15,minY - 15,width + 30,height + 30));

            // let croppedCanvas = document.createElement('canvas');
            //
            // let scaleFactor = 5;
            //
            // croppedCanvas.width = width * scaleFactor;
            // croppedCanvas.height = height * scaleFactor;
            // croppedCanvas.getContext("2d").scale(scaleFactor, scaleFactor);
            //
            // const canvas = document.getElementById('canvas-wrapper');
            // croppedCanvas.getContext("2d").drawImage(canvas.getContext("2d"), minX, minY, width, height, 0, 0, width, height);
            //
            // // let dataUrl = croppedCanvas.toDataURL("image/png", 1);
            // // var base64Image = dataUrl.replace("data:image/png;base64,", "");
            //
            // if(window.navigator.msSaveBlob) {
            //     window.navigator.msSaveBlob(croppedCanvas.msToBlob(), "canvas.png")
            // }
            // else {
            //     const a = document.createElement("a");
            //     document.body.appendChild(a);
            //     a.href = croppedCanvas.toDataURL("image/png", 1);
            //     a.download = 'canvas.jpg';
            //     a.click();
            //     document.body.removeChild(a);
            // }
        }

        designer.$connector.getPng = function () {
            return pngResult;
        }

        $(document).addEventListener("DOMContentLoaded",function () {


            setTimeout(function() {
                _this.exportPng();
            },1);

            // add an event listener to the Canvas for change notifications.
            // We just dump the current canvas document into the IMG
            //
            designer.$connector.designer.getCommandStack().addEventListener(function(e){
                if(e.isPostChangeEvent()){
                    _this.exportPng();
                }
            });
        });



        designer.$connector.setReadOnly = function (readonly) {
            if(readonly === true){
                designer.$connector.designer.uninstallEditPolicy( new draw2d.policy.canvas.FadeoutDecorationPolicy());
                designer.$connector.designer.uninstallEditPolicy( new draw2d.policy.canvas.SnapToGeometryEditPolicy());
                designer.$connector.designer.uninstallEditPolicy( new draw2d.policy.canvas.SnapToCenterEditPolicy());
                designer.$connector.designer.uninstallEditPolicy( new draw2d.policy.canvas.SnapToInBetweenEditPolicy());
                designer.$connector.designer.installEditPolicy( new draw2d.policy.canvas.ReadOnlySelectionPolicy());
            }
            else {
                designer.$connector.designer.installEditPolicy( new draw2d.policy.canvas.FadeoutDecorationPolicy());
                designer.$connector.designer.installEditPolicy( new draw2d.policy.canvas.SnapToGeometryEditPolicy());
                designer.$connector.designer.installEditPolicy( new draw2d.policy.canvas.SnapToCenterEditPolicy());
                designer.$connector.designer.installEditPolicy( new draw2d.policy.canvas.SnapToInBetweenEditPolicy());
                designer.$connector.designer.uninstallEditPolicy( new draw2d.policy.canvas.ReadOnlySelectionPolicy());
            }
        }



    }
}
