interface SimulationSettings {
    /**
     * How much two CONNECTED nodes are attracted to their ideal distance.
     */
    edgeIntegrity: number;

    /**
     * The distance at which two CONNECTED nodes aim to be.
     */
    idealDistance: number;

    /**
     * How much ANY two nodes repel each other.
     */
    repulsion: number;

    /**
     * The dampening of the movement of nodes.
     */
    friction: number;

    /**
     * The maximum speed (just in case).
     */
    maxSpeed: number;
}

interface BoundRect {
    left: number;
    right: number;
    top: number;
    bottom: number;
}

const EPS = 0.00001;

const NODE_CLASSNAME = "dialogue-node";

class JigglyNode {
    static nextId: number = 0;

    readonly id: number = JigglyNode.nextId++;
    private posX: number = 0.5;
    private posY: number = 0.5;
    private motionX: number = 0;
    private motionY: number = 0;
    private readonly connections: { [id: number]: JigglyNode } = {};
    private readonly settings: SimulationSettings;
    private readonly bounds: BoundRect;
    readonly element: HTMLElement = document.createElement("div");
    private currentClientX: number = 0;
    private currentClientY: number = 0;

    constructor(settings: SimulationSettings, bounds: BoundRect, x: number, y: number) {
        this.settings = settings;
        this.bounds = bounds;
        this.posX = x;
        this.posY = y;
        this.element.className = NODE_CLASSNAME;
        this.element.textContent = this.id.toString();
    }

    get x(): number {
        return this.posX;
    }

    get y(): number {
        return this.posY;
    }

    get connectionsWithHigherId(): JigglyNode[] {
        const result: JigglyNode[] = [];
        for (const id in this.connections) {
            result.push(this.connections[id]);
        }
        return result;
        // return Object.values(this.connections).filter((node) => node.id > this.id);
    }

    moveTo(clientX: number, clientY: number): void {
        const inverseResolution = 5;
        clientX = Math.round(clientX / inverseResolution) * inverseResolution;
        clientY = Math.round(clientY / inverseResolution) * inverseResolution;
        if (clientX === this.currentClientX && clientY === this.currentClientY) {
            return;
        }
        this.currentClientX = clientX;
        this.currentClientY = clientY;
        this.element.style.transform = `translate(${clientX - this.element.clientWidth / 2}px, ${
            clientY - this.element.clientHeight / 2
        }px)`;
    }

    connectTo(other: JigglyNode): void {
        this.connections[other.id] = other;
        other.connections[this.id] = this;
    }

    applyPhysics(dt: number, frozen: boolean, allNodes: JigglyNode[]): void {
        if (frozen) {
            this.motionX = this.motionY = 0;
            return;
        }
        for (const other of allNodes) {
            const dx = other.posX - this.posX;
            const dy = other.posY - this.posY;
            const distanceSquared = dx * dx + dy * dy;
            if (distanceSquared < EPS) {
                continue;
            }
            const distance = Math.sqrt(distanceSquared); // Sob
            const dxNormalized = dx / distance;
            const dyNormalized = dy / distance;

            if (this.connections[other.id]) {
                // Connected -> strive for the ideal distance
                const distanceFromIdealSpot = (distance - this.settings.idealDistance) / this.settings.idealDistance;
                const distanceFromIdealSpotSquared = distanceFromIdealSpot * distanceFromIdealSpot;
                if (distance < this.settings.idealDistance) {
                    // Too close - push (go away from [other])
                    this.motionX -= dxNormalized * this.settings.edgeIntegrity * distanceFromIdealSpotSquared;
                    this.motionY -= dyNormalized * this.settings.edgeIntegrity * distanceFromIdealSpotSquared;
                } else {
                    // Too far - pull (go towards [other])
                    this.motionX += dxNormalized * this.settings.edgeIntegrity * distanceFromIdealSpotSquared;
                    this.motionY += dyNormalized * this.settings.edgeIntegrity * distanceFromIdealSpotSquared;
                }
            } else {
                // Disconnected -> nothing special matters here
            }
            // Repulsion
            const repulsionMultiplier = -this.settings.repulsion / distanceSquared;
            this.motionX += repulsionMultiplier * dxNormalized;
            this.motionY += repulsionMultiplier * dyNormalized;
        }

        // Apply friction
        this.motionX *= 1.0 - this.settings.friction;
        this.motionY *= 1.0 - this.settings.friction;

        // Limit motion speed
        const motionSquared = this.motionX * this.motionX + this.motionY * this.motionY;
        if (motionSquared > this.settings.maxSpeed * this.settings.maxSpeed) {
            const motion = Math.sqrt(motionSquared);
            this.motionX = (this.motionX / motion) * this.settings.maxSpeed;
            this.motionY = (this.motionY / motion) * this.settings.maxSpeed;
        }
    }

