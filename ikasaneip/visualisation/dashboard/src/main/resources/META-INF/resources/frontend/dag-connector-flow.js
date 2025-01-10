import NiceDag from '@ebay/nice-dag-core';
import {LitElement, html, css, render} from 'lit';
import Fontawesome from 'lit-fontawesome';

/**
 * Class representing a connector for a Directed Acyclic Graph (DAG).
 */
export class DagConnector extends LitElement {

    static get properties() {
        return {
            dagNodes: { type: Array },
            ikasanDagNodeStyle: {type: String}
        };
    }

    static get styles() {
        return [ Fontawesome ];
    }

    niceDag = null;
    scale = 1;
    x = 0;
    y = 0;
    mousedown = false;
    container = null;
    isZooming = true;

    /**
     * Constructor for creating a new instance of MyClass.
     *
     * @return {void}
     */
    constructor() {
        super();
        this.container = document.createElement("div");
        this.container.id = "my-dag-chart";
        this.container.setAttribute("style", "width:100%; height:100%; display:flex; overflow:auto");
        console.log("constructor called!");
    }

    /**
     * Styles the node with the specified ID using the provided color.
     *
     * @param {string} id - The ID of the node to style.
     * @param {string} colour - The color to apply to the node.
     *
     * @return {void} - This method does not return anything.
     */
    styleNode(id, colour) {
        if(this.niceDag) {
            debugger;
            let element = this.niceDag.getElementByNodeId(id);
            if(element) {
                let container = element.children.namedItem("container");
                if(container) {
                    let statusElement = container.children.namedItem(id + "_status");
                    console.log("Element ID " + id +"_status " + colour);
                    statusElement.style.backgroundColor = colour;
                    this.niceDag.findNodeById(id).data = colour;
                }
            }
        }
    }

    /**
     * Renders the NiceDag chart on the specified container element. If the chart has not been initialized yet,
     * it initializes the chart with the provided configuration and renders it.
     *
     * @return {HTMLElement} The container element on which the NiceDag chart is rendered.
     */
    render() {
        let ikasanMinimapContainer = document.getElementById("ikasanMinimapContainer");

        if(this.niceDag == null) {
            let args
                = {
                id: "my-dag-chart",
                container: this.container,
                // minimapContainer: ikasanMinimapContainer,
                getNodeSize
            };
            this.niceDag = NiceDag.init(args, false);

            // debugger;
            console.log("Attempting to render: " + this.dagNodes);
            this.niceDag = this.niceDag.withNodes(JSON.parse(this.dagNodes));

            this.niceDag.render();
            // let bounds = container.getBoundingClientRect();
            this.niceDag.center({width: 500, height:500});

            let nodes = this.niceDag.getAllNodes(true);
            nodes.forEach((node) => {
                this.renderNode(node, this.niceDag.getElementByNodeId(node.id));
            });

            this.niceDag.addNiceDagChangeListener(this);

            this.addZoomListener();

            addEventListener("dblclick", (event) => {{
                if(this.isZooming) {
                    this.removeZoomListener();
                }
                else {
                    this.addZoomListener();
                }
            }});
        }

        console.log("render method called!");
        return this.container;
    }

    /**
     * Handle zoom functionality based on the scroll event.
     *
     * @param {Event} event - The scroll event triggering the zoom operation.
     *
     * @return {void} - This method does not return anything.
     */
    handleZoom(event) {

        if(event.deltaY > 0) {
            this.scale = this.scale * .96;
        }
        else {
            this.scale = this.scale * 1.04;
        }

        this.niceDag.setScale(this.scale);
    }

    /**
     * Adds a zoom listener to the element.
     *
     * @return {void}
     */
    addZoomListener() {
        this.addEventListener("wheel", this.handleZoom);
        this.isZooming = true;
    }

    /**
     * Removes the event listener used for zoom functionality.
     *
     * @return {void}
     */
    removeZoomListener() {
        this.removeEventListener("wheel", this.handleZoom);
        this.isZooming=false;
    }

    /**
     * Executes tasks when the element is connected to the DOM.
     * Be sure to call the super.connectedCallback() before using this method.
     * Sets 'now' property to the current timestamp and initiates an interval to ensure scroll visibility.
     *
     * @return {void}
     */
    connectedCallback() {
        // be sure to call the super
        super.connectedCallback();
        this.now = Date.now();
        this.interval = window.setInterval(this.ensureScrollVisible, 250, this);
    }

    /**
     * Perform cleanup operations when the element is disconnected from the DOM.
     * Clears the interval previously set by the element.
     *
     * @return {void}
     */
    disconnectedCallback() {
        super.disconnectedCallback();
        window.clearInterval(this.interval);
    }

