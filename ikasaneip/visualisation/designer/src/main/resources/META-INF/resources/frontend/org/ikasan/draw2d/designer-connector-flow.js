    window.Vaadin.Flow.designerConnector = {
    initLazy : function(designer, name, readonly) {

        // Check whether the connector was already initialized for the Iron list
        if (designer.$connector) {
            return;
        }
        console.log('init designer');

        designer.$connector = {};
        this.clippboardFigure=null;

        let canvasName = name;
        designer.$connector.designer = new View(this, name, readonly);

        let _this = designer.$connector.designer;

        let x=100;
        let y=100;

        let canvasRightClickX=0;
        let canvasRightClickY=0;
        let rightClickX=0;
        let rightClickY=0;

        $(document).ready(function () {
            $("#"+canvasName).mouseover(function (e) {
                if(e.offsetX > 100) {
                    x = e.offsetX;
                }
                if(e.offsetY > 100) {
                    y=e.offsetY;
                }
            });
            $("#"+canvasName).on("contextmenu", function(e){
                console.log("Context Menu Mouse click:" + e.offsetX + "," + e.offsetY);
                canvasRightClickX=e.offsetX;
                canvasRightClickY=e.offsetY;
                rightClickX=e.pageX;
                rightClickY=e.pageY;
                return false;
            });
        });

        class FigureLite {
            constructor(name, x, y, width, height, type, atttributes) {
                this.identifier = name;
                this.x = x;
                this.y = y;
                this.width = width;
                this.height= height;
                this.type = type;
                this.attributes = JSON.stringify(atttributes);
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


            _this.getSelection().each((i, figure)=>{
                figures.push(new FigureLite(figure.getId(), figure.x, figure.y, figure.getWidth()
                    , figure.getHeight(), figure.NAME, figure.getPersistentAttributes()));
            });

            let container = new Container(figures,  designer.$connector.designer.getRightMouseX(),  designer.$connector.designer.getRightMouseY()
                , rightClickX, rightClickY);

            console.log(JSON.stringify(container));
            return JSON.stringify(container);
        }


        designer.$connector.addIconNoCoordinates = function (identifier, image, h, w, isClickable) {
            debugger;
            let icon = new draw2d.shape.basic.Image({id: identifier, path: image, width:w, height:h, x:x, y:y, keepAspectRatio: true});

            let inputLocator  = new draw2d.layout.locator.InputPortLocator();
            let outputLocator = new draw2d.layout.locator.OutputPortLocator();


            icon.createPort("hybrid", inputLocator);
            icon.createPort("hybrid", inputLocator);
            icon.createPort("hybrid", inputLocator);

            icon.createPort("hybrid", outputLocator);
            icon.createPort("hybrid", outputLocator);
            icon.createPort("hybrid", outputLocator);

            let ports = icon.getPorts();

            ports.each((i, port) => {
                port.setDiameter(5);
            });

            let command = new draw2d.command.CommandAdd(_this, icon, x, y);
            _this.getCommandStack().execute(command);

            if(isClickable === true) {
                icon.shape.attr({"cursor": "pointer"});
            }

            let figure = designer.$connector.designer.getFigure(icon.getId());
            figure.toFront();
        }

        designer.$connector.addIcon = function (identifier, image, x, y, h, w, showPorts, isClickable) {
            debugger;
            let icon = new draw2d.shape.basic.Image({id: identifier, path: image, width:w, height:h, x:x, y:y, keepAspectRatio: true});

            if(showPorts === true) {
                icon.createPort("input");
                icon.createPort("output");

            }

            let command = new draw2d.command.CommandAdd(_this, icon, x, y);
            _this.getCommandStack().execute(command);

            if(isClickable === true) {
                icon.shape.attr({"cursor": "pointer"});
            }
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

        designer.$connector.addBoundarySimple = function (h, w) {
            let boundary =  new draw2d.shape.basic.Rectangle({
                bgColor:"rgba(255,255,255,0)",
                x: x,
                y: y,
                width: w,
                height: h,
                radius: 10,
            });

            boundary.uninstallEditPolicy(new draw2d.policy.figure.RectangleSelectionFeedbackPolicy());
            boundary.installEditPolicy(new RotateRectangleSelectionFeedbackPolicy());

            let command = new draw2d.command.CommandAdd(_this, boundary, x, y);
            _this.getCommandStack().execute(command);
            boundary.toFront();
        }

        designer.$connector.addBoundaryToShape = function (identifier, shapeIdentifier, x, y, h, w, colour) {
            let boundary =  new draw2d.shape.basic.Rectangle({
                id: identifier,
                bgColor:"rgba(255,255,255,0)",
                color:colour,
                x: x,
                y: y,
                width: w,
                height: h,
                radius: 10,
                stroke: 3,
            });

            boundary.uninstallEditPolicy(new draw2d.policy.figure.RectangleSelectionFeedbackPolicy());
            boundary.installEditPolicy(new RotateRectangleSelectionFeedbackPolicy());

            let command = new draw2d.command.CommandAdd(_this, boundary, x, y);
            _this.getCommandStack().execute(command);

            _this.getFigure(shapeIdentifier).toFront();
        }

        designer.$connector.designer.on("dblclick", function(emitter, event){
            let figure = event.figure;
            let figureLite = new FigureLite(figure.getId(), figure.x, figure.y, figure.getWidth()
                , figure.getHeight(), figure.NAME, figure.getPersistentAttributes());
            let element = document.getElementById(canvasName);
            element.$server.doubleClickEvent(JSON.stringify(figureLite));
        });

        designer.$connector.undo = function () {
            _this.getCommandStack().undo();
        }

        designer.$connector.redo = function () {
            _this.getCommandStack().redo();
        }

        designer.$connector.copy = function () {
            designer.$connector.designer.copy();
        }

        designer.$connector.paste = function () {
            designer.$connector.designer.paste();
        }

        designer.$connector.delete = function () {
            designer.$connector.designer.delete();
        }

        designer.$connector.clear = function () {
            designer.$connector.designer.clear();
        }

        designer.$connector.addTriangle = function (h, w) {
            let triangle = new TriangleFigure({x: x, y:y, width:w, height:h, bgColor:"rgba(255,255,255,0)"});

            let command = new draw2d.command.CommandAdd(_this, triangle, x, y);
            _this.getCommandStack().execute(command);
            triangle.toFront();
        }


        designer.$connector.addOval = function (h, w) {
            let oval =  new draw2d.shape.basic.Oval({width:w,height:h, x:x, y:y, bgColor:"rgba(255,255,255,0)"});

            let command = new draw2d.command.CommandAdd(_this, oval, x, y);
            _this.getCommandStack().execute(command);
        }

        designer.$connector.addCircle = function (d) {
            let circle =new draw2d.shape.basic.Circle({diameter:d, x:x, y:y, bgColor:"rgba(255,255,255,0)"});

            let command = new draw2d.command.CommandAdd(_this, circle, x, y);
            _this.getCommandStack().execute(command);
            circle.toFront();
        }

        designer.$connector.addLabel = function (labelString) {
            let label = new draw2d.shape.basic.Label({
                text: labelString,
                color:"rgba(255,255,255,0)",
                fontColor:"#0d0d0d",
                bgColor:"rgba(255,255,255,0)",
                outlineColor:"rgba(255,255,255,0)",
                fontFamily: "Helvetica",
                fontSize: "12pt",
                x:x, y:y
            });


            label.installEditor(new draw2d.ui.LabelInplaceEditor());

            let command = new draw2d.command.CommandAdd(_this, label, x, y);
            _this.getCommandStack().execute(command);
            label.toFront();
        }

        designer.$connector.setFont = function (font) {
            debugger;
            _this.getFigures().each((i, figure)=>{
                debugger;
                if(figure.isSelected()) {
                    debugger;
                    // figure.setDashArray(pattern);

                    let command = new draw2d.command.CommandAttr(figure, {fontFamily:font});
                    _this.getCommandStack().execute(command);
                }
            });
        }

        designer.$connector.setFontSize = function (fontSize) {
            debugger;
            _this.getFigures().each((i, figure)=>{
                debugger;
                if(figure.isSelected()) {
                    debugger;

                    let command = new draw2d.command.CommandAttr(figure, {fontSize:fontSize});
                    _this.getCommandStack().execute(command);
                }
            });
        }

        designer.$connector.removeFigure = function (figureIdentifier) {
            _this.getFigures().each((i, figure)=>{
                if(figure.id === figureIdentifier) {
                    let command = new draw2d.command.CommandDelete(figure);
                    _this.getCommandStack().execute(command);
                }
            });
        }

        designer.$connector.addLabelToFigure = function (figureIdentifier, labelString) {
            let _figure = _this.getFigure(figureIdentifier);

            if(_figure != null) {

                let x = _figure.x - (_figure.width / 2);
                let y = _figure.y + _figure.getHeight() + 10;
                let label = new draw2d.shape.basic.Label({
                    text: labelString,
                    color: "rgba(255,255,255,0)",
                    fontColor: "#0d0d0d",
                    bgColor: "rgba(255,255,255,0)",
                    outlineColor: "rgba(255,255,255,0)",
                    fontFamily: "Trebuchet MS",
                    fontSize: "12pt",
                    x: x, y: y
                });

                let command = new draw2d.command.CommandAdd(_this, label, x, y);
                _this.getCommandStack().execute(command);

                label.setX(_figure.x - (label.getWidth() / 2) + (_figure.getWidth() / 2));

                let figuresToGroup = new draw2d.util.ArrayList();
                figuresToGroup.add(label);
                figuresToGroup.add(_figure);

                _this.getCommandStack().execute(new draw2d.command.CommandGroup(_this, figuresToGroup));

                label.toFront();
                _figure.toFront();

                _this.getPrimarySelection().toFront();
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
            _this.getSelection().each((i, figure)=>{
                debugger;
                // figure.setDashArray(pattern);

                let command = new draw2d.command.CommandAttr(figure, {dasharray:pattern});
                _this.getCommandStack().execute(command);
            });
        }

        designer.$connector.setTargetDecorator = function (decorator) {
            _this.getSelection().each((i, figure)=>{
                debugger;
                if(figure.NAME === 'draw2d.Connection') {
                    if(decorator === "ARROW"){
                        let arrow = new draw2d.decoration.connection.ArrowDecorator();
                        let command = new draw2d.command.CommandAttr(figure, {targetDecorator:arrow});
                        _this.getCommandStack().execute(command);
                    }
                    else {
                        let circle = new NoDecorator();
                        let command = new draw2d.command.CommandAttr(figure, {targetDecorator:circle});
                        _this.getCommandStack().execute(command);
                    }
                }
            });
        }

        designer.$connector.setSourceDecorator = function (decorator) {
            _this.getSelection().each((i, figure)=>{
                debugger;
                if(figure.NAME === 'draw2d.Connection') {
                    if(decorator === "ARROW"){
                        let arrow = new draw2d.decoration.connection.ArrowDecorator();
                        let command = new draw2d.command.CommandAttr(figure, {sourceDecorator:arrow});
                        _this.getCommandStack().execute(command);
                    }
                    else {
                        let circle = new NoDecorator();
                        let command = new draw2d.command.CommandAttr(figure, {sourceDecorator:circle});
                        _this.getCommandStack().execute(command);
                    }
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

            _this.getFigures().each((i, figure)=>{
                console.log(i + " init " +figure.getId());
                if(figure.NAME === 'draw2d.shape.basic.Image') {
                    figure.setKeepAspectRatio(true);
                    // We want to bring images to the front so that
                    // they can be double clicked!
                    figure.toFront();
                }
            });

            let xCoords = [];
            let yCoords = [];
            _this.getFigures().each(function(i,f){
                let b = f.getBoundingBox();
                xCoords.push(b.x, b.x+b.w);
                yCoords.push(b.y, b.y+b.h);
            });

            let minX   = Math.min.apply(Math, xCoords);
            let minY   = Math.min.apply(Math, yCoords);
            let width  = Math.max.apply(Math, xCoords)-minX;
            let height = Math.max.apply(Math, yCoords)-minY;

            x = minX + 100;
            y = minY + 100;

            let widthZoomFactor = width / 1500;
            let heightZoomFactor = height / 800;

            let zoomFactor = 0;

            if(widthZoomFactor > heightZoomFactor) {
                zoomFactor = widthZoomFactor;
            }
            else {
                zoomFactor = heightZoomFactor;
            }

            if (zoomFactor < 1) {
                zoomFactor = 1;
            }


            designer.$connector.designer.setZoom(zoomFactor)
            designer.$connector.designer.scrollTo((minY/zoomFactor)-((800-(height/zoomFactor))/4), (minX-100)/zoomFactor);
        }

        designer.$connector.manageClickableItems = function () {
            let _figures = _this.getFigures();
            _figures.each((i, figure)=>{
                if(figure.NAME === 'draw2d.shape.basic.Image') {
                    if(figure.getId().startsWith("FLOW")) {
                        debugger;
                        figure.shape.attr({"cursor": "pointer"});
                    }
                }

                return true;
            });
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

        designer.$connector.setReadOnly = function (readonly) {
            debugger;
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

    }
}
