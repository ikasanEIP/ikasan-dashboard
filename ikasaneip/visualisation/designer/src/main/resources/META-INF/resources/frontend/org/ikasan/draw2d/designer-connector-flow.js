
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
        let spinner=null;

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
            constructor(name, x, y, width, height, type, atttributes, userData) {
                this.identifier = name;
                this.x = x;
                this.y = y;
                this.width = width;
                this.height= height;
                this.type = type;
                this.attributes = JSON.stringify(atttributes);
                this.userData = userData;
            }

        }

        class ConnectionEvent {
            constructor(canvasJson, eventType, sourceFigureId, sourceUserData, targetFigureId, targetUserData) {
                this.canvasJson = canvasJson;
                this.eventType = eventType;
                this.sourceFigureId = sourceFigureId;
                this.sourceUserData = sourceUserData;
                this.targetFigureId = targetFigureId;
                this.targetUserData = targetUserData;
            }
        }

        class CanvasUpdatedEvent {
            constructor(canvasJson) {
                this.canvasJson = canvasJson;
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
                    , figure.getHeight(), figure.NAME, figure.getPersistentAttributes(), figure.getUserData()));
            });

            let container = new Container(figures,  designer.$connector.designer.getRightMouseX(),  designer.$connector.designer.getRightMouseY()
                , rightClickX, rightClickY);

            console.log(JSON.stringify(container));
            return JSON.stringify(container);
        }


        designer.$connecIconNoCoordinates = function (identifier, image, h, w, isClickable) {
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
                if(readonly) {
                    port.setDiameter(0);
                }
                else {
                    port.setDiameter(5);
                }
            });

            let command = new draw2d.command.CommandAdd(_this, icon, x, y);
            _this.getCommandStack().execute(command);

            if(isClickable === true) {
                icon.shape.attr({"cursor": "pointer"});
            }
        }

        designer.$connector.addImageFigure = function (image) {
            let attributes = JSON.parse(image);
            let icon = new draw2d.shape.basic.Image(attributes);

            debugger;
            console.log(icon);

            attributes.ports.forEach(function (port, index) {
                let type = "hybrid";

                if(port.type != null) {
                    if(port.type == "draw2d.InputPort") {
                        type = "input";
                    }
                    if(port.type == "draw2d.OutputPort") {
                        type = "output";
                    }
                }

                if(port.locator === "draw2d.layout.locator.RightLocator") {
                    icon.createPort(type, new draw2d.layout.locator.RightLocator());
                }
                else if(port.locator === "draw2d.layout.locator.LeftLocator") {
                    icon.createPort(type, new draw2d.layout.locator.LeftLocator());
                }
                else if(port.locator === "draw2d.layout.locator.TopLocator") {
                    icon.createPort(type, new draw2d.layout.locator.TopLocator());
                }
                else if(port.locator === "draw2d.layout.locator.BottomLocator") {
                    icon.createPort(type, new draw2d.layout.locator.BottomLocator());
                }
            });

            let xCoords = [];
            let yCoords = [];
            _this.getFigures().each(function (i, f) {
                let b = f.getBoundingBox();
                xCoords.push(b.x, b.x + b.w);
                yCoords.push(b.y, b.y + b.h);
            });

            if(xCoords.length === 0) {
                xCoords = [100, 200]
            }

            if(yCoords.length === 0) {
                yCoords = [100, 200]
            }

            let minX = Math.min.apply(Math, xCoords);
            let minY = Math.min.apply(Math, yCoords);
            let x = (Math.max.apply(Math, xCoords) + minX);
            let y = (Math.max.apply(Math, yCoords) + minY) / 2;

            icon.setX(x);
            icon.setY(y);

            let command = new draw2d.command.CommandAdd(_this, icon, x, y);
            _this.getCommandStack().execute(command);
        }

        designer.$connector.addConnection = function (connectionAttributes) {
            let attributes = JSON.parse(connectionAttributes);
            let connection = new draw2d.Connection(attributes);
            console.log(connection);

            _this.designer.add(connection);
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
        }

        designer.$connector.addIcon = function (identifier, image, x, y, h, w, showPorts, isClickable) {
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

        designer.$connector.zoomIn = function () {
            designer.$connector.designer.setZoom(designer.$connector.designer.getZoom()*0.8,true);
        }

        designer.$connector.zoomOut = function () {
            designer.$connector.designer.setZoom(designer.$connector.designer.getZoom()*1.2,true);
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
        }

        designer.$connector.addBoundaryStyled = function (id, h, w, dashArray, colour, stroke) {
            let boundary =  new draw2d.shape.basic.Rectangle({
                bgColor:"rgba(255,255,255,0)",
                x: x,
                y: y,
                width: w,
                height: h,
                radius: 10,
                id: id,
                dasharray: dashArray,
                color: colour,
                stroke: stroke,
                resizable:true,
                selectable:true,
                draggable:true
            });

            let command = new draw2d.command.CommandAdd(_this, boundary, x, y);
            _this.getCommandStack().execute(command);

            _this.getFigure(id).toBack();
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
            debugger;
            let figure = event.figure;
            let figureLite = new FigureLite(figure.id, $(':hover').last().offset().left, $(':hover').last().offset().top, figure.getWidth()
                , figure.getHeight(), figure.NAME, figure.getPersistentAttributes(), figure.getUserData());
            let element = document.getElementById(canvasName);
            element.$server.doubleClickEvent(JSON.stringify(figureLite));
        });

        designer.$connector.designer.on("click", function(emitter, event){
            debugger;
            let figure = event.figure;
            let figureLite = new FigureLite(figure.id, $(':hover').last().offset().left, $(':hover').last().offset().top, figure.getWidth()
                , figure.getHeight(), figure.NAME, figure.getPersistentAttributes(), figure.getUserData());
            let element = document.getElementById(canvasName);
            element.$server.clickEvent(JSON.stringify(figureLite));
            console.log("Click on element - " + figureLite);
        });

        designer.$connector.designer.on("contextmenu", function(emitter, event){
            let figure = event.figure;
            debugger;
            let figureLite = new FigureLite(figure.id, $(':hover').last().offset().left, $(':hover').last().offset().top, figure.getWidth()
                , figure.getHeight(), figure.NAME, figure.getPersistentAttributes(), figure.getUserData());
            let element = document.getElementById(canvasName);
            element.$server.rightClickEvent(JSON.stringify(figureLite));
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
            designer.$connector.designer.reset();
        }

        designer.$connector.addTriangle = function (h, w) {
            let triangle = new TriangleFigure({x: x, y:y, width:w, height:h, bgColor:"rgba(255,255,255,0)"});

            let command = new draw2d.command.CommandAdd(_this, triangle, x, y);
            _this.getCommandStack().execute(command);
        }

        designer.$connector.addTriangle = function (x, y, h, w, backgroundColour) {
            let triangle = new TriangleFigure({x: x, y:y, width:w, height:h, bgColor:backgroundColour});

            let command = new draw2d.command.CommandAdd(_this, triangle, x, y);
            _this.getCommandStack().execute(command);
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

        designer.$connector.deselectAllFigures = function () {
            _this.getSelection().clear();
        }

        designer.$connector.addLabelToFigure = function (figureIdentifier, labelString) {
            let _figure = null;
            debugger;
            _this.getFigures().each((i, figure)=>{
                if(figure.id === figureIdentifier) {
                    _figure = figure;
                }
            });

            if(_figure != null) {

                let x = _figure.x - (_figure.width / 2);
                let y = _figure.y + _figure.getHeight() + 10;
                let label = new draw2d.shape.basic.Label({
                    text: labelString,
                    color: "rgba(255,255,255,0)",
                    fontColor: "#0d0d0d",
                    bgColor: "rgba(255,255,255,0)",
                    outlineColor: "rgba(255,255,255,0)",
                    fontFamily: "Arial, Helvetica, sans-serif",
                    fontSize: "14pt",
                    x: x, y: y
                });

                let command = new draw2d.command.CommandAdd(_this, label, x, y);
                _this.getCommandStack().execute(command);

                label.setX(_figure.x - (label.getWidth() / 2) + (_figure.getWidth() / 2));

                let figuresToGroup = new draw2d.util.ArrayList();
                figuresToGroup.add(label);
                figuresToGroup.add(_figure);

                _this.getCommandStack().execute(new draw2d.command.CommandGroup(_this, figuresToGroup));

                _this.getFigures().each((i, figure) => {
                    if (figure.NAME === 'draw2d.shape.basic.Image' || figure.NAME === 'draw2d.shape.composite.Group') {
                        console.log("to front " + figure.NAME + " " + figure.id);
                        figure.setKeepAspectRatio(true);
                        // We want to bring images to the front so that
                        // they can be double clicked!
                        figure.toFront();
                    } else {
                        console.log("to back " + figure.NAME + " " + figure.id);
                        figure.toBack();
                    }
                });
            }
        }

        designer.$connector.addLabelToFigureWithFontSize = function (figureIdentifier, labelString, fontSize) {
            let _figure = null;
            debugger;
            _this.getFigures().each((i, figure)=>{
                if(figure.id === figureIdentifier) {
                    _figure = figure;
                }
            });

            if(_figure != null) {

                let x = _figure.x - (_figure.width / 2);
                let y = _figure.y + _figure.getHeight() + 10;
                let label = new draw2d.shape.basic.Label({
                    text: labelString,
                    color: "rgba(255,255,255,0)",
                    fontColor: "#0d0d0d",
                    bgColor: "rgba(255,255,255,0)",
                    outlineColor: "rgba(255,255,255,0)",
                    fontFamily: "Arial, Helvetica, sans-serif",
                    fontSize: fontSize,
                    x: x, y: y
                });

                let command = new draw2d.command.CommandAdd(_this, label, x, y);
                _this.getCommandStack().execute(command);

                label.setX(_figure.x - (label.getWidth() / 2) + (_figure.getWidth() / 2));

                let figuresToGroup = new draw2d.util.ArrayList();
                figuresToGroup.add(label);
                figuresToGroup.add(_figure);

                _this.getCommandStack().execute(new draw2d.command.CommandGroup(_this, figuresToGroup));

                _this.getFigures().each((i, figure) => {
                    if (figure.NAME === 'draw2d.shape.basic.Image' || figure.NAME === 'draw2d.shape.composite.Group') {
                        console.log("to front " + figure.NAME + " " + figure.id);
                        figure.setKeepAspectRatio(true);
                        // We want to bring images to the front so that
                        // they can be double clicked!
                        figure.toFront();
                    } else {
                        console.log("to back " + figure.NAME + " " + figure.id);
                        figure.toBack();
                    }
                });
            }
        }

        designer.$connector.addImageToFigure = function (figureIdentifier, iconIdentifier, image, h, w) {
            let _figure = null;
            debugger;
            _this.getFigures().each((i, figure)=>{
                if(figure.id === figureIdentifier) {
                    _figure = figure;
                }
            });

            if(_figure != null) {
                if(_figure.getUserData() != null
                    && _figure.getUserData().itemType == "CONTEXT") {
                    let x = _figure.x + _figure.width + 60;
                    let y = _figure.y - (_figure.getHeight() / 2) - 60;

                    this.addIcon(iconIdentifier, image, x, y, h, w, false, false);
                }
                else {
                    let x = _figure.x + _figure.width + 10;
                    let y = _figure.y - (_figure.getHeight() / 2) - 10;

                    this.addIcon(iconIdentifier, image, x, y, h, w, false, false);
                }
            }
        }

        designer.$connector.addTriangleToFigure = function (figureIdentifier, h, w, backgroundColour) {
            let _figure = null;
            debugger;
            _this.getFigures().each((i, figure)=>{
                if(figure.id === figureIdentifier) {
                    _figure = figure;
                }
            });

            if(_figure != null) {

                let x = _figure.x - 10;
                let y = _figure.y - (_figure.getHeight() / 2) - 10 ;

                this.addTriangle(iconIdentifier, x, y, h, w, backgroundColour);
            }
        }

        /**
         * Add a boundary to an existing figure and optionally
         * scroll to and zoom onto the figure.
         *
         * @param figureIdentifier
         * @param itemIdentifier
         * @param h
         * @param w
         * @param lineFormat
         * @param backgroundColor
         * @param scrollToFigure
         */
        designer.$connector.addBoundaryToFigure = function (figureIdentifier, itemIdentifier, h, w,
                                                            lineFormat, backgroundColor, scrollToFigure) {
            debugger;
            let _figure = null;
            _this.getFigures().each((i, figure)=>{
                if(figure.id === figureIdentifier) {
                    _figure = figure;
                }
            });

            if(_figure != null) {
                let x = _figure.x - (_figure.width / 2);
                let y = _figure.y - (_figure.getHeight() / 2) ;

                let boundary =  new draw2d.shape.basic.Rectangle({
                    bgColor:backgroundColor,
                    x: x,
                    y: y,
                    width: w,
                    height: h,
                    radius: 10,
                    id: itemIdentifier,
                    dasharray: lineFormat,
                    stroke: 3,
                    resizable:true,
                    selectable:true,
                    draggable:true
                });

                let command = new draw2d.command.CommandAdd(_this, boundary, x, y);
                _this.getCommandStack().execute(command);

                _this.getFigure(itemIdentifier).toBack();

                if(scrollToFigure === true) {
                    let figures = _this.getFigures();
                    let yCoords = [];
                    figures.each(function (i, f) {
                        let b = f.getBoundingBox();
                        yCoords.push(b.y, b.y + b.h);
                    });

                    let minY = Math.min.apply(Math, yCoords);
                    let height = Math.max.apply(Math, yCoords) - minY;

                    let left = (x / 5) - ((800 - (height / 3)) / 4) - (1500 / 2);
                    let top = ((y - 100) / 5) - (1500 / 2);

                    designer.$connector.designer.setZoom(5);
                    designer.$connector.designer.scrollTo(top, left);
                }
            }
        }

        designer.$connector.addLabelWithCoordinates = function (labelString, x, y) {
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

        designer.$connector.setBackgroundColorOnFigure = function (figureIdentifier, color) {
            _this.getFigures().each((i, figure)=>{
                if(figure.id === figureIdentifier) {
                    figure.setBackgroundColor(color);
                    figure.repaint();
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

        designer.$connector.startSpinner = async function() {
            let opts = {
                lines: 13, // The number of lines to draw
                length: 38, // The length of each line
                width: 17, // The line thickness
                radius: 45, // The radius of the inner circle
                scale: 1, // Scales overall size of the spinner
                corners: 1, // Corner roundness (0..1)
                speed: 1, // Rounds per second
                rotate: 0, // The rotation offset
                animation: 'spinner-line-fade-quick', // The CSS animation name for the lines
                direction: 1, // 1: clockwise, -1: counterclockwise
                color: 'rgba(241, 90, 35, 1.0)', // CSS color or array of colors
                fadeColor: 'transparent', // CSS color or array of colors
                top: '50%', // Top position relative to parent
                left: '50%', // Left position relative to parent
                shadow: '0 0 1px transparent', // Box-shadow for the lines
                zIndex: 2000000000, // The z-index (defaults to 2e9)
                className: 'spinner', // The CSS class to assign to the spinner
                position: 'absolute', // Element positioning
            };

            let target = document.getElementById(canvasName);
            spinner = new Spin.Spinner(opts).spin(target);
            await new Promise(r => setTimeout(r, 100));
        }

        designer.$connector.stopSpinner = function() {
            if(spinner != null) spinner.stop();
            spinner = null;
        }

        designer.$connector.importJson = async function (jsonDocument, toBack) {

            let opts = {
                lines: 13, // The number of lines to draw
                length: 38, // The length of each line
                width: 17, // The line thickness
                radius: 45, // The radius of the inner circle
                scale: 1, // Scales overall size of the spinner
                corners: 1, // Corner roundness (0..1)
                speed: 1, // Rounds per second
                rotate: 0, // The rotation offset
                animation: 'spinner-line-fade-quick', // The CSS animation name for the lines
                direction: 1, // 1: clockwise, -1: counterclockwise
                color: 'rgba(241, 90, 35, 1.0)', // CSS color or array of colors
                fadeColor: 'transparent', // CSS color or array of colors
                top: '50%', // Top position relative to parent
                left: '50%', // Left position relative to parent
                shadow: '0 0 1px transparent', // Box-shadow for the lines
                zIndex: 2000000000, // The z-index (defaults to 2e9)
                className: 'spinner', // The CSS class to assign to the spinner
                position: 'absolute', // Element positioning
            };


            let reader = new draw2d.io.json.Reader();

            let target = document.getElementById(canvasName);
            let spinner = new Spin.Spinner(opts).spin(target);
            await new Promise(r => setTimeout(r, 100));

            console.log("before unmarshal " + performance.now());
            let figures = reader.unmarshal(designer.$connector.designer, jsonDocument);
            console.log("after unmarshal " + performance.now());

            debugger;

            figures.each((i, figure) => {
                debugger;
                if (figure.NAME === 'draw2d.shape.basic.Image' || figure.NAME === 'draw2d.shape.composite.Group') {
                    console.log("to front " + figure.NAME + " " + figure.id);
                    figure.setKeepAspectRatio(true);
                    // We want to bring images to the front so that
                    // they can be double clicked!
                    figure.toFront();
                } else if(toBack){
                    console.log("to back " + figure.NAME + " " + figure.id);
                    figure.toBack();
                }
            });

            let xCoords = [];
            let yCoords = [];
            figures.each(function (i, f) {
                let b = f.getBoundingBox();
                xCoords.push(b.x, b.x + b.w);
                yCoords.push(b.y, b.y + b.h);
            });

            let minX = Math.min.apply(Math, xCoords);
            let minY = Math.min.apply(Math, yCoords);
            let width = Math.max.apply(Math, xCoords) - minX;
            let height = Math.max.apply(Math, yCoords) - minY;

            let widthZoomFactor = width / 1500;
            let heightZoomFactor = height / 800;

            let zoomFactor = 0;

            if (widthZoomFactor > heightZoomFactor) {
                zoomFactor = widthZoomFactor;
            } else {
                zoomFactor = heightZoomFactor;
            }

            if (zoomFactor < 1) {
                zoomFactor = 1;
            }

            console.log("minX = " + minX);
            console.log("minY = " + minY);
            console.log("width = " + width);
            console.log("height = " + height);
            console.log("zoomfactor = " + zoomFactor);

            let top = (minY / zoomFactor) - ((800 - (height / zoomFactor)) / 4);
            let left = (minX - 100) / zoomFactor;

            console.log("top = " + top);
            console.log("left = " + left);

            designer.$connector.designer.setZoom(zoomFactor);
            designer.$connector.designer.scrollTo(top, left);

            spinner.stop();

            let element = document.getElementById(canvasName);

            designer.$connector.designer.getCommandStack().addEventListener(function(e){
                debugger;
                if(e.getCommand().getLabel() === "Connect Ports" && e.action === "POST_EXECUTE") {
                    element.$server.connectorEvent(JSON.stringify(new ConnectionEvent(exportJsonLocal(), "CONNECTOR_ADDED", e.getCommand().source.parent.getId(), e.getCommand().source.parent.getUserData(),
                        e.getCommand().target.parent.getId(), e.getCommand().target.parent.getUserData())));
                }
                else if(e.getCommand().getLabel() === "Delete Shape" && e.action === "PRE_EXECUTE") {
                    if (e.getCommand().group != null && e.getCommand().group.assignedFigures != null) {
                        // send a connection removed event back to the server
                        e.getCommand().group.assignedFigures.each((i, figure)=> {
                            if(figure.NAME === "draw2d.shape.basic.Image" && figure.getUserData() != null) {
                                let figureLite = new FigureLite(figure.id, figure.getX(), figure.getY(), figure.getWidth()
                                    , figure.getHeight(), figure.NAME, figure.getPersistentAttributes(), figure.getUserData());
                                element.$server.figureDeleted(JSON.stringify(figureLite));
                            }
                        });
                    }
                }
                else if(e.getCommand().getLabel() === "Delete Shape" && e.action === "POST_UNDO") {
                    if (e.getCommand().group != null && e.getCommand().group.assignedFigures != null) {
                        // send a connection removed event back to the server
                        e.getCommand().group.assignedFigures.each((i, figure)=> {
                            if(figure.NAME === "draw2d.shape.basic.Image" && figure.getUserData() != null) {
                                let figureLite = new FigureLite(figure.id, figure.getX(), figure.getY(), figure.getWidth()
                                    , figure.getHeight(), figure.NAME, figure.getPersistentAttributes(), figure.getUserData());
                                element.$server.undoFigureDeleted(JSON.stringify(figureLite));
                            }
                        });
                    }
                }
                else if(e.getCommand().getLabel() === "Delete Shape" && e.action === "POST_REDO") {
                    if (e.getCommand().group != null && e.getCommand().group.assignedFigures != null) {
                        // send a connection removed event back to the server
                        e.getCommand().group.assignedFigures.each((i, figure)=> {
                            if(figure.NAME === "draw2d.shape.basic.Image" && figure.getUserData() != null) {
                                let figureLite = new FigureLite(figure.id, figure.getX(), figure.getY(), figure.getWidth()
                                    , figure.getHeight(), figure.NAME, figure.getPersistentAttributes(), figure.getUserData());
                                element.$server.figureDeleted(JSON.stringify(figureLite));
                            }
                        });
                    }
                }
                else if(e.getCommand().getLabel() === "Delete Shape" && e.action === "POST_EXECUTE") {
                    if (e.getCommand().figure != null && e.getCommand().figure.NAME === "draw2d.Connection") {
                        // send a connection removed event back to the server
                        element.$server.connectorEvent(JSON.stringify(new ConnectionEvent(exportJsonLocal(), "CONNECTOR_REMOVED", e.getCommand().figure.sourcePort.parent.getId(), e.getCommand().source.parent.getUserData(),
                            e.getCommand().figure.targetPort.parent.getId(), e.getCommand().target.parent.getUserData())));
                    }
                }
            });

            console.log("finished import json " + performance.now());

            element.$server.canvasInitialised();
        }

         function exportJsonLocal() {
            debugger;
            let writer = new draw2d.io.json.Writer();
            let result = null;
            writer.marshal(designer.$connector.designer, function(json){
                result = JSON.stringify(json,null,2);
            });

            return result;
        }

        designer.$connector.manageClickableItems = function () {
            debugger;
            let _figures = _this.getFigures();
            _figures.each((i, figure)=>{
                if(figure.NAME === 'draw2d.shape.basic.Image') {
                    debugger;
                    if(figure.getId().startsWith("FLOW")) {
                        figure.shape.attr({"cursor": "pointer"});
                    }
                    else if(figure.getUserData() != null) {
                        figure.shape.attr({"cursor": "pointer"});
                        if(figure.getUserData().itemType === 'INTERNAL_EVENT_DRIVEN_JOB' ||
                            figure.getUserData().itemType === 'QUARTZ_EVENT_DRIVEN_JOB'||
                            figure.getUserData().itemType === 'FILE_EVENT_DRIVEN_JOB') {
                            figure.shape.attr({"title": figure.getUserData().jobName});
                        }
                        else if(figure.getUserData().itemType === 'CONTEXT') {
                            figure.shape.attr({"title": figure.getUserData().contextName});
                        }
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
                debugger;
                // let element = document.getElementById(canvasName);
                // element.$server.stackEvent(JSON.stringify(e));
                if(e.isPostChangeEvent()){
                    _this.exportPng();
                }
            });
        });

    }
}