    /**
     * Ensure that the content within the container is visible by adjusting the scroll position.
     * @param {Element} container - The container element to ensure visibility of content within.
     * @return {void}
     */
    ensureScrollVisible(container) {
        // Vertical scroll bar
        console.log("ensureScrollVisible " + container);
        if (container.scrollTop === 0) {
            container.scrollTop += 1;
            container.scrollTop -= 1;
        } else {
            container.scrollTop -= 1;
            container.scrollTop += 1;
        }

        // Horizontal scroll bar
        if (container.scrollLeft === 0) {
            container.scrollLeft += 1;
            container.scrollLeft -= 1;
        } else {
            container.scrollLeft -= 1;
            container.scrollLeft += 1;
        }
    }

    /**
     * Renders a node on a given HTML element, including child nodes if available.
     *
     * @param {Object} node - The node object to be rendered, containing an 'id', 'children', and 'collapse' properties.
     * @param {HTMLElement} element - The HTML element on which the node will be rendered.
     *
     * @return {Promise<void>} - A Promise that resolves once the node and its children are successfully rendered on the element.
     */
    async renderNode(node, element) {
        if(!element) return;
        console.log("rendering node " + node.id);
        const newDiv = document.createElement('div');
        newDiv.id = "container";
        newDiv.setAttribute("style", this.ikasanDagNodeStyle);
        newDiv.setAttribute("title", node.id);

        if(node.children?.length > 0 && !node.collapse) {
            let child = await this.groupControl(node);
            newDiv.appendChild(child);
        }
        else {
            let child = await this.nodeControl(node);
            newDiv.appendChild(child);
        }

        element.appendChild(newDiv);
        let colour = await this.getStatusColour(node.id);
        this.styleNode(node.id, colour);
    }

    /**
     * Sets the scale of the niceDag.
     *
     * @param {number} scale - The scale value to set.
     * @return {void}
     */
    zoom(scale) {
        this.niceDag.setScale(scale);
    }

    /**
     * Logs the change of the DAG, retrieves all nodes, and sends the updated DAG model to the server.
     *
     * @return {void}
     */
    onChange() {
        console.log("the dag has changed! " + this.niceDag);
        let nodes = this.niceDag.getAllNodes(true);
        console.log("the dag has changed! nodes " + nodes);
        let dagJsonModel = "["
        nodes.forEach((node) => {
            console.log("the dag has changed! node " + node);
            console.log("the dag has changed! node " + node.parentId);
            console.log("adding node " + node.id);
            if(node.children) {
                this.setChildCollapseStatus(node.children);
            }
            dagJsonModel = dagJsonModel + JSON.stringify(
                {
                    "id": node.id,
                    "dependencies": node.dependencies,
                    "data": node.data,
                    "collapse": node.collapse,
                    "children": node.children,
                    "parentId": node.parentId
                }) + ",";
        })
        dagJsonModel = dagJsonModel.substring(0, dagJsonModel.length - 1);
        dagJsonModel = dagJsonModel + "]";
        console.log("sending data " + dagJsonModel);
        this.$server.setDag(dagJsonModel);
    }

    /**
     * Set the collapse status for a given list of nodes and their children.
     *
     * @param {Array<Object>} nodes - The list of nodes to set collapse status for.
     * @return {void}
     */
    setChildCollapseStatus(nodes) {
        nodes.forEach((node) => {
            let n = this.niceDag.findNodeById(node.id);
            if(n) {
                node.collapse = n.collapse;
            }
            if(node.children) {
                this.setChildCollapseStatus(node.children);
            }
        });
    }

    /**
     * Controls the display and actions of a group node in the diagram.
     *
     * @param {Object} node - The node object representing the group to control.
     * @return {Promise} - A Promise that resolves with the created group control element.
     */
    async groupControl(node) {
        const groupControlDiv = document.createElement('div');
        groupControlDiv.id = node.id + "_status";
        render(html`<div>
                <span style="margin-left: 6px; font-size: 8pt; word-break: break-all; width: 95%;">${node.data?.label || node.id}</span>
                <button style="padding: 0; border: none; background: none; cursor: pointer;" @click="${(e) => this.$server.openDiagram(node.id)}">
                    <i class="fas fa-object-group" style="position: absolute; right: 26px; top: 6px;" title="Open diagram"></i>
                </button>
                <button style="padding: 0; border: none; background: none; cursor: pointer;" @click="${(e) => this.shrinkNode(node.id)}">
                    <i class="fas fa-minus-square" style="position: absolute; right: 6px; top: 6px;" title="Collapse job plan"></i>
                </button>
            </div>`, groupControlDiv);

        // debugger;
        groupControlDiv.attributeStyleMap.set("border-radius", "10px 10px 0 0");
        let colour = await this.getStatusColour(node.id);
        this.styleNode(node.id, colour);
        return groupControlDiv;
    }

