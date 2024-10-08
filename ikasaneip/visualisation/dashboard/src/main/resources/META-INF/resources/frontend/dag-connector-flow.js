import NiceDag from '@ebay/nice-dag-core';
import {LitElement, html, css, render} from 'lit';
import Fontawesome from 'lit-fontawesome';

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

    constructor() {
        super();
        this.container = document.createElement("div");
        this.container.id = "my-dag-chart";
        this.container.setAttribute("style", "width:100%; height:100%; display:flex; overflow:auto");
        console.log("constructor called!");
    }

    styleNode(id) {
        if(this.niceDag) {
            let element = this.niceDag.getElementByNodeId(id);
            element.attributeStyleMap.set("background", "green");
        }
    }

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

            debugger;
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
            this.scale = this.scale * .98;
        }
        else {
            this.scale = this.scale * 1.02;
        }

        this.niceDag.setScale(this.scale);
    }

    addZoomListener() {
        this.addEventListener("wheel", this.handleZoom);
        this.isZooming = true;
    }

    removeZoomListener() {
        this.removeEventListener("wheel", this.handleZoom);
        this.isZooming=false;
    }

    connectedCallback() {
        // be sure to call the super
        super.connectedCallback();
        this.now = Date.now();
        this.interval = window.setInterval(this.ensureScrollVisible, 250, this);
    }

    disconnectedCallback() {
        super.disconnectedCallback();
        window.clearInterval(this.interval);
    }

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

    firstUpdated(changedProperties) {
        // document.getElementById("my-dag-chart").addEventListener("wheel", (event) => {
        //     debugger;
        //     event.preventDefault();
        //
        //     let scale = this.niceDag.getScale();
        //     if(event.deltaY > 0) {
        //         scale = scale * 1.02;
        //     }
        //     else {
        //         scale = scale * 0.98;
        //     }
        //
        //     this.zoom(scale);
        // });
    }

    renderNode(node, element) {
        if(!element) return;
        console.log("rendering node " + node.id);
        const newDiv = document.createElement('div');
        newDiv.setAttribute("style", this.ikasanDagNodeStyle);
        newDiv.setAttribute("title", node.id);
        // this.styleNode(node.id);

        if(node.children?.length > 0 && !node.collapse) {
            newDiv.appendChild(this.groupControl(node));
        }
        else {
            newDiv.appendChild(this.nodeControl(node));
        }

        element.appendChild(newDiv);
    }

    zoom(scale) {
        this.niceDag.setScale(scale);
    }

    onChange() {
        console.log("the dag has changed! " + this.niceDag);
        let nodes = this.niceDag.getAllNodes(true);
        console.log("the dag has changed! nodes " + nodes);
        let dagJsonModel = "["
        nodes.forEach((node) => {
            console.log("the dag has changed! node " + node);
            console.log("the dag has changed! node " + node.parentId);
            //if(!node.parentId) {
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
            // }
            // else {
            //     console.log("skipping node " + node.id + " with collapse " + node.collapse);
            // }
        })
        dagJsonModel = dagJsonModel.substring(0, dagJsonModel.length - 1);
        dagJsonModel = dagJsonModel + "]";
        debugger;
        console.log("sending data " + dagJsonModel);
        this.$server.setDag(dagJsonModel);
    }

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

    groupControl(node) {
        const groupControlDiv = document.createElement('div');
        render(html`<div>
                <span style="margin-left: 6px; font-size: 8pt; word-break: break-all; width: 95%;">${node.data?.label || node.id}</span>
                <button style="padding: 0; border: none; background: none; cursor: pointer;" @click="${(e) => this.$server.openDiagram(node.id)}">
                    <i class="fas fa-object-group" style="position: absolute; right: 26px; top: 6px;" title="Open diagram"></i>
                </button>
                <button style="padding: 0; border: none; background: none; cursor: pointer;" @click="${(e) => this.shrinkNode(node.id)}">
                    <i class="fas fa-minus-square" style="position: absolute; right: 6px; top: 6px;" title="Collapse job plan"></i>
                </button>
            </div>`, groupControlDiv);

        groupControlDiv.attributeStyleMap.set("background", "rgb(102,187,106)");
        return groupControlDiv;
    }

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

    nodeControl(node) {
        const nodeControlDiv = document.createElement('div');

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

        nodeControlDiv.attributeStyleMap.set("background", "rgb(102,187,106)");
        return nodeControlDiv;
    }

    expandNode(id) {
        console.log("expanding node " + id);
        let parentNode = this.niceDag.findNodeById(id);
        console.log("expanding node " + parentNode);
        if(parentNode != null) {
            let children = this.niceDag.getElementByNodeId(parentNode.id).children;
            for (let i of children) {
                i.remove();
            }

            // if(parentNode.collapse==true)
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
const CIRCLE_W_H = 30;

const getNodeSize = node => {
    // if (node.id === 'start' || node.id === 'end' || node.joint) {
    //     return {
    //         width: CIRCLE_W_H,
    //         height: CIRCLE_W_H,
    //     };
    // }
    return {
        width: NODE_WIDTH,
        height: NODE_HEIGHT,
    };
};


customElements.define('dag-chart', DagConnector);
