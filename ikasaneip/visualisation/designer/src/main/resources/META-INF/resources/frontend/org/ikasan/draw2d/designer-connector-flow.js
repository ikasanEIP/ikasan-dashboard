
window.Vaadin.Flow.designerConnector = {

    /**
     * Initializes the designer with a given canvas name and read-only status.
     *
     * @param {Object} designer - The designer object to initialize
     * @param {string} name - The name of the canvas element
     * @param {boolean} readonly - Indicates whether the designer is read-only
     */
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

        let cursor_x = -1;
        let cursor_y = -1;

        /**
         * Adds an event listener to capture mouse movement on the document.
         *
         * @param {MouseEvent} event - The MouseEvent object representing the mouse movement
         * @returns {void}
         */
        document.onmousemove = function(event)
        {
            cursor_x = event.pageX;
            cursor_y = event.pageY;
        }

        $(document).ready(function () {
            $("#"+canvasName).mouseover(function (e) {
                if(e.offsetX > 100) {
                    x = e.offsetX;
                }
                if(e.offsetY > 100) {
                    y=e.offsetY;
                }
            });
            $("#"+canvasName).on("mousemove", function(event) {
                cursor_x = event.pageX;
                cursor_y = event.pageY;
            });
            $("#"+canvasName).on("contextmenu", function(e){
                canvasRightClickX=e.offsetX;
                canvasRightClickY=e.offsetY;
                rightClickX=e.pageX;
                rightClickY=e.pageY;
                return false;
            });
        });

        /**
         * Represents a basic figure with lite properties.
         */
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

        /**
         * Represents an event related to connecting figures in a canvas.
         * @constructor
         * @param {Object} canvasJson - The JSON representation of the canvas.
         * @param {string} eventType - The type of the connection event.
         * @param {string} sourceFigureId - The ID of the source figure involved in the connection.
         * @param {Object} sourceUserData - Additional user data associated with the source figure.
         * @param {string} targetFigureId - The ID of the target figure involved in the connection.
         * @param {Object} targetUserData - Additional user data associated with the target figure.
         */
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

        /**
         * Represents an event when a canvas is updated.
         *
         * @constructor
         * @param {Object} canvasJson - The JSON representation of the updated canvas.
         */
        class CanvasUpdatedEvent {
            constructor(canvasJson) {
                this.canvasJson = canvasJson;
            }
        }

        /**
         * Represents a Container that holds figures within specified dimensions and position in a window.
         */
        class Container {
            constructor(figures, x, y, windowx, windowy) {
                this.figures = figures;
                this.x = x;
                this.y = y;
                this.windowx = windowx;
                this.windowy = windowy;
            }

        }

        /**
         * Retrieves the currently selected item from the designer connector.
         * @function getSelected
         * @memberof designer.$connector
         * @returns {Object} The currently selected item from the designer connector.
         */
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

        document.getElementById(name).addEventListener("wheel", (event) => {
            designer.$connector.zoom(event);
        });


        /**
         * Function to connect an icon without coordinates to the designer tool.
         *
         * @param {string} icon - The icon to be connected.
         * @returns {void}
         */
        designer.$connecIconNoCoordinates = function (identifier, image, h, w, isClickable) {

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

        /**
         * Adds an image figure to the designer canvas through the connector interface.
         * This method should be used to add an image figure to the designer canvas.
         *
         * @function addImageFigure
         * @memberof designer.$connector
         * @param {string} imageSrc - The source URL of the image to be added as a figure.
         * @returns {void}
         */
        designer.$connector.addImageFigure = function (image) {
            let attributes = JSON.parse(image);
            let icon = new draw2d.shape.basic.Image(attributes);

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

            let x = (_this.getScrollLeft() * _this.getZoom()) + (1000 * _this.getZoom());
            let y = (_this.getScrollTop() * _this.getZoom()) + (500 * _this.getZoom());

            icon.setX(x);
            icon.setY(y);

            let command = new draw2d.command.CommandAdd(_this, icon, x, y);
            _this.getCommandStack().execute(command);
        }

        /**
         * Adds a new connection to the designer interface.
         */
        designer.$connector.addConnection = function (connectionAttributes) {
            let attributes = JSON.parse(connectionAttributes);
            let connection = new draw2d.Connection(attributes);

            _this.designer.add(connection);
        }

        /**
         * Adds an icon to the connector without specifying coordinates.
         * This method is used by the designer to add an icon to the connector on the canvas without specifying the exact position.
         * The icon will be placed in a default position determined by the connector implementation.
         */
        designer.$connector.addIconNoCoordinates = function (identifier, image, h, w, isClickable) {

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


        /**
         * Adds an icon to the designer connector.
         * This function is responsible for adding an icon to the designer connector.
         */
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

        /**
         * Increases the zoom level of the designer canvas.
         *
         * @function designer.$connector.zoomIn
         */
        designer.$connector.zoomIn = function () {
            designer.$connector.designer.setZoom(designer.$connector.designer.getZoom()*0.95,true);
            scrollToCenter(0.95);
        }

        /**
         * Zooms out the designer canvas by decreasing the zoom level.
         * This method is accessed through the $connector of the designer object.
         */
        designer.$connector.zoomOut = function () {
            designer.$connector.designer.setZoom(designer.$connector.designer.getZoom()*1.05,true);
            scrollToCenter(1.05);
        }

        /**
         * Represents the zoom factor of the connector in the designer module.
         * The zoom factor determines the magnification level of the connector.
         * @type {number}
         */
        designer.$connector.zoomFactor = function (factor) {
            designer.$connector.designer.setZoom(designer.$connector.designer.getZoom()*factor,true);
            scrollToCenter(factor);
        }

        /**
         * Bring the connector element to the front of the z-index stack.
         * This method takes no parameters, and is used to visually bring the connector
         * element to the front of all other elements in the designer interface.
         */
        designer.$connector.bringToFront = function () {
            _this.getFigures().each((i, figure)=>{
                if(figure.isSelected()) {
                    figure.toFront();
                }
            });
        }

        /**
         * Represents the zoom level of the connector in the designer module.
         * The zoom level determines the scale at which the connector is displayed.
         * @type {number}
         */
        designer.$connector.zoom = function(event) {

            event.preventDefault();

            if(event.deltaY > 0) {
                designer.$connector.zoomFactor(1.15);
            }
            else {
                designer.$connector.zoomFactor(0.90);
            }
        }

        /**
         * Send the element to the back of the rendering order within its parent container.
         *
         * @param {Object} element - The element to be sent to the back.
         */
        designer.$connector.sendToBack = function () {
            _this.getFigures().each((i, figure)=>{
                if(figure.isSelected()) {
                    figure.toBack();
                }
            });
        }

        /**
         * Connects the designer object to a group.
         * This method establishes a connection between the designer and a specific group.
         * @function designer.$connector.group
         * @param {Object} designer - The designer object to connect.
         * @param {string} group - The group to connect the designer to.
         * @returns {void}
         */
        designer.$connector.group = function () {
            _this.getCommandStack().execute(new draw2d.command.CommandGroup(_this, _this.getSelection()))
        }

        /**
         * Removes the grouping of the selected shapes in the designer.
         */
        designer.$connector.ungroup = function () {
            _this.getCommandStack().execute(new draw2d.command.CommandUngroup(_this, _this.getSelection()))
        }

        /**
         * Rotates the connector of the given designer.
         *
         * @param {Object} designer - The designer whose connector is to be rotated.
         * @returns {void}
         */
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

        /**
         * Connects a boundary element to a specified target element using a simple method.
         * This method is used by the designer tool for establishing connectivity with UI elements.
         */
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

        /**
         * Adds a styled boundary to the connector of the designer.
         * The styled boundary serves as a visual indicator or separator within the connector.
         */
        designer.$connector.addBoundaryStyled = function (id, h, w, dashArray, colour, stroke) {
            let _x = (_this.getScrollLeft() * _this.getZoom()) + (1000 * _this.getZoom());
            let _y = (_this.getScrollTop() * _this.getZoom()) + (500 * _this.getZoom());

            debugger;
            let boundary =  new draw2d.shape.basic.Rectangle({
                bgColor:"rgba(255,255,255,0)",
                x: _x,
                y: _y,
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

            let command = new draw2d.command.CommandAdd(_this, boundary, _x, _y);
            _this.getCommandStack().execute(command);

            _this.getFigure(id).toBack();
        }

        /**
         * Adds a boundary to the given shape in the Designer tool.
         */
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
            let figureLite = new FigureLite(figure.id, $(':hover').last().offset().left, $(':hover').last().offset().top, figure.getWidth()
                , figure.getHeight(), figure.NAME, figure.getPersistentAttributes(), figure.getUserData());
            let element = document.getElementById(canvasName);
            element.$server.doubleClickEvent(JSON.stringify(figureLite));
        });

        designer.$connector.designer.on("click", function(emitter, event){
            let figure = event.figure;
            let figureLite = new FigureLite(figure.id, $(':hover').last().offset().left, $(':hover').last().offset().top, figure.getWidth()
                , figure.getHeight(), figure.NAME, figure.getPersistentAttributes(), figure.getUserData());
            let element = document.getElementById(canvasName);
            element.$server.clickEvent(JSON.stringify(figureLite));
            console.log("Click on element - " + figureLite);
        });

        designer.$connector.designer.on("contextmenu", function(emitter, event){
            let figure = event.figure;

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
                fontFamily: "Roboto Mono",
                fontSize: "12pt",
                x:x, y:y
            });


            label.installEditor(new draw2d.ui.LabelInplaceEditor());

            let command = new draw2d.command.CommandAdd(_this, label, x, y);
            _this.getCommandStack().execute(command);
        }

        designer.$connector.setFont = function (font) {

            _this.getFigures().each((i, figure)=>{

                if(figure.isSelected()) {

                    // figure.setDashArray(pattern);

                    let command = new draw2d.command.CommandAttr(figure, {fontFamily:font});
                    _this.getCommandStack().execute(command);
                }
            });
        }

        designer.$connector.setFontSize = function (fontSize) {

            _this.getFigures().each((i, figure)=>{

                if(figure.isSelected()) {


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
                    fontFamily: "Roboto Mono",
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
                    fontFamily: "Roboto Mono",
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

        /**
         * Adds a label element to the designer with the specified coordinates.
         *
         * @param {number} x - The x-coordinate for the label element.
         * @param {number} y - The y-coordinate for the label element.
         */
        designer.$connector.addLabelWithCoordinates = function (labelString, x, y) {
            let label = new draw2d.shape.basic.Label({
                text: labelString,
                color: "rgba(255,255,255,0)",
                fontColor: "#0d0d0d",
                bgColor: "rgba(255,255,255,0)",
                outlineColor: "rgba(255,255,255,0)",
                fontFamily: "Roboto Mono",
                fontSize: "12pt",
                x: x, y: y
            });

            let command = new draw2d.command.CommandAdd(_this, label, x, y);
            _this.getCommandStack().execute(command);
        }


        /**
         * Sets the background color of the connector in the designer.
         *
         * @param {string} color - The color to set as the background color of the connector.
         * @returns {void}
         */
        designer.$connector.setBackgroundColor = function (color) {

            _this.getSelection().each((i, figure)=>{

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

        /**
         * Sets the background color on a given figure within the designer.
         */
        designer.$connector.setBackgroundColorOnFigure = function (figureIdentifier, color) {
            _this.getFigures().each((i, figure)=>{
                if(figure.id === figureIdentifier) {
                    figure.setBackgroundColor(color);
                    figure.repaint();
                }
            });
        }

        /**
         * Set the line type for the connector in the designer.
         * This method allows the designer to specify the type of line to be used for the connector.
         * The line type can affect the appearance and behavior of the connector when rendered in the designer.
         */
        designer.$connector.setLineType = function (pattern) {

            _this.getSelection().each((i, figure)=>{
                let command = new draw2d.command.CommandAttr(figure, {dasharray:pattern});
                _this.getCommandStack().execute(command);
            });
        }

        /**
         * Sets the target decorator for the connector in the designer.
         * The target decorator is used to visually represent the connection endpoint
         * on the target element in the designer.
         */
        designer.$connector.setTargetDecorator = function (decorator) {
            _this.getSelection().each((i, figure)=>{

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

        /**
         * Sets the source decorator for the connector in the Designer.
         * The source decorator is responsible for visually representing the starting point of the connector.
         */
        designer.$connector.setSourceDecorator = function (decorator) {
            _this.getSelection().each((i, figure)=>{

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


        /**
         * Sets the radius for the connector of the designer object.
         */
        designer.$connector.setRadius = function (radius) {

            _this.getFigures().each((i, figure)=>{

                if(figure.isSelected()) {

                    figure.setRadius(radius);
                }
            });
        }


        /**
         * Set the stroke color for the connector in the designer.
         */
        designer.$connector.setStroke = function (width) {
            _this.getFigures().each((i, figure)=>{
                if(figure.isSelected()) {
                    figure.setStroke(width);
                }
            });
        }

        /**
         * Exports JSON data from the connector in the Designer module.
         * This function is responsible for exporting JSON data from the designer's connector.
         * It allows for retrieving data in JSON format for further processing or external use.
         */
        designer.$connector.exportJson = function () {

            let writer = new draw2d.io.json.Writer();
            let result = null;
            writer.marshal(designer.$connector.designer, function(json){
                result = JSON.stringify(json,null,2);
            });

            return result;
        }

        /**
         * Starts the spinner for the given jQuery object with the `$connector.startSpinner` method of the `designer` object.
         * This function is used to show a loading spinner associated with a specific jQuery element.
         * Note: Make sure the jQuery object contains the necessary elements for the spinner to display properly.
         */
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

        /**
         * Stops the spinner animation of the designer connector.
         */
        designer.$connector.stopSpinner = function() {
            if(spinner != null) spinner.stop();
            spinner = null;
        }

        /**
         * Imports data from a JSON object into the designer's connector component.
         * This function allows the designer to load JSON data into the connector for further processing or visualization.
         * @param {Object} data - The JSON object containing the data to be imported.
         * @returns {void}
         */
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

            figures.each((i, figure) => {
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
                figure.onMouseEnter = function () {
                    if(figure.getUserData() != null &&
                        figure.getUserData().itemType != null &&
                        (figure.getUserData().itemType === 'INTERNAL_EVENT_DRIVEN_JOB' ||
                            figure.getUserData().itemType === 'QUARTZ_EVENT_DRIVEN_JOB'||
                            figure.getUserData().itemType === 'FILE_EVENT_DRIVEN_JOB' ||
                            figure.getUserData().itemType === 'BRIDGING_JOB')) {
                        console.log("mouse dragged onto " + figure.getUserData().jobName);
                        console.log(cursor_x);
                        console.log(cursor_y);
                        let element = document.getElementById(canvasName);
                        let figureLite = new FigureLite(figure.id, cursor_x, cursor_y, figure.getWidth()
                            , figure.getHeight(), figure.NAME, figure.getPersistentAttributes(), figure.getUserData());
                        element.$server.showJobDetails(JSON.stringify(figureLite));
                    }
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
                else if(e.getCommand().getLabel() === "Move Shape" && e.action === "POST_EXECUTE") {
                    debugger;
                    console.log("MOVE EVENT!");
                    if (e.getCommand().figure != null && e.getCommand().figure.assignedFigures != null) {
                        // send a context moved back to the server
                        e.getCommand().figure.assignedFigures.each((i, figure) => {
                            if (figure.NAME === "draw2d.shape.basic.Image" &&
                                figure.getUserData() != null &&
                                figure.getUserData().itemType === 'CONTEXT') {
                                let figureLite = new FigureLite(figure.id, $(':hover').last().offset().left, $(':hover').last().offset().top, figure.getWidth()
                                    , figure.getHeight(), figure.NAME, figure.getPersistentAttributes(), figure.getUserData());
                                let element = document.getElementById(canvasName);
                                element.$server.figureMoved(JSON.stringify(figureLite));
                            }
                        });
                    }
                }
            });

            console.log("finished import json " + performance.now());

            element.$server.canvasInitialised();
        }

        /**
         * Moves the scroll position of the designer element to the center with a specified factor.
         *
         * @param {number} factor - Factor to determine the scroll position. A value less than 1 will move the scroll position towards the left, while a value greater than or equal to 1 will
         * move it towards the right.
         *
         * @return {void} - This function does not return any value.
         */
        function scrollToCenter(factor) {
            let scrollTopFactor = designer.$connector.designer.getScrollTop() * (1/factor);
            let scrollLeftFactor = 0;

            if(factor < 1) {
                scrollLeftFactor = designer.$connector.designer.getScrollLeft() + (designer.$connector.designer.getScrollLeft() - designer.$connector.designer.getScrollLeft() * factor)  * ((1/factor) ** 6);
            }
            else {
                scrollLeftFactor = designer.$connector.designer.getScrollLeft() + (designer.$connector.designer.getScrollLeft() - designer.$connector.designer.getScrollLeft() * factor)  * ((factor) ** 6);
            }


            designer.$connector.designer.scrollTo(scrollTopFactor, scrollLeftFactor);
        }

        /**
         * Exports the local JSON representation of the designer.
         *
         * @return {string} The local JSON representation of the designer with proper indentations.
         */
        function exportJsonLocal() {

            let writer = new draw2d.io.json.Writer();
            let result = null;
            writer.marshal(designer.$connector.designer, function(json){
                result = JSON.stringify(json,null,2);
            });

            return result;
        }

        /**
         * Manages clickable items for the designer component.
         * This function is responsible for handling the behavior and interactions of clickable items within the designer.
         * It provides functionality to manage the state and actions related to these items.esigner.$connector
         */
        designer.$connector.manageClickableItems = function () {

            let _figures = _this.getFigures();
            _figures.each((i, figure)=>{
                if(figure.NAME === 'draw2d.shape.basic.Image') {

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
        }

        designer.$connector.getPng = function () {
            return pngResult;
        }

        designer.$connector.setReadOnly = function (readonly) {
            console.log("Setting readonly - " + readonly);
            if(readonly === true){
                designer.$connector.designer.uninstallEditPolicy( new draw2d.policy.canvas.FadeoutDecorationPolicy());
                designer.$connector.designer.uninstallEditPolicy( new draw2d.policy.canvas.SnapToGeometryEditPolicy());
                designer.$connector.designer.uninstallEditPolicy( new draw2d.policy.canvas.SnapToCenterEditPolicy());
                designer.$connector.designer.uninstallEditPolicy( new draw2d.policy.canvas.SnapToInBetweenEditPolicy());
                designer.$connector.designer.installEditPolicy( new draw2d.policy.canvas.ReadOnlySelectionPolicy());
                console.log("Successfully set readonly - " + readonly);
            }
            else {
                designer.$connector.designer.installEditPolicy( new draw2d.policy.canvas.FadeoutDecorationPolicy());
                designer.$connector.designer.installEditPolicy( new draw2d.policy.canvas.SnapToGeometryEditPolicy());
                designer.$connector.designer.installEditPolicy( new draw2d.policy.canvas.SnapToCenterEditPolicy());
                designer.$connector.designer.installEditPolicy( new draw2d.policy.canvas.SnapToInBetweenEditPolicy());
                designer.$connector.designer.uninstallEditPolicy( new draw2d.policy.canvas.ReadOnlySelectionPolicy());
                console.log("Successfully set readonly - " + readonly);
            }
        }
    }
}