    move(dt: number): void {
        this.posX += this.motionX * dt;
        if (this.motionX < 0 && this.posX < this.bounds.left) {
            this.posX = this.bounds.left;
            this.motionX *= -1;
        } else if (this.motionX > 0 && this.posX > this.bounds.right) {
            this.posX = this.bounds.right;
            this.motionX *= -1;
        }
        this.posY += this.motionY * dt;
        if (this.motionY < 0 && this.posY < this.bounds.top) {
            this.posY = this.bounds.top;
            this.motionY *= -1;
        } else if (this.motionY > 0 && this.posY > this.bounds.bottom) {
            this.posY = this.bounds.bottom;
            this.motionY *= -1;
        }
    }
}

const MIN_FPS = 15;
const MAX_DT = 1.0 / MIN_FPS;

export class DialogueGraph {
    private readonly parentElement: HTMLElement;
    private readonly canvas: HTMLCanvasElement;
    private readonly fpsCounter: HTMLElement;
    private delayQueue: number[] = [];
    private frameNumber = 0;

    private nodes: JigglyNode[] = [];
    private animFrameRequest?: number;
    private lastTimestamp?: DOMHighResTimeStamp;
    private readonly bounds: BoundRect = { left: 0, right: 1, top: 0, bottom: 1 };

    private mouseMoveEventFn: (event: MouseEvent) => void;
    private frozenNode?: HTMLElement;

    private cameraX: number;
    private cameraY: number;
    private cameraExtent: number;

    constructor(parentElement: HTMLElement) {
        this.parentElement = parentElement;
        this.canvas = document.createElement("canvas");
        this.canvas.style.width = "100%";
        this.canvas.style.height = "100%";
        this.fpsCounter = document.createElement("div");
        this.fpsCounter.style.cssText = "position: absolute; inset-block-end: 2em; inset-inline-end: 1em";
        this.fpsCounter.addEventListener("click", this.endSimulation.bind(this));

        this.cameraX = (this.bounds.left + this.bounds.right) / 2;
        this.cameraY = (this.bounds.top + this.bounds.bottom) / 2;
        this.cameraExtent = 1.0;
    }

    onMouseMove(event: MouseEvent): void {
        let node = event.target;
        while (node && node instanceof HTMLElement && !node.classList.contains(NODE_CLASSNAME)) {
            node = node.parentElement;
        }
        if (node && node instanceof HTMLElement) {
            this.frozenNode = node;
        } else {
            this.frozenNode = undefined;
        }
    }

    beginSimulation(settings: SimulationSettings, nodeCount: number): void {
        if (this.animFrameRequest) {
            this.endSimulation();
        }
        this.nodes = [];
        const width = this.bounds.right - this.bounds.left;
        const height = this.bounds.bottom - this.bounds.top;

        this.canvas.width = this.parentElement.clientWidth;
        this.canvas.height = this.parentElement.clientHeight;

        while (this.nodes.length < nodeCount) {
            this.nodes.push(
                new JigglyNode(
                    settings,
                    this.bounds,
                    this.bounds.left + Math.random() * width,
                    this.bounds.top + Math.random() * height,
                ),
            );
        }

        // Generate edges
        const potentialEdges: [number, number][] = [];
        for (let firstIndex = 0; firstIndex < nodeCount; firstIndex++) {
            for (let secondIndex = firstIndex + 1; secondIndex < nodeCount; secondIndex++) {
                potentialEdges.push([firstIndex, secondIndex]);
            }
        }
        const edgeCount = Math.floor(
            (Math.random() * Math.random() * Math.random() * Math.random() * nodeCount * nodeCount) / 2,
        );
        for (let index = 1; index < potentialEdges.length; index++) {
            const toSwapWith = Math.min(Math.floor(Math.random() * (index + 1)), index);
            if (toSwapWith !== index) {
                const tmp = potentialEdges[index];
                potentialEdges[index] = potentialEdges[toSwapWith];
                potentialEdges[toSwapWith] = tmp;
            }
        }
        let edgesLeft = potentialEdges.length;
        for (let index = 0; index < edgeCount; index++) {
            const lastEdgeIndex = edgesLeft - 1;
            const pickedEdgeIndex = Math.min(Math.floor(Math.random() * edgesLeft), lastEdgeIndex);
            const edge = potentialEdges[pickedEdgeIndex];
            this.nodes[edge[0]].connectTo(this.nodes[edge[1]]);

            if (pickedEdgeIndex !== lastEdgeIndex) {
                potentialEdges[pickedEdgeIndex] = potentialEdges[lastEdgeIndex];
            }
            edgesLeft--;
        }

        // Add HTML elements

        for (const node of this.nodes) {
            this.parentElement.appendChild(node.element);
        }

        this.frameNumber = 0;
        this.parentElement.appendChild(this.canvas);
        this.fpsCounter.textContent = "";
        this.parentElement.appendChild(this.fpsCounter);
        this.animFrameRequest = requestAnimationFrame(this.animate.bind(this));

        document.addEventListener("mousemove", (this.mouseMoveEventFn = this.onMouseMove.bind(this)));
    }