    /**
     * Shrinks the node with the specified ID.
     *
     * @param {string} id - The ID of the node to be shrunk.
     * @return {void}
     */
    shrinkNode(id) {
        console.log("shrinking node " + id);
        let node = this.niceDag.findNodeById(id);
        console.log("shrinking node " + node);
        if(node != null) {
            node.shrink();
            let element = this.niceDag.getElementByNodeId(id);
            console.log("shrinking node " + element);
            this.renderNode(node, element);
        }
    }

    /**
     * Controls the display of a node in the UI.
     *
     * @param {Object} node - The node object to be displayed.
     * @return {Promise} - A Promise that resolves to the HTML element representing the node control.
     */
    async nodeControl(node) {
        const nodeControlDiv = document.createElement('div');
        nodeControlDiv.id = node.id + "_status";
        if (node.children?.length > 0) {
            render(html`
                <div>
                    <span style="margin-left: 6px; font-size: 8pt; word-break: break-all; width: 95%;">${node.data?.label || node.id}</span>
                    <button style="padding: 0; border: none; background: none; cursor: pointer;" @click="${(e) => this.$server.openDiagram(node.id)}">
                        <i class="fas fa-object-group" style="position: absolute; right: 26px; top: 6px;" title="Open diagram"></i>
                    </button>
                    <button style="padding: 0; border: none; background: none; cursor: pointer;" @click="${(e) => this.expandNode(node.id)}">
                        <i class="fas fa-plus-square" style="position: absolute; right: 6px; top: 6px;" title="Expand job plan"></i>
                    </button>
                </div>`, nodeControlDiv);
        }
        else {
            render(html`
                <div>
                    <span style="margin-left: 6px;font-size: 8pt; word-break: break-all; width: 95%;">${node.data?.label || node.id}</span>
                    <button style="padding: 0; border: none; background: none; cursor: pointer;" @click="${(e) => this.$server.openDiagram(node.id)}">
                        <i class="fas fa-object-group" style="position: absolute; right: 6px; top: 6px;" title="Open diagram"></i>
                    </button>
                    
                </div>`, nodeControlDiv);
        }

        // debugger;
        nodeControlDiv.attributeStyleMap.set("border-radius", "10px 10px 0 0");
        let colour = await this.getStatusColour(node.id);
        this.styleNode(node.id, colour);
        return nodeControlDiv;
    }

    /**
     * Get the status colour based on the provided ID.
     *
     * @param {number} id The ID used to determine the status colour.
     *
     * @return {Promise<string>} A promise that resolves to the status colour string.
     */
    async getStatusColour(id) {
        let colour = await this.$server.getStatusColour(id);
        return colour;
    }

    /**
     * Expands a node in the tree structure based on the provided node ID.
     *
     * @param {string} id - The ID of the node to be expanded.
     * @return {void}
     */
    expandNode(id) {
        console.log("expanding node " + id);
        let parentNode = this.niceDag.findNodeById(id);
        console.log("expanding node " + parentNode);
        if(parentNode != null) {
            let children = this.niceDag.getElementByNodeId(parentNode.id).children;
            for (let i of children) {
                i.remove();
            }

            parentNode.expand();
            let element = this.niceDag.getElementByNodeId(parentNode.id);
            console.log("expanding node " + element);
            this.renderNode(parentNode, element);
            if(parentNode.children)this.setChildCollapseStatus(parentNode.children);
            parentNode.children.forEach((node) => {
                this.renderNode(node, this.niceDag.getElementByNodeId(node.id));
                if(node.collapse == false && node.children) {
                    this.renderChildren(node.children);
                }
            });
        }
    }

    /**
     * Renders children nodes recursively.
     *
     * @param {Array} children - The array of child nodes to render
     * @return {void}
     */
    renderChildren(children) {
        children.forEach((node) => {
            this.renderNode(node, this.niceDag.getElementByNodeId(node.id));
            if(node.collapse == false && node.children) {
                this.renderChildren(node.children);
            }
        });
    }
}

const NODE_WIDTH = 300;
const NODE_HEIGHT = 170;

const getNodeSize = node => {
    return {
        width: NODE_WIDTH,
        height: NODE_HEIGHT,
    };
};


customElements.define('dag-chart', DagConnector);