    private simulate(dt: number): void {
        for (const node of this.nodes) {
            node.applyPhysics(dt, node.element === this.frozenNode, this.nodes);
        }
        for (const node of this.nodes) {
            node.move(dt);
        }
    }

    private render(): void {
        const draw = this.canvas.getContext("2d");
        if (!draw) {
            console.log("No draw?!");
            this.endSimulation();
            return;
        }

        this.cameraExtent += 0.001;

        this.canvas.width = this.canvas.clientWidth;
        this.canvas.height = this.canvas.clientHeight;

        let viewWidth = this.cameraExtent;
        let viewHeight = this.cameraExtent;
        if (this.canvas.height > this.canvas.width) {
            viewWidth *= this.canvas.width / this.canvas.height;
        } else {
            viewHeight *= this.canvas.height / this.canvas.width;
        }
        const camBounds: BoundRect = {
            left: this.cameraX - viewWidth / 2,
            right: this.cameraX + viewWidth / 2,
            top: this.cameraY - viewHeight / 2,
            bottom: this.cameraY + viewHeight / 2,
        };

        const mulX = this.canvas.clientWidth / (camBounds.right - camBounds.left);
        const mulY = this.canvas.clientHeight / (camBounds.bottom - camBounds.top);
        const shiftX = -camBounds.left * mulX;
        const shiftY = -camBounds.top * mulY;

        draw.clearRect(0, 0, this.canvas.width, this.canvas.height);
        draw.beginPath();
        for (const node of this.nodes) {
            for (const other of node.connectionsWithHigherId) {
                draw.moveTo(node.x * mulX + shiftX, node.y * mulY + shiftY);
                draw.lineTo(other.x * mulX + shiftX, other.y * mulY + shiftY);
            }
        }
        draw.stroke();
        draw.closePath();

        // if (this.nodes.length > 0) {
        //     const nodeToMove = this.nodes[this.frameNumber % this.nodes.length];
        //     nodeToMove.moveTo(nodeToMove.x * mulX + shiftX, nodeToMove.y * mulY + shiftY);
        // }
        // for (const node of this.nodes) {
        //     node.moveTo(node.x * mulX + shiftX, node.y * mulY + shiftY);
        // }

        for (const node of this.nodes) {
            draw.beginPath();
            draw.ellipse(node.x * mulX + shiftX, node.y * mulY + shiftY, 10, 10, 0, 0, 2 * Math.PI);
            draw.fillStyle = "yellow";
            draw.fill();
            draw.strokeStyle = "black";
            draw.stroke();
            draw.closePath();
        }
    }

    private animate(now: DOMHighResTimeStamp): void {
        if (this.lastTimestamp == null) {
            this.lastTimestamp = now;
        }
        let dt = (now - this.lastTimestamp) * 0.001;

        this.delayQueue.push(dt);
        if (this.delayQueue.length > 100) {
            this.delayQueue = this.delayQueue.slice(1);
        }
        let sum = 0;
        for (const delay of this.delayQueue) {
            sum += delay;
        }

        this.fpsCounter.textContent = `${Math.round(this.delayQueue.length / sum)} FPS`;
        // In order to prevent things from getting out of hand, limit the time delta
        this.simulate(Math.min(dt, MAX_DT));
        this.render();
        this.lastTimestamp = now;
        this.animFrameRequest = requestAnimationFrame(this.animate.bind(this));
        this.frameNumber++;
    }

    endSimulation(): void {
        if (this.animFrameRequest) {
            cancelAnimationFrame(this.animFrameRequest);
        }
        // for (const node of this.nodes) {
        //     node.element.remove();
        // }
        // this.nodes = [];
        // this.lastTimestamp = undefined;
        // this.canvas.remove();
        this.fpsCounter.remove();
        this.delayQueue = [];
        document.removeEventListener("mousemove", this.mouseMoveEventFn);
    }
}
